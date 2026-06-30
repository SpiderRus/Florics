import { useEffect, useRef } from 'react';

/**
 * Общая логика автоскролла чата для AiChatBot и FlorariumChatBot.
 *
 * «Прилипание» к низу: пока пользователь у нижнего края — автоскролл при новых сообщениях
 * и догрузке контента; как только он прокрутил вверх — автоскролл выключается, чтобы не
 * вырывать его из чтения истории.
 *
 * @param dep значение, изменение которого триггерит автоскролл (обычно массив сообщений)
 */
export function useChatScroll(dep: unknown) {
    const containerRef = useRef<HTMLDivElement>(null);
    const stickToBottomRef = useRef(true);

    const scrollToBottom = (instant = false) => {
        const c = containerRef.current;
        if (c) c.scrollTo({ top: c.scrollHeight, behavior: instant ? 'auto' : 'smooth' });
    };

    // Повесить на onScroll контейнера: обновляет «прилипание» по близости к нижнему краю
    const handleScroll = () => {
        const c = containerRef.current;
        if (!c) return;
        stickToBottomRef.current = c.scrollHeight - c.scrollTop - c.clientHeight < 120;
    };

    // Доскролл к низу, если пользователь «прилип» (например, догрузилась картинка и выросла высота)
    const scrollIfSticky = () => {
        if (stickToBottomRef.current) scrollToBottom(true);
    };

    // Автоскролл при изменении зависимости (новые сообщения), если пользователь у низа
    useEffect(() => {
        if (stickToBottomRef.current) scrollToBottom(true);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [dep]);

    return { containerRef, scrollToBottom, handleScroll, scrollIfSticky };
}
