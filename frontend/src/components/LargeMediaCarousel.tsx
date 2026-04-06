import React, { useState, useMemo, useCallback, useRef } from 'react';
import { Carousel } from 'react-bootstrap';
import VideoPlayer, { VideoPlayerHandle } from './VideoPlayer';
import { Media } from '../services/goodsService';
import { convertMediaToItems, MediaItem } from '../utils/mediaUtils';

interface LargeMediaCarouselProps {
    media: Media[];
    goodsName: string;
    onMediaClick?: (mediaItems: MediaItem[], index: number) => void;
}

const LargeMediaCarousel: React.FC<LargeMediaCarouselProps> = ({
    media,
    goodsName,
    onMediaClick
}) => {
    const [currentIndex, setCurrentIndex] = useState(0);
    const videoRefs = useRef<Map<number, VideoPlayerHandle>>(new Map());

    const mediaItems = useMemo(() => {
        return convertMediaToItems(media, goodsName);
    }, [media, goodsName]);

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
                            style={{ cursor: 'pointer', height: '400px', objectFit: 'contain' }}
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
                                    height: '400px',
                                    objectFit: 'contain'
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
