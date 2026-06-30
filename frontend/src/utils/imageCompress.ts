export interface CompressOptions {
    maxDim?: number;          // длинная сторона, по умолчанию 1600
    maxBytes?: number;        // по умолчанию 2 МБ
    qualitySteps?: number[];  // по умолчанию [0.8, 0.7, 0.6, 0.5]
}

/**
 * Уменьшает изображение по длинной стороне до maxDim (сохраняя пропорции)
 * и кодирует в JPEG, понижая quality пока не уложится в maxBytes. Возвращает JPEG Blob.
 *
 * Декодирование — через <img> + decode(), а НЕ createImageBitmap: это самый совместимый путь.
 * createImageBitmap с options (в т.ч. imageOrientation: 'from-image') реджектится на части
 * мобильных браузеров (старые Safari/iPadOS не принимали options-аргумент) и не умеет HEIC/HEIF.
 * Нативный <img> на iOS/iPadOS декодирует HEIC и применяет EXIF-ориентацию (drawImage её учитывает).
 */
export async function compressImage(file: File, opts: CompressOptions = {}): Promise<Blob> {
    const maxDim = opts.maxDim ?? 1600;
    const maxBytes = opts.maxBytes ?? 2 * 1024 * 1024;
    const qualitySteps = opts.qualitySteps ?? [0.8, 0.7, 0.6, 0.5];

    const url = URL.createObjectURL(file);
    try {
        const img = await loadImage(url);
        const sw = img.naturalWidth || img.width;
        const sh = img.naturalHeight || img.height;
        if (!sw || !sh) throw new Error('Изображение декодировано с нулевыми размерами');

        const scale = Math.min(1, maxDim / Math.max(sw, sh));
        const w = Math.max(1, Math.round(sw * scale));
        const h = Math.max(1, Math.round(sh * scale));

        const canvas = document.createElement('canvas');
        canvas.width = w;
        canvas.height = h;
        const ctx = canvas.getContext('2d');
        if (!ctx) throw new Error('Canvas 2D context недоступен');
        ctx.drawImage(img, 0, 0, w, h);

        const toBlob = (q: number) =>
            new Promise<Blob | null>(resolve => canvas.toBlob(resolve, 'image/jpeg', q));

        let blob: Blob | null = null;
        for (const q of qualitySteps) {
            blob = await toBlob(q);
            if (blob && blob.size <= maxBytes) return blob;
        }
        if (!blob) throw new Error('canvas.toBlob вернул null (кодирование JPEG не удалось)');
        return blob; // последняя (наименьшая) попытка, даже если чуть превышает лимит
    } finally {
        URL.revokeObjectURL(url);
    }
}

/** Загрузка изображения в <img> с дожиданием декодирования (decode() надёжнее onload). */
function loadImage(src: string): Promise<HTMLImageElement> {
    const img = new Image();
    img.decoding = 'async';
    img.src = src;
    if (typeof img.decode === 'function') {
        return img.decode().then(() => img);
    }
    return new Promise<HTMLImageElement>((resolve, reject) => {
        img.onload = () => resolve(img);
        img.onerror = () => reject(new Error('Не удалось загрузить изображение'));
    });
}
