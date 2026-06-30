import React, { useCallback, useRef, useEffect } from 'react';
import { Modal } from 'react-bootstrap';
import VideoPlayer, { VideoPlayerHandle } from './VideoPlayer';
import VideoControls from './VideoControls';
import LazyImage from './LazyImage';

type MediaType = 'image' | 'video';

interface MediaItem {
    type: MediaType;
    url: string;
    alt: string;
}

interface MediaModalProps {
    show: boolean;
    mediaItems: MediaItem[];
    currentIndex: number;
    onHide: () => void;
    onNavigate: (index: number) => void;
}

const MediaModal: React.FC<MediaModalProps> = ({ show, mediaItems, currentIndex, onHide, onNavigate }) => {
    const videoRef = useRef<VideoPlayerHandle>(null);
    const currentItem = mediaItems[currentIndex];

    // Autoplay видео при смене слайда
    useEffect(() => {
        if (show && currentItem?.type === 'video') {
            // Небольшая задержка для инициализации
            const timer = setTimeout(() => {
                videoRef.current?.play();
            }, 150);
            return () => clearTimeout(timer);
        }
    }, [show, currentIndex, currentItem]);

    // Пауза при закрытии модала
    const handleHide = useCallback(() => {
        if (currentItem?.type === 'video')
            videoRef.current?.pause();
        onHide();
    }, [currentItem, onHide]);

    // Переключение с паузой
    const handlePrev = useCallback(() => {
        if (currentItem?.type === 'video')
            videoRef.current?.pause();
        const newIndex = currentIndex > 0 ? currentIndex - 1 : mediaItems.length - 1;
        onNavigate(newIndex);
    }, [currentIndex, mediaItems.length, currentItem, onNavigate]);

    const handleNext = useCallback(() => {
        if (currentItem?.type === 'video')
            videoRef.current?.pause();
        const newIndex = currentIndex < mediaItems.length - 1 ? currentIndex + 1 : 0;
        onNavigate(newIndex);
    }, [currentIndex, mediaItems.length, currentItem, onNavigate]);

    // Навигация стрелками с клавиатуры, пока модалка открыта (Escape закрывает через Bootstrap Modal)
    useEffect(() => {
        if (!show || mediaItems.length <= 1) return;
        const onKey = (e: KeyboardEvent) => {
            if (e.key === 'ArrowLeft') handlePrev();
            else if (e.key === 'ArrowRight') handleNext();
        };
        window.addEventListener('keydown', onKey);
        return () => window.removeEventListener('keydown', onKey);
    }, [show, mediaItems.length, handlePrev, handleNext]);

    return (
        <Modal show={show} onHide={handleHide} fullscreen className="image-modal">
            <Modal.Body className="p-0">
                <div className="image-modal-content">
                    <button type="button" className="modal-close" onClick={handleHide} aria-label="Закрыть">×</button>
                    {mediaItems.length > 1 && (
                        <>
                            <button type="button" className="modal-prev" onClick={handlePrev} aria-label="Предыдущее медиа">‹</button>
                            <button type="button" className="modal-next" onClick={handleNext} aria-label="Следующее медиа">›</button>
                        </>
                    )}
                    {currentItem ? (
                        currentItem.type === 'image' ? (
                            <LazyImage src={currentItem.url} alt={currentItem.alt} className="modal-image" />
                        ) : (
                            <>
                                <VideoPlayer
                                    ref={videoRef}
                                    src={currentItem.url}
                                    alt={currentItem.alt}
                                    muted={true}
                                    loop={true}
                                    style={{
                                        maxWidth: '100%',
                                        maxHeight: '100%',
                                        width: 'auto',
                                        height: 'auto',
                                        objectFit: 'contain'
                                    }}
                                />
                                <VideoControls videoRef={videoRef} />
                            </>
                        )
                    ) : null}
                </div>
            </Modal.Body>
        </Modal>
    );
};

export default MediaModal;
