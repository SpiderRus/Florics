import React, { forwardRef, useImperativeHandle, useRef, useState } from 'react';

interface VideoPlayerProps {
    src: string;
    alt: string;
    muted?: boolean;
    loop?: boolean;
    style?: React.CSSProperties;
    onError?: () => void;
    onLoadedData?: () => void;
}

export interface VideoPlayerHandle {
    play: () => Promise<void>;
    pause: () => void;
    getCurrentTime: () => number;
    getDuration: () => number;
    setCurrentTime: (time: number) => void;
    getVideoElement: () => HTMLVideoElement | null;
    isPaused: () => boolean;
}

const VideoPlayer = forwardRef<VideoPlayerHandle, VideoPlayerProps>((props, ref) => {
    const { src, alt, muted = true, loop = true, style, onError, onLoadedData } = props;
    const videoRef = useRef<HTMLVideoElement>(null);
    const [hasError, setHasError] = useState(false);

    useImperativeHandle(ref, () => ({
        play: async () => {
            try {
                if (videoRef.current)
                    await videoRef.current.play();
            } catch (err) {
                console.warn('Video autoplay failed:', err);
            }
        },
        pause: () => {
            if (videoRef.current)
                videoRef.current.pause();
        },
        getCurrentTime: () => videoRef.current?.currentTime || 0,
        getDuration: () => videoRef.current?.duration || 0,
        setCurrentTime: (time: number) => {
            if (videoRef.current)
                videoRef.current.currentTime = time;
        },
        getVideoElement: () => videoRef.current,
        isPaused: () => videoRef.current?.paused || true
    }));

    const handleError = () => {
        setHasError(true);
        onError?.();
    };

    if (hasError)
        return (
            <div className="video-placeholder" style={style}>
                <div style={{ textAlign: 'center', color: 'var(--forest-green)' }}>
                    🎬
                    <div>Видео недоступно</div>
                </div>
            </div>
        );

    return (
        <video
            ref={videoRef}
            src={src}
            muted={muted}
            loop={loop}
            playsInline
            controls={false}
            style={style}
            onError={handleError}
            onLoadedData={onLoadedData}
            aria-label={alt}
        />
    );
});

VideoPlayer.displayName = 'VideoPlayer';

export default VideoPlayer;
