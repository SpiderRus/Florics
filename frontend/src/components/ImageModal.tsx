import React from 'react';
import { Modal } from 'react-bootstrap';

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

    return (
        <Modal show={show} onHide={onHide} centered size="xl" className="image-modal">
            <Modal.Body className="p-0">
                <div className="image-modal-content">
                    <button className="modal-close" onClick={onHide}>×</button>
                    {images.length > 1 && (
                        <>
                            <button className="modal-prev" onClick={handlePrev}>‹</button>
                            <button className="modal-next" onClick={handleNext}>›</button>
                        </>
                    )}
                    <img src={image} alt="Full size" className="modal-image" />
                </div>
            </Modal.Body>
        </Modal>
    );
};

export default ImageModal;
