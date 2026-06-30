# AI Bot Service Integration

Reactive WebClient клиент чат-бота **OllamaTestController** (проект AIAgentNew).

Бот сам хранит историю (по `chatId`), делает RAG автоматически и умеет стриминг (SSE).
GreenDecor добавляет аутентификацию (роль BUYER), изоляцию разговоров по пользователям и
привязку к товарам. Контракт `/api/aibot/**` для фронтенда стабилен; за ним стоит [AiBotService].

## Компоненты

- **AiBotService** — клиент бота (ensure/stream/send/get/delete)
- **AiConversationRepository** — маппинг `conversationId (== chatId) → userId, goodsId` в PostgreSQL (`ai_conversations`)
- **DTOs** — `CreateChatRequest` (к боту), `OllamaHistoryMessage` (история бота), `TokenChunk` (SSE к фронту);
  `ConversationResponse`/`MessageResponse`/`ChatResponse` — синтезируем сами (контракт к фронту)
- **Exceptions** — кастомные исключения для обработки ошибок

## Контракт бота (base = `.../api/v1/ollama`)

| Действие | Бот |
|---|---|
| Создать сессию (идемпотентно по `chatId`+`agentType`) | `POST /chat` `{agentType, chatId, topic}` → 201 |
| Стриминг ответа | `POST /{chatId}/chat/stream`, тело — сырой текст, SSE токенов |
| Ответ целиком (fallback) | `POST /{chatId}/chat`, тело — сырой текст → `String` |
| История | `GET /{chatId}/chat` → `[{id, role, content, createdAt}]` |
| Удаление | **отсутствует** (чистим только локальный маппинг) |

Контекст товара передаётся боту через `topic` (уходит в системный промпт, а не в видимую историю).

## Изоляция пользователей

- При создании conversation маппинг сохраняется в `AiConversationRepository`.
- При доступе к conversation проверяется, что текущий `userId` совпадает с владельцем.
- Чужой conversation → `ConversationAccessDeniedException`.

## Обработка ошибок

- `ConversationNotFoundException` — разговор не найден в локальном маппинге
- `ConversationAccessDeniedException` — доступ запрещён (не владелец)
- `AiBotServiceException` — ошибка связи с ботом или серверная ошибка
- `AiBotTimeoutException` — таймаут запроса

## Конфигурация (`application.yml`)

```yaml
ai-agent:
  base-url: http://localhost:8081   # AIAgentNew по умолчанию слушает 8080 — запускать на 8081
  base-path: /api/v1/ollama
  agent-type: plants
  connect-timeout: 5000
  read-timeout: 120000              # первый токен может приходить дольше (RAG + tool-calls)
```

## Требования

- AIAgentNew запущен на `http://localhost:8081` (порт отличается от GreenDecor 8080).
- Пользователь авторизован с ролью BUYER (userId через `SecurityUtils`).
