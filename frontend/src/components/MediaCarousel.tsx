import React, { useState, useMemo, useCallback, useRef, useEffect } from 'react';
import { Carousel } from 'react-bootstrap';
import VideoPlayer, { VideoPlayerHandle } from './VideoPlayer';
import LazyImage from './LazyImage';
import { Media } from '../services/goodsService';
import { convertMediaToItems } from '../utils/mediaUtils';

interface MediaCarouselProps {
    media: Media[];
    goodsName: string;
    goodsId?: string | number;
}

const MediaCarousel: React.FC<MediaCarouselProps> = ({ media, goodsName, goodsId }) => {
    // Отслеживаем битые элементы
    const [failedMedia, setFailedMedia] = useState<Set<string>>(new Set());
    // Состояние для управления автоскроллингом
    const [isHovered, setIsHovered] = useState(false);
    const [isManuallyPaused, setIsManuallyPaused] = useState(false);
    // Текущий активный слайд
    const [activeIndex, setActiveIndex] = useState(0);
    // Refs для видеоплееров
    const videoRefs = useRef<Map<number, VideoPlayerHandle>>(new Map());

    // Объединяем медиа
    const mediaItems = useMemo(() => {
        return convertMediaToItems(media, goodsName);
    }, [media, goodsName]);

    // Фильтруем битые элементы
    const validMediaItems = useMemo(
        () => mediaItems.filter((item) => !failedMedia.has(item.url)),
        [mediaItems, failedMedia]
    );

    // Fallback изображение
    const placeholderImage = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="250" viewBox="0 0 400 250"%3E%3Crect width="400" height="250" fill="%23e9ecef"/%3E%3Ctext x="50%25" y="50%25" dominant-baseline="middle" text-anchor="middle" font-family="Arial, sans-serif" font-size="18" fill="%236c757d"%3E🌿 Медиа недоступно%3C/text%3E%3C/svg%3E';

    // Обработчик ошибки загрузки
    const handleMediaError = useCallback((url: string) => {
        setFailedMedia((prev) => new Set(prev).add(url));
    }, []);

    // Обработка переключения слайдов с управлением видео
    const handleSlideChange = useCallback((selectedIndex: number, event: any) => {
        const previousIndex = activeIndex;

        // Пауза предыдущего видео
        const prevItem = validMediaItems[previousIndex];
        if (prevItem?.type === 'video') {
            const prevVideoRef = videoRefs.current.get(previousIndex);
            prevVideoRef?.pause();
        }

        setActiveIndex(selectedIndex);

        // Autoplay текущего видео
        const currentItem = validMediaItems[selectedIndex];
        if (currentItem?.type === 'video') {
            const currentVideoRef = videoRefs.current.get(selectedIndex);
            currentVideoRef?.play().catch(err => {
                console.warn('Autoplay blocked:', err);
            });
        }

        // Логика ручной паузы из оригинального ImageCarousel
        if (event !== null && event !== undefined && event.source !== 'timer')
            setIsManuallyPaused(true);
    }, [activeIndex, validMediaItems]);

    // Cleanup при unmount
    useEffect(() => {
        return () => {
            // Останавливаем все видео при размонтировании
            videoRefs.current.forEach(videoRef => {
                videoRef.pause();
            });
            videoRefs.current.clear();
        };
    }, []);

    // Если все элементы битые, показываем placeholder
    if (validMediaItems.length === 0)
        return (
            <LazyImage
                src={placeholderImage}
                alt={`${goodsName} - медиа недоступно`}
                style={{
                    width: '100%',
                    height: 'var(--media-card-h)',
                    objectFit: 'cover'
                }}
                showLoader={false}
            />
        );

    // Если только один элемент, показываем его без карусели
    if (validMediaItems.length === 1) {
        const item = validMediaItems[0];
        if (item.type === 'image')
            return (
                <LazyImage
                    src={item.url}
                    alt={item.alt}
                    onError={() => handleMediaError(item.url)}
                    style={{
                        width: '100%',
                        height: 'var(--media-card-h)',
                        objectFit: 'cover'
                    }}
                />
            );
        else
            return (
                <VideoPlayer
                    ref={(ref) => {
                        if (ref) videoRefs.current.set(0, ref);
                    }}
                    src={item.url}
                    alt={item.alt}
                    muted={true}
                    loop={true}
                    onError={() => handleMediaError(item.url)}
                    style={{
                        width: '100%',
                        height: 'var(--media-card-h)',
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
                onSelect={handleSlideChange}
                activeIndex={activeIndex}
                indicators={validMediaItems.length > 1}
                controls={validMediaItems.length > 1}
                wrap={true}
            >
                {validMediaItems.map((item, index) => (
                    <Carousel.Item key={`${item.type}-${index}`}>
                        {item.type === 'image' ? (
                            <LazyImage
                                className="d-block w-100"
                                src={item.url}
                                alt={item.alt}
                                onError={() => handleMediaError(item.url)}
                                style={{
                                    height: 'var(--media-card-h)',
                                    objectFit: 'cover'
                                }}
                            />
                        ) : (
                            <VideoPlayer
                                ref={(ref) => {
                                    if (ref) videoRefs.current.set(index, ref);
                                    else videoRefs.current.delete(index);
                                }}
                                src={item.url}
                                alt={item.alt}
                                muted={true}
                                loop={true}
                                onError={() => handleMediaError(item.url)}
                                style={{
                                    width: '100%',
                                    height: 'var(--media-card-h)',
                                    objectFit: 'cover'
                                }}
                            />
                        )}
                    </Carousel.Item>
                ))}
            </Carousel>
        </div>
    );
};

export default MediaCarousel;
