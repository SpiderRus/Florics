import { Media, MediaType } from '../services/goodsService';

export interface MediaItem {
    type: MediaType;
    url: string;
    alt: string;
    order: number;
}

/**
 * Конвертирует Media[] в MediaItem[] для использования в компонентах
 */
export function convertMediaToItems(
    media: Media[],
    goodsName: string
): MediaItem[] {
    return media
        .sort((a, b) => a.order - b.order)
        .map((item, index) => ({
            type: item.type,
            url: item.url,
            alt: `${goodsName} - ${item.type === 'image' ? 'фото' : 'видео'} ${index + 1}`,
            order: item.order
        }));
}
