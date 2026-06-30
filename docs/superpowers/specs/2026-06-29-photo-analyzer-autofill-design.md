# Спецификация: автозаполнение карточки товара по фото (photo-analyzer)

**Дата:** 2026-06-29
**Статус:** утверждён дизайн, ожидается план реализации

## Цель

Во вкладке «Фотографии» формы создания/редактирования товара добавить кнопку **«🔍 Анализировать фото»** (активна, когда есть хотя бы одно фото). По клику фотографии товара отправляются AI-боту (`agentType=photo-analyzer`) через эндпоинт AI Agent `ChatController.chatStreamWithImages()`; бот возвращает `PlantCard`, которым **перезаписываются** соответствующие поля формы. Весь вызов сопровождается спиннером ожидания на клиенте.

## Контракт AI Agent (внешний, AIAgentNew)

- База бота для Florics: `http://localhost:8081/api/v1/ollama` (тот же базовый путь, что у существующего чата — переиспользуем `aiAgentWebClient`).
- **Двухшаговый вызов:**
  1. `POST /api/v1/ollama/chat` — тело `CreateChatRequest { agentType:"photo-analyzer", chatId, topic:null }` (создаёт сессию; уже умеет `AiBotService.ensureConversationWithAgent`).
  2. `POST /api/v1/ollama/{chatId}/chat/stream-with-images` — тело `ChatStreamRequest { message:String, images:List<String> }`, где `images` — base64 или data-URL (`data:<mime>;base64,<payload>`). `Accept: text/event-stream`.
- **Ответ:** SSE, но **не токены** — одно событие, `data:` которого = целиком сериализованный JSON `PlantCard`. (Бот считает карточку полностью двухпроходно и эмитит одной строкой.)
- **PlantCard** (snake_case JSON, все поля nullable): `name`, `short_description`, `full_description`, `care`, `error`. На успехе `name..care` заполнены, `error=null`. На неудаче (нет изображения / не растение / ошибка модели) — контент пуст, `error` содержит причину (бот никогда не отдаёт 500). `null`-поля опускаются из JSON.
- Эндпоинт без auth/CORS (server-to-server из Florics — ок).

## Зафиксированные решения

| Развилка | Решение |
|---|---|
| Размещение кнопки | Внутри вкладки «Фотографии» (`MediaManager`), рядом с кнопками добавления |
| Активность кнопки | Активна при `photos.length ≥ 1`; заблокирована во время анализа |
| Спиннер | На клиенте: при анализе кнопка показывает `<Spinner>` + текст «Анализ фотографий…», заблокирована |
| Применение результата | **Перезаписать** все 4 поля (явное действие) |
| Маппинг полей | `name`→Название, `short_description`→Описание, `full_description`→Детальное описание, `care`→Уход |
| Категория | На **успешном** анализе (бот вернул поля) дополнительно ставить категорию типа `PLANT` (`categories.find(c => c.type === 'PLANT')`) |
| Какие фото | **Все** фото из вкладки (бот сам выбирает главное растение) |
| Не трогаем | price, сложность, длительность, видео (бот их не возвращает) |

## Разрешение байтов фото (ключевой нюанс)

Во вкладке три вида фото, и читать их байты по-разному:
- **Новые blob'ы** (`db-new`, `p.file`) — байты только в браузере → клиент шлёт **сами байты** (multipart part).
- **Сохранённые db** (`p.url` начинается с `/api/media/`) — байты в `media_content` → клиент шлёт **`mediaId`**, Florics читает на сервере (`MediaRepository.loadContent`).
- **Внешние URL** (`p.url` не `/api/media/`) — чтение байтов в браузере ломает CORS → клиент шлёт **`url`**, Florics делает **server-side fetch** (CORS не действует).

Порядок фото для анализа неважен (бот берёт главное растение), поэтому строгий порядок между частями/ссылками не сохраняем.

## Backend (Florics)

### Конфиг
`AiAgentProperties`: добавить `photoAnalyzerAgentType: String = "photo-analyzer"`. (Опционально в `application.yml` `ai-agent.photo-analyzer-agent-type`.)

### DTO
`PlantCardDto` — парсит ответ бота (snake_case) и отдаётся фронту:
```
name: String?, shortDescription: String?, fullDescription: String?, care: String?, error: String?
```
`shortDescription`/`fullDescription` маппятся на JSON-ключи `short_description`/`full_description` (через @JsonProperty или snake-case ридер; конкретный механизм — на этапе реализации, т.к. проект на Jackson 3 `tools.jackson`).

Вспомогательный `ImageBytes(bytes: ByteArray, contentType: String)` — внутренний носитель резолвленного фото.

### Сервис
`AiBotService.analyzePhotos(images: List<ImageBytes>): PlantCardDto`:
1. `chatId = UUID`; `ensureConversationWithAgent(properties.photoAnalyzerAgentType, chatId, null)`.
2. `POST /{chatId}/chat/stream-with-images` (`contentType=application/json`, `accept=text/event-stream`) с телом `{ message:"", images: images.map { "data:${it.contentType};base64,${base64(it.bytes)}" } }`.
3. Прочитать поток существующим `sseDataFlow` (байтовый SSE-парсер) → собрать единственную data-строку (`toList().joinToString("")`/`firstOrNull`) → `objectMapper.readValue(json, PlantCardDto::class.java)`.
4. Ошибки связи/таймаут → пробросить как `AiBotServiceException`/`AiBotTimeoutException` (как в остальных методах). Таймаут чтения уже 120с.
   Эфемерно: без записи в `ai_conversations`.

