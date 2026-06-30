import React, { useState, useEffect, useRef, CSSProperties } from 'react'

interface LazyImageProps {
    src: string
    alt: string
    className?: string
    style?: CSSProperties
    onClick?: () => void
    onError?: () => void
    placeholder?: string
    showLoader?: boolean
}

/**
 * Компонент для ленивой загрузки изображений с placeholder'ом и индикатором загрузки
 * Использует native lazy loading браузера + progressive loading эффект
 */
const LazyImage: React.FC<LazyImageProps> = ({
    src,
    alt,
    className = '',
    style = {},
    onClick,
    onError,
    placeholder,
    showLoader = true
}) => {
    const [imageLoaded, setImageLoaded] = useState(false)
    const [imageError, setImageError] = useState(false)
    const imgRef = useRef<HTMLImageElement>(null)

    // SVG placeholder если не указан custom
    const defaultPlaceholder = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="250" viewBox="0 0 400 250"%3E%3Crect width="400" height="250" fill="%23f4f1ea"/%3E%3Ctext x="50%25" y="50%25" dominant-baseline="middle" text-anchor="middle" font-family="Arial, sans-serif" font-size="16" fill="%238b7355"%3E🌿 Загрузка...%3C/text%3E%3C/svg%3E'

    const errorPlaceholder = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="250" viewBox="0 0 400 250"%3E%3Crect width="400" height="250" fill="%23e9ecef"/%3E%3Ctext x="50%25" y="50%25" dominant-baseline="middle" text-anchor="middle" font-family="Arial, sans-serif" font-size="16" fill="%236c757d"%3E🌿 Изображение недоступно%3C/text%3E%3C/svg%3E'

    // Сброс состояния при изменении src.
    // ВАЖНО: для уже закэшированных изображений (например, при перезагрузке
    // страницы по F5) браузер завершает загрузку мгновенно, и событие `load`
    // успевает выстрелить ДО того, как React навесит обработчик onLoad —
    // тогда onLoad теряется и спиннер висит вечно. Поэтому после коммита
    // проверяем фактическое состояние <img> через ref.
    useEffect(() => {
        const img = imgRef.current
        if (img && img.complete) {
            if (img.naturalWidth > 0) {
                // изображение уже загружено из кэша
                setImageLoaded(true)
                setImageError(false)
            } else {
                // загрузка завершилась без размеров → битое изображение
                setImageLoaded(false)
                setImageError(true)
                onError?.()
            }
        } else {
            setImageLoaded(false)
            setImageError(false)
        }
        // onError намеренно не в зависимостях: проп пересоздаётся на каждый
        // рендер, а реагировать нужно только на смену src
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [src])

    const handleLoad = () => {
        setImageLoaded(true)
    }

    const handleError = () => {
        setImageError(true)
        onError?.()
    }

    // Стили для плавного появления
    const imageStyle: CSSProperties = {
        ...style,
        opacity: imageLoaded ? 1 : 0,
        transition: 'opacity 0.3s ease-in-out'
    }

    // Стили для loader'а
    const loaderStyle: CSSProperties = {
        position: 'absolute',
        top: '50%',
        left: '50%',
        transform: 'translate(-50%, -50%)',
        zIndex: 1
    }

    const containerStyle: CSSProperties = {
        position: 'relative',
        width: style.width || '100%',
        height: style.height || 'auto'
    }

    // Если ошибка загрузки - показываем error placeholder
    if (imageError) {
        return (
            <img
                src={errorPlaceholder}
                alt={`${alt} - ошибка загрузки`}
                className={className}
                style={style}
                onClick={onClick}
            />
        )
    }

    // Стили оверлея (placeholder + спиннер) — поверх основного изображения,
    // чтобы placeholder не отрисовывался отдельным блоком в потоке и не
    // переполнял контейнер, перекрывая тело карточки и блокируя клики.
    const overlayImageStyle: CSSProperties = {
        ...style,
        position: 'absolute',
        top: 0,
        left: 0,
        width: '100%',
        height: '100%'
    }

    return (
        <div style={containerStyle}>
            {/* Основное изображение с lazy loading (в потоке — задаёт высоту контейнера) */}
            <img
                ref={imgRef}
                src={src}
                alt={alt}
                className={className}
                style={imageStyle}
                onClick={onClick}
                onLoad={handleLoad}
                onError={handleError}
                loading="lazy"
                decoding="async"
            />

            {/* Placeholder изображение поверх, пока основное грузится */}
            {!imageLoaded && (placeholder || defaultPlaceholder) && (
                <img
                    src={placeholder || defaultPlaceholder}
                    alt={`${alt} - загрузка`}
                    className={className}
                    style={overlayImageStyle}
                    aria-hidden="true"
                />
            )}

            {/* Loader spinner пока изображение загружается */}
            {showLoader && !imageLoaded && (
                <div style={loaderStyle}>
                    <div
                        className="spinner-border text-success"
                        role="status"
                        style={{ width: '2rem', height: '2rem' }}
                    >
                        <span className="visually-hidden">Загрузка изображения...</span>
                    </div>
                </div>
            )}
        </div>
    )
}

export default LazyImage
