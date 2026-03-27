import React, { useState, useMemo, useCallback, useRef } from 'react';
import { Carousel } from 'react-bootstrap';
import VideoPlayer, { VideoPlayerHandle } from './VideoPlayer';

type MediaType = 'image' | 'video';

interface MediaItem {
    type: MediaType;
    url: string;
    alt: string;
}

interface LargeMediaCarouselProps {
    images: string[];
    videoUrls?: string[];
    plantName: string;
    onMediaClick?: (mediaItems: MediaItem[], index: number) => void;
}

// Функция для объединения изображений и видео
function createMediaItems(
    images: string[],
    videoUrls: string[] | null | undefined,
    plantName: string
): MediaItem[] {
    const mediaItems: MediaItem[] = [];

    // Добавляем изображения
    images.forEach((url, idx) => {
        mediaItems.push({
            type: 'image',
            url,
            alt: `${plantName} - фото ${idx + 1}`
        });
    });

    // Добавляем видео
    videoUrls?.forEach((url, idx) => {
        mediaItems.push({
            type: 'video',
            url,
            alt: `${plantName} - видео ${idx + 1}`
        });
    });

    return mediaItems;
}

const LargeMediaCarousel: React.FC<LargeMediaCarouselProps> = ({
    images,
    videoUrls,
    plantName,
    onMediaClick
}) => {
    const [currentIndex, setCurrentIndex] = useState(0);
    const videoRefs = useRef<Map<number, VideoPlayerHandle>>(new Map());

    const mediaItems = useMemo(
        () => createMediaItems(images, videoUrls, plantName),
        [images, videoUrls, plantName]
    );

    const handleSelect = useCallback((selectedIndex: number) => {
        // Пауза предыдущего видео
        const prevItem = mediaItems[currentIndex];
        if (prevItem?.type === 'video') {
            videoRefs.current.get(currentIndex)?.pause();
        }

        setCurrentIndex(selectedIndex);

        // Autoplay нового видео с небольшой задержкой для плавности
        const currentItem = mediaItems[selectedIndex];
        if (currentItem?.type === 'video') {
            setTimeout(() => {
                videoRefs.current.get(selectedIndex)?.play();
            }, 100);
        }
    }, [currentIndex, mediaItems]);

    const handleMediaClick = (index: number) => {
        // Пауза видео перед открытием модала
        const currentItem = mediaItems[index];
        if (currentItem?.type === 'video') {
            videoRefs.current.get(index)?.pause();
        }

        onMediaClick?.(mediaItems, index);
    };

    return (
        <Carousel
            className="large-carousel"
            interval={null}
            activeIndex={currentIndex}
            onSelect={handleSelect}
        >
            {mediaItems.map((item, index) => (
                <Carousel.Item key={`${item.type}-${index}`}>
                    {item.type === 'image' ? (
                        <img
                            className="d-block w-100 carousel-image"
                            src={item.url}
                            alt={item.alt}
                            onClick={() => handleMediaClick(index)}
                            style={{ cursor: 'pointer', height: '500px', objectFit: 'cover' }}
                        />
                    ) : (
                        <div onClick={() => handleMediaClick(index)} style={{ cursor: 'pointer' }}>
                            <VideoPlayer
                                ref={(ref) => {
                                    if (ref) videoRefs.current.set(index, ref);
                                    else videoRefs.current.delete(index);
                                }}
                                src={item.url}
                                alt={item.alt}
                                muted={true}
                                loop={true}
                                style={{
                                    width: '100%',
                                    height: '500px',
                                    objectFit: 'cover'
                                }}
                            />
                        </div>
                    )}
                </Carousel.Item>
            ))}
        </Carousel>
    );
};

// Экспортируем MediaItem для использования в других компонентах
export type { MediaItem };
export default LargeMediaCarousel;
