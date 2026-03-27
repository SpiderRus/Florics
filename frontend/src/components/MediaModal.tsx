import React, { useCallback, useRef, useEffect } from 'react';
import { Modal } from 'react-bootstrap';
import VideoPlayer, { VideoPlayerHandle } from './VideoPlayer';
import VideoControls from './VideoControls';

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

    return (
        <Modal show={show} onHide={handleHide} fullscreen className="image-modal">
            <Modal.Body className="p-0">
                <div className="image-modal-content">
                    <button className="modal-close" onClick={handleHide}>×</button>
                    {mediaItems.length > 1 && (
                        <>
                            <button className="modal-prev" onClick={handlePrev}>‹</button>
                            <button className="modal-next" onClick={handleNext}>›</button>
                        </>
                    )}
                    {currentItem ? (
                        currentItem.type === 'image' ? (
                            <img src={currentItem.url} alt={currentItem.alt} className="modal-image" />
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
