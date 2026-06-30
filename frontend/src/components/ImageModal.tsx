import React, { useEffect } from 'react';
import { Modal } from 'react-bootstrap';
import LazyImage from './LazyImage';

interface ImageModalProps {
    show: boolean;
    image: string;
    images: string[];
    currentIndex: number;
    onHide: () => void;
    onNavigate: (index: number) => void;
}

const ImageModal: React.FC<ImageModalProps> = ({ show, image, images, currentIndex, onHide, onNavigate }) => {
    const handlePrev = () => {
        const newIndex = currentIndex > 0 ? currentIndex - 1 : images.length - 1;
        onNavigate(newIndex);
    };

    const handleNext = () => {
        const newIndex = currentIndex < images.length - 1 ? currentIndex + 1 : 0;
        onNavigate(newIndex);
    };

    // Навигация стрелками с клавиатуры, пока модалка открыта (Escape закрывает через Bootstrap Modal)
    useEffect(() => {
        if (!show || images.length <= 1) return;
        const onKey = (e: KeyboardEvent) => {
            if (e.key === 'ArrowLeft') onNavigate(currentIndex > 0 ? currentIndex - 1 : images.length - 1);
            else if (e.key === 'ArrowRight') onNavigate(currentIndex < images.length - 1 ? currentIndex + 1 : 0);
        };
        window.addEventListener('keydown', onKey);
        return () => window.removeEventListener('keydown', onKey);
    }, [show, images.length, currentIndex, onNavigate]);

    return (
        <Modal show={show} onHide={onHide} fullscreen className="image-modal">
            <Modal.Body className="p-0">
                <div className="image-modal-content">
                    <button type="button" className="modal-close" onClick={onHide} aria-label="Закрыть">×</button>
                    {images.length > 1 && (
                        <>
                            <button type="button" className="modal-prev" onClick={handlePrev} aria-label="Предыдущее изображение">‹</button>
                            <button type="button" className="modal-next" onClick={handleNext} aria-label="Следующее изображение">›</button>
                        </>
                    )}
                    <LazyImage src={image} alt="Изображение в полном размере" className="modal-image" />
                </div>
            </Modal.Body>
        </Modal>
    );
};

export default ImageModal;