### Эндпоинт
Новый контроллер (или метод) `POST /api/admin/goods/analyze-photos`, `@PreAuthorize("hasRole('ADMIN')")`, `consumes=multipart/form-data`:
- `@RequestPart("files") files: Flux<FilePart>` (0..n blob-фото) — опционально;
- `@RequestPart("mediaIds") mediaIds: String?` — JSON-массив db-id'шников;
- `@RequestPart("urls") urls: String?` — JSON-массив внешних URL.

Логика (новый `PhotoAnalyzeService` или метод в `MediaService`):
1. Собрать `images: List<ImageBytes>`:
   - из `files`: байты части + `headers().contentType` (деф. `image/jpeg`);
   - из `mediaIds`: `MediaRepository.loadContent(id)` → `ImageBytes(content, contentType)`;
   - из `urls`: server-side fetch (лёгкий `WebClient.create()` с лимитом размера и таймаутом) → байты + `Content-Type` ответа (деф. `image/jpeg`); неудачный fetch — пропускаем (best-effort).
2. Если `images` пуст → вернуть `PlantCardDto(error="Не удалось получить изображения для анализа")`.
3. `AiBotService.analyzePhotos(images)` → вернуть `PlantCardDto` как JSON.

### Безопасность/лимиты
- Путь под `/api/admin/**` → требует auth + ADMIN (через `@PreAuthorize`). Новых `permitAll` не нужно.
- Multipart-лимиты Florics: проверить `spring.webflux.multipart.max-in-memory-size`/`max-parts`/`max-disk-usage` — несколько сжатых фото (≤2 МБ каждое) могут превысить дефолты; при необходимости поднять.
- Размер тела base64 → AI Agent: вне нашего контроля (эндпоинт бота спроектирован принимать base64-в-JSON).

## Frontend

### `MediaManager`
Новые пропсы: `onAnalyze: () => void`, `analyzing: boolean`. Кнопка «🔍 Анализировать фото» рядом с «Загрузить файлы»/«Сделать фото»:
- `disabled={photos.length === 0 || analyzing}`;
- при `analyzing` — `<Spinner size="sm">` + «Анализ фотографий…»;
- `onClick={onAnalyze}`.

### `adminService.analyzePhotos(photos): Promise<PlantCard>`
Собирает `FormData`:
- `files`: каждый `p.file` (blob) → `append('files', blob, 'photo.jpg')`;
- `mediaIds`: `JSON.stringify(photos.filter(p => !p.file && p.url?.startsWith('/api/media/') && p.mediaId).map(p => p.mediaId))`;
- `urls`: `JSON.stringify(photos.filter(p => !p.file && p.url && !p.url.startsWith('/api/media/')).map(p => p.url))`;
`POST /admin/goods/analyze-photos` (multipart) → `PlantCard { name?, shortDescription?, fullDescription?, care?, error? }`.

### `GoodsForm`
- Состояние `analyzing: boolean`; категории уже загружаются (`categories: Category[]`).
- `handleAnalyze`:
  1. `setAnalyzing(true)`;
  2. `const card = await adminService.analyzePhotos(photos)`;
  3. если `card.error` → `toast.error(card.error)`;
  4. иначе перезаписать поля:
     ```
     setFormData(prev => ({
       ...prev,
       name: card.name ?? prev.name,
       description: card.shortDescription ?? prev.description,
       detailedDescription: card.fullDescription ?? prev.detailedDescription,
       careInstructions: card.care ?? prev.careInstructions,
       categoryId: categories.find(c => c.type === 'PLANT')?.id ?? prev.categoryId,
     }));
     toast.success('Поля заполнены по фото');
     ```
  5. `catch` → `toast.error('Не удалось проанализировать фото')`; `finally` → `setAnalyzing(false)`.
- Передать `onAnalyze={handleAnalyze}` и `analyzing` в `<MediaManager>`.

## Ошибки / edge
- Нет фото → кнопка неактивна.
- `PlantCard.error` (не растение / нет фото) → тост текста ошибки, поля не трогаем, категорию не ставим.
- Сеть/таймаут бота → общий тост «Не удалось проанализировать фото».
- Внешний fetch упал → пропускаем это фото; все упали → `images` пуст → error-карточка.

## Вне scope (YAGNI)
- Не трогаем price/сложность/длительность/видео (бот не возвращает).
- Без истории/маппинга разговора (одноразовый вызов, без записи в БД).
- Без настоящего стриминга (бот отдаёт одно событие — собираем целиком).
- Без предпросмотра/выбора полей (решено: перезапись всех 4).
- Порядок фото для анализа не сохраняем (бот берёт главное растение).

## Риски / проверки
- **Jackson 3 (`tools.jackson`) маппинг snake_case** `short_description`/`full_description` — подобрать рабочую аннотацию/настройку ридера на этапе реализации.
- **SSE-сбор**: убедиться, что `sseDataFlow` корректно собирает один большой JSON-event (несколько `data:`-строк склеиваются через `\n`).
- **Multipart-лимиты** Florics при нескольких фото — поднять при необходимости.
- **Внешний fetch**: лимит размера/таймаут, чтобы большой/зависший URL не вешал запрос.

## Verification
- `cd backend && mvn test` (компиляция + зелёные существующие) ; `cd frontend && npm run build` зелёные.
- Ручной QA на http://localhost:8080 (нужен запущенный AI Agent на :8081), аккаунт ADMIN `spiderru5597@gmail.com`:
  - создать товар, добавить фото растения, «🔍 Анализировать фото» → спиннер → поля Название/Описание/Детальное/Уход заполнились, категория стала «Растения»;
  - проверить смесь фото (blob + db + внешнее) — все доходят (db/внешние резолвятся на сервере);
  - не-растение / без фото → тост ошибки, поля не тронуты;
  - кнопка неактивна без фото и во время анализа.
