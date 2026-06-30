-- Публичный бакет Google с демо-видео (commondatastorage.googleapis.com/gtv-videos-bucket)
-- стал отдавать HTTP 403, из-за чего видео мастер-классов и видео-медиа товаров не воспроизводились
-- (тег <video> получал ошибку "Format error" / NO_SOURCE).
-- Заменяем мёртвые URL на доступный CC0-сэмпл, чтобы плеер реально проигрывал контент.

UPDATE goods
SET video_url = 'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4'
WHERE video_url LIKE '%commondatastorage.googleapis.com%';

UPDATE media
SET url = 'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4'
WHERE type = 'VIDEO' AND url LIKE '%commondatastorage.googleapis.com%';
