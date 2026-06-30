import React, { useEffect, useRef, useState } from 'react';
import { Button, Form, Spinner } from 'react-bootstrap';
import { toast } from 'react-toastify';
import { compressImage } from '../../utils/imageCompress';

/**
 * Стабильный локальный ключ-идентификатор черновика фото (UUID, 36 символов).
 * Используется только на клиенте (React key + идентификатор draft); media_id в БД генерит сервер.
 *
 * crypto.randomUUID доступен лишь в secure context (https/localhost), а на планшете приложение
 * открывают по http://<ip>:8080 (insecure) — там его нет. Фолбэк: UUIDv4 через crypto.getRandomValues
 * (он работает и в insecure context). Последний резерв обрезаем до 36 символов на всякий случай.
 */
function genKey(): string {
    const c: Crypto | undefined = typeof crypto !== 'undefined' ? crypto : undefined;
    if (c && typeof c.randomUUID === 'function') return c.randomUUID();
    if (c && typeof c.getRandomValues === 'function') {
        const b = c.getRandomValues(new Uint8Array(16));
        b[6] = (b[6] & 0x0f) | 0x40; // версия 4
        b[8] = (b[8] & 0x3f) | 0x80; // вариант RFC 4122
        const h = Array.from(b, x => x.toString(16).padStart(2, '0'));
        return `${h[0]}${h[1]}${h[2]}${h[3]}-${h[4]}${h[5]}-${h[6]}${h[7]}-${h[8]}${h[9]}-${h[10]}${h[11]}${h[12]}${h[13]}${h[14]}${h[15]}`;
    }
    return ('k-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2)).slice(0, 36);
}

export interface PhotoDraft {
    key: string;          // стабильный локальный ключ
    mediaId?: string;     // есть => уже сохранено (внешнее или из БД)
    url?: string;         // внешний URL (новое внешнее) или url существующего (для показа)
    file?: Blob;          // есть => новый файл/снимок (сжатый JPEG) для POST
    previewUrl: string;   // что показывает <img src>
}

interface Props {
    photos: PhotoDraft[];
    onChange: (next: PhotoDraft[]) => void;
    onAnalyze: () => void;
    analyzing: boolean;
}

export const MediaManager: React.FC<Props> = ({ photos, onChange, onAnalyze, analyzing }) => {
    const [urlInput, setUrlInput] = useState('');
    const [busy, setBusy] = useState(false);
    const fileRef = useRef<HTMLInputElement>(null);
    const cameraRef = useRef<HTMLInputElement>(null);

    // Освобождать objectURL новых файлов при размонтировании (закрытие модалки)
    const photosRef = useRef(photos);
    photosRef.current = photos;
    useEffect(() => () => {
        photosRef.current.forEach(p => { if (p.file) URL.revokeObjectURL(p.previewUrl); });
    }, []);

    const addFiles = async (files: FileList | null) => {
        if (!files || files.length === 0) return;
        setBusy(true);
        try {
            const added: PhotoDraft[] = [];
            for (const file of Array.from(files)) {
                if (!file.type.startsWith('image/')) {
                    toast.error(`«${file.name}» — не изображение`);
                    continue;
                }
                try {
                    const blob = await compressImage(file);
                    added.push({ key: genKey(), file: blob, previewUrl: URL.createObjectURL(blob) });
                } catch (err) {
                    // Реальную ошибку (тип файла + текст) оставляем в консоли для диагностики мобильных сбоев.
                    console.error('Обработка фото не удалась:', { name: file.name, type: file.type, size: file.size }, err);
                    toast.error(`Не удалось обработать «${file.name}»`);
                }
            }
            if (added.length) onChange([...photos, ...added]);
        } finally {
            setBusy(false);
            if (fileRef.current) fileRef.current.value = '';
            if (cameraRef.current) cameraRef.current.value = '';
        }
    };

    const addUrl = () => {
        const u = urlInput.trim();
        if (!u) return;
        if (!/^https?:\/\//i.test(u)) {
            toast.error('Ссылка должна начинаться с http:// или https://');
            return;
        }
        onChange([...photos, { key: genKey(), url: u, previewUrl: u }]);
        setUrlInput('');
    };

    const remove = (idx: number) => {
        const p = photos[idx];
        if (p.file) URL.revokeObjectURL(p.previewUrl);
        onChange(photos.filter((_, i) => i !== idx));
    };

    const move = (idx: number, dir: -1 | 1) => {
        const j = idx + dir;
        if (j < 0 || j >= photos.length) return;
        const next = [...photos];
        [next[idx], next[j]] = [next[j], next[idx]];
        onChange(next);
    };

    const makePreview = (idx: number) => {
        if (idx === 0) return;
        const next = [...photos];
        const [item] = next.splice(idx, 1);
        next.unshift(item);
        onChange(next);
    };

    return (
        <div>
            <div className="d-flex flex-wrap gap-2 mb-3">
                <Button variant="outline-success" size="sm" disabled={busy} onClick={() => fileRef.current?.click()}>
                    📁 Загрузить файлы
                </Button>
                <Button variant="outline-success" size="sm" disabled={busy} onClick={() => cameraRef.current?.click()}>
                    📷 Сделать фото
                </Button>
                <input ref={fileRef} type="file" accept="image/*" multiple hidden
                       onChange={e => addFiles(e.target.files)} />
                <input ref={cameraRef} type="file" accept="image/*" capture="environment" hidden
                       onChange={e => addFiles(e.target.files)} />
                <Button
                    variant="success"
                    size="sm"
                    disabled={photos.length === 0 || analyzing || busy}
                    onClick={onAnalyze}
                >
                    {analyzing
                        ? <><Spinner animation="border" size="sm" className="me-1" />Анализ фотографий…</>
                        : '🔍 Анализировать фото'}
                </Button>
            </div>

            <div className="d-flex gap-2 mb-3">
                <Form.Control
                    type="url"
                    placeholder="Или вставьте ссылку на изображение (https://…)"
                    value={urlInput}
                    onChange={e => setUrlInput(e.target.value)}
                    onKeyDown={e => { if (e.key === 'Enter') { e.preventDefault(); addUrl(); } }}
                />
                <Button variant="outline-secondary" onClick={addUrl}>Добавить</Button>
            </div>

            {busy && <div className="text-muted small mb-2">Обработка изображений…</div>}

            {photos.length === 0 ? (
                <div className="text-muted text-center py-4">
                    Фотографий пока нет. Добавьте файл, снимок или ссылку.
                </div>
            ) : (
                <div className="media-grid">
                    {photos.map((p, idx) => (
                        <div key={p.key} className="media-tile">
                            <img
                                src={p.previewUrl}
                                alt={`Фото ${idx + 1}`}
                                className="media-tile-img"
                                onError={e => { (e.currentTarget as HTMLImageElement).style.opacity = '0.3'; }}
                            />
                            {idx === 0 && <span className="media-badge">Превью</span>}
                            <div className="media-tile-actions">
                                <button type="button" title="Левее" disabled={idx === 0}
                                        onClick={() => move(idx, -1)}>←</button>
                                <button type="button" title="Правее" disabled={idx === photos.length - 1}
                                        onClick={() => move(idx, 1)}>→</button>
                                {idx !== 0 && (
                                    <button type="button" title="Сделать превью"
                                            onClick={() => makePreview(idx)}>★</button>
                                )}
                                <button type="button" title="Удалить" className="media-del"
                                        onClick={() => remove(idx)}>✕</button>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default MediaManager;
