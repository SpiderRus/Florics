import React, { useState } from 'react';
import { Carousel } from 'react-bootstrap';
import ImageModal from './ImageModal';

interface LargeImageCarouselProps {
    images: string[];
    plantName: string;
}

const LargeImageCarousel: React.FC<LargeImageCarouselProps> = ({ images, plantName }) => {
    const [showModal, setShowModal] = useState(false);
    const [currentIndex, setCurrentIndex] = useState(0);

    const handleImageClick = (index: number) => {
        setCurrentIndex(index);
        setShowModal(true);
    };

    return (
        <>
            <Carousel 
                className="large-carousel" 
                interval={null}
                activeIndex={currentIndex}
                onSelect={(selectedIndex) => setCurrentIndex(selectedIndex)}
            >
                {images.map((image, index) => (
                    <Carousel.Item key={index}>
                        <img
                            className="d-block w-100 carousel-image"
                            src={image}
                            alt={`${plantName} ${index + 1}`}
                            onClick={() => handleImageClick(index)}
                            style={{ cursor: 'pointer', height: '500px', objectFit: 'cover' }}
                        />
                    </Carousel.Item>
                ))}
            </Carousel>

            <ImageModal
                show={showModal}
                image={images[currentIndex]}
                images={images}
                currentIndex={currentIndex}
                onHide={() => setShowModal(false)}
                onNavigate={setCurrentIndex}
            />
        </>
    );
};

export default LargeImageCarousel;
