import { useState, useEffect } from 'react';

/**
 * Реактивно отслеживает соответствие CSS media-запросу.
 *
 * @example
 *   const isMobile = useMediaQuery('(max-width: 767.98px)');
 */
export function useMediaQuery(query: string): boolean {
    const getMatches = (): boolean => {
        if (typeof window === 'undefined' || !window.matchMedia) {
            return false;
        }
        return window.matchMedia(query).matches;
    };

    const [matches, setMatches] = useState<boolean>(getMatches);

    useEffect(() => {
        if (typeof window === 'undefined' || !window.matchMedia) {
            return;
        }

        const mediaQueryList = window.matchMedia(query);
        const handleChange = () => setMatches(mediaQueryList.matches);

        // Синхронизируемся на случай, если запрос изменился между рендерами
        handleChange();
        mediaQueryList.addEventListener('change', handleChange);

        return () => mediaQueryList.removeEventListener('change', handleChange);
    }, [query]);

    return matches;
}

export default useMediaQuery;
