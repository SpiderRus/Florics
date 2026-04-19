import React, { useState } from 'react';
import { Carousel } from 'react-bootstrap';
import LazyImage from './LazyImage';

interface ImageCarouselProps {
    images: string[];
    goodsName: string;
    goodsId?: string | number;
}

const ImageCarousel: React.FC<ImageCarouselProps> = ({images, goodsName, goodsId}) => {
    // Отслеживаем битые изображения
    const [failedImages, setFailedImages] = useState<Set<string>>(new Set());
    // Состояние для управления автоскроллингом
    const [isHovered, setIsHovered] = useState(false);
    const [isManuallyPaused, setIsManuallyPaused] = useState(false);

    // Fallback изображение
    const placeholderImage = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="250" viewBox="0 0 400 250"%3E%3Crect width="400" height="250" fill="%23e9ecef"/%3E%3Ctext x="50%25" y="50%25" dominant-baseline="middle" text-anchor="middle" font-family="Arial, sans-serif" font-size="18" fill="%236c757d"%3E🌿 Изображение недоступно%3C/text%3E%3C/svg%3E';

    // Обработчик ошибки загрузки
    const handleImageError = (imageUrl: string) => setFailedImages((prev) => new Set(prev).add(imageUrl));

    // Фильтруем битые изображения
    const validImages = images.filter((img) => !failedImages.has(img));

    // Если все изображения битые, показываем placeholder
    if (validImages.length === 0) {
        return (
            <LazyImage
                src={placeholderImage}
                alt={`${goodsName} - изображение недоступно`}
                style={{
                    width: '100%',
                    height: '250px',
                    objectFit: 'cover'
                }}
                showLoader={false}
            />
        );
    }

    // Если только одно изображение, показываем его без карусели
    if (validImages.length === 1) {
        return (
            <LazyImage
                src={validImages[0]}
                alt={goodsName}
                onError={() => handleImageError(validImages[0])}
                style={{
                    width: '100%',
                    height: '250px',
                    objectFit: 'cover'
                }}
            />
        );
    }

    return (
        <div
            onMouseEnter={() => {
                setIsHovered(true);
                setIsManuallyPaused(false);
            }}
            onMouseLeave={() => {
                setIsHovered(false);
            }}
        >
            <Carousel
                key={`carousel-${goodsId}`}
                interval={isHovered && !isManuallyPaused ? 2000 : null}
                pause={false}
                onSelect={(_selectedIndex, event) => {
                    if (event !== null && event !== undefined && event.source !== 'timer')
                        setIsManuallyPaused(true);
                }}
                indicators={validImages.length > 1}
                controls={validImages.length > 1}
                wrap={true}
            >
                {validImages.map((image, index) => (
                    <Carousel.Item key={index}>
                        <LazyImage
                            className="d-block w-100"
                            src={image}
                            alt={`${goodsName} - фото ${index + 1}`}
                            onError={() => handleImageError(image)}
                            style={{
                                height: '250px',
                                objectFit: 'cover'
                            }}
                        />
                    </Carousel.Item>
                ))}
            </Carousel>
        </div>
    );
};

export default ImageCarousel;
