import React, { useState, useEffect } from 'react';
import { VideoPlayerHandle } from './VideoPlayer';

interface VideoControlsProps {
    videoRef: React.RefObject<VideoPlayerHandle>;
}

const VideoControls: React.FC<VideoControlsProps> = ({ videoRef }) => {
    const [isPlaying, setIsPlaying] = useState(false);
    const [currentTime, setCurrentTime] = useState(0);
    const [duration, setDuration] = useState(0);
    const [isMuted, setIsMuted] = useState(true);

    useEffect(() => {
        const videoElement = videoRef.current?.getVideoElement();
        if (!videoElement) return;

        const handleTimeUpdate = () => {
            setCurrentTime(videoElement.currentTime);
        };

        const handleLoadedMetadata = () => {
            setDuration(videoElement.duration);
        };

        const handlePlay = () => setIsPlaying(true);
        const handlePause = () => setIsPlaying(false);

        videoElement.addEventListener('timeupdate', handleTimeUpdate);
        videoElement.addEventListener('loadedmetadata', handleLoadedMetadata);
        videoElement.addEventListener('play', handlePlay);
        videoElement.addEventListener('pause', handlePause);

        // Инициализация при монтировании
        if (videoElement.duration)
            setDuration(videoElement.duration);
        setCurrentTime(videoElement.currentTime);
        setIsPlaying(!videoElement.paused);
        setIsMuted(videoElement.muted);

        return () => {
            videoElement.removeEventListener('timeupdate', handleTimeUpdate);
            videoElement.removeEventListener('loadedmetadata', handleLoadedMetadata);
            videoElement.removeEventListener('play', handlePlay);
            videoElement.removeEventListener('pause', handlePause);
        };
    }, [videoRef]);


    const handlePlayPause = () => {
        if (isPlaying)
            videoRef.current?.pause();
        else
            videoRef.current?.play();
    };

    const handleSeek = (e: React.ChangeEvent<HTMLInputElement>) => {
        const time = parseFloat(e.target.value);
        videoRef.current?.setCurrentTime(time);
        setCurrentTime(time);
    };

    const handleMuteToggle = () => {
        const videoElement = videoRef.current?.getVideoElement();
        if (videoElement) {
            videoElement.muted = !videoElement.muted;
            setIsMuted(videoElement.muted);
        }
    };

    const formatTime = (seconds: number): string => {
        if (!isFinite(seconds)) return '0:00';
        const mins = Math.floor(seconds / 60);
        const secs = Math.floor(seconds % 60);
        return `${mins}:${secs.toString().padStart(2, '0')}`;
    };

    const progress = duration > 0 ? (currentTime / duration) * 100 : 0;

    return (
        <div className="video-controls">
            <div className="video-progress-container">
                <div className="video-progress-bar" style={{ width: `${progress}%` }} />
                <input
                    type="range"
                    className="video-seek-bar"
                    min="0"
                    max={duration || 0}
                    step="0.1"
                    value={currentTime}
                    onChange={handleSeek}
                    aria-label="Перемотка видео"
                />
            </div>
            <div className="video-controls-buttons">
                <button
                    type="button"
                    className="video-control-btn"
                    onClick={handlePlayPause}
                    aria-label={isPlaying ? 'Пауза' : 'Воспроизвести'}
                >
                    {isPlaying ? '⏸' : '▶'}
                </button>
                <button
                    type="button"
                    className="video-control-btn"
                    onClick={handleMuteToggle}
                    aria-label={isMuted ? 'Включить звук' : 'Выключить звук'}
                >
                    {isMuted ? '🔇' : '🔊'}
                </button>
                <span className="video-time">
                    {formatTime(currentTime)} / {formatTime(duration)}
                </span>
            </div>
        </div>
    );
};

export default VideoControls;
