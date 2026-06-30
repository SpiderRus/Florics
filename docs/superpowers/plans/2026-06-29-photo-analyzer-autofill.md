# Автозаполнение карточки товара по фото — план реализации

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Кнопка «🔍 Анализировать фото» во вкладке «Фотографии» формы товара отправляет все фото боту `photo-analyzer`, получает `PlantCard` и перезаписывает поля Название/Описание/Детальное/Уход (+ категория «Растения»), под спиннером.

**Architecture:** Клиент шлёт фото в Florics (blob'ы — байтами, db/внешние — ссылками); Florics резолвит байты на сервере, base64 → создаёт `photo-analyzer` сессию у AI Agent (переиспользуя `aiAgentWebClient`, тот же базовый путь `/api/v1/ollama`) → `POST /{chatId}/chat/stream-with-images` → читает единственное SSE-событие с JSON `PlantCard` → возвращает фронту → клиент перезаписывает поля.

**Tech Stack:** Backend — Spring Boot 4 WebFlux + Kotlin + Coroutines, WebClient, существующий `AiBotService`/`sseDataFlow`, `MediaRepository`. Frontend — React + TS + React-Bootstrap.

## Global Constraints

- **Unit-тесты не пишем.** Верификация: backend — `cd backend && mvn test` (компиляция + зелёные существующие); frontend — `cd frontend && npm run build`; затем ручной браузерный QA.
- **Коммиты не делаем** (решение пользователя) — код остаётся в рабочем дереве `main` рядом с прочими несохранёнными изменениями.
- Переиспользовать существующий `@Qualifier("aiAgentWebClient")` WebClient (база `http://localhost:8081/api/v1/ollama`). Нового бина для бота не создавать.
- Контракт бота: (1) `POST /chat {agentType:"photo-analyzer", chatId, topic:null}`; (2) `POST /{chatId}/chat/stream-with-images {message:String, images:List<String>}` (`images` = `data:<mime>;base64,<payload>`), ответ — **одно** SSE-событие = JSON `PlantCard` (snake_case: `name`, `short_description`, `full_description`, `care`, `error`).
- Маппинг: `name→name(Название)`, `short_description→description(Описание)`, `full_description→detailedDescription(Детальное)`, `care→careInstructions(Уход)`. На успехе доп. `categoryId = категория type==='PLANT'`. Не трогаем price/сложность/длительность/видео.
- Парсинг JSON бота — через `Map` (snake_case ключи), чтобы не зависеть от аннотаций Jackson 3 (`tools.jackson`).
- Для сквозного QA нужен запущенный AI Agent на :8081 с агентом `photo-analyzer`. Просмотр приложения — http://localhost:8080 (после фронт-правок: `npm run build` → рестарт `spring-boot:run`). QA-аккаунт ADMIN: `spiderru5597@gmail.com` / `$Spider74`. Bash предпочтительнее PowerShell.

## File Structure

**Backend**
- Modify `backend/.../config/AiAgentProperties.kt` — добавить `photoAnalyzerAgentType`.
- Create `backend/.../service/aibot/dto/PlantCardDto.kt` — DTO ответа.
- Create `backend/.../service/aibot/dto/ImageBytes.kt` — носитель резолвленного фото.
- Modify `backend/.../service/AiBotService.kt` — `analyzePhotos(images): PlantCardDto`.
- Create `backend/.../service/PhotoAnalyzeService.kt` — резолв файлов/mediaIds/urls → байты, оркестрация.
- Create `backend/.../controller/AdminPhotoAnalyzeController.kt` — `POST /api/admin/goods/analyze-photos`.

**Frontend**
- Modify `frontend/src/types/admin.ts` — интерфейс `PlantCard`.
- Modify `frontend/src/services/adminService.ts` — `analyzePhotos(photos)`.
- Modify `frontend/src/components/admin/MediaManager.tsx` — кнопка + пропсы `onAnalyze`/`analyzing`.
- Modify `frontend/src/components/admin/GoodsForm.tsx` — `analyzing`, `handleAnalyze`, проброс пропсов.

---

### Task 1: Backend — конфиг + DTO

**Files:**
- Modify: `backend/src/main/kotlin/com/example/webflux/config/AiAgentProperties.kt`
- Create: `backend/src/main/kotlin/com/example/webflux/service/aibot/dto/PlantCardDto.kt`
- Create: `backend/src/main/kotlin/com/example/webflux/service/aibot/dto/ImageBytes.kt`

**Interfaces:**
- Produces: `AiAgentProperties.photoAnalyzerAgentType: String`; `PlantCardDto(name, shortDescription, fullDescription, care, error)`; `ImageBytes(bytes: ByteArray, contentType: String)`.

- [ ] **Step 1: Добавить свойство в `AiAgentProperties`**

После поля `florariumExpertAgentType` (строка ~39) добавить:

```kotlin
    /**
     * Тип агента-анализатора фото растений (заполняет карточку товара по изображениям).
     */
    val photoAnalyzerAgentType: String = "photo-analyzer",
```

- [ ] **Step 2: Создать `PlantCardDto.kt`**

```kotlin
package com.example.webflux.service.aibot.dto

/**
 * Карточка растения от бота photo-analyzer. Отдаётся фронту как есть (camelCase).
 * На успехе name..care заполнены, error=null. На неудаче — content пуст, error содержит причину.
 */
data class PlantCardDto(
    val name: String? = null,
    val shortDescription: String? = null,
    val fullDescription: String? = null,
    val care: String? = null,
    val error: String? = null
)
```

- [ ] **Step 3: Создать `ImageBytes.kt`**

```kotlin
package com.example.webflux.service.aibot.dto

/** Резолвленное изображение (байты + mime) для отправки боту. */
data class ImageBytes(
    val bytes: ByteArray,
    val contentType: String
)
```

- [ ] **Step 4: Компиляция**

Run: `cd /c/Users/spide/WORK/Florics/backend && mvn -q compile`
Expected: BUILD SUCCESS.

---

### Task 2: Backend — `AiBotService.analyzePhotos`

**Files:**
- Modify: `backend/src/main/kotlin/com/example/webflux/service/AiBotService.kt`

**Interfaces:**
- Consumes: `properties.photoAnalyzerAgentType` (Task 1), `PlantCardDto`, `ImageBytes`, существующие `ensureConversationWithAgent`, `sseDataFlow`.
- Produces: `suspend fun analyzePhotos(images: List<ImageBytes>): PlantCardDto`.

- [ ] **Step 1: Добавить импорты `PlantCardDto`/`ImageBytes`**

В блоке импортов `import com.example.webflux.service.aibot.dto.*` уже есть (строка 9) — оба DTO в этом пакете, отдельные импорты не нужны. `java.util.*` (Base64, UUID) уже импортирован.

- [ ] **Step 2: Добавить метод `analyzePhotos`** (вставить после метода `sendMessage`, перед `getMessages`)

```kotlin
    /**
     * Анализ фотографий растения ботом (agentType=photo-analyzer).
     *
     * Одноразовый вызов: генерируем chatId, создаём сессию, отправляем картинки (data-URL в JSON),
     * читаем единственное SSE-событие с JSON PlantCard. Маппинг разговора в БД не ведём.
     * JSON парсим через Map (ключи snake_case), чтобы не зависеть от аннотаций Jackson.
     */
    suspend fun analyzePhotos(images: List<ImageBytes>): PlantCardDto {
        val chatId = UUID.randomUUID().toString()
        ensureConversationWithAgent(properties.photoAnalyzerAgentType, chatId, null)

        val dataUrls = images.map {
            "data:${it.contentType};base64," + Base64.getEncoder().encodeToString(it.bytes)
        }

        val json = try {
            val rawChunks = webClient.post()
                .uri("/{chatId}/chat/stream-with-images", chatId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(mapOf("message" to "", "images" to dataUrls))
                .retrieve()
                .bodyToFlow<DataBuffer>()
            sseDataFlow(rawChunks).toList().joinToString("")
        } catch (e: WebClientRequestException) {
            logger.error("Failed to connect to AI bot (photo-analyzer)", e)
            throw AiBotServiceException("Failed to connect to AI bot", e)
        } catch (e: TimeoutException) {
            logger.error("AI bot photo-analyzer timed out", e)
            throw AiBotTimeoutException("AI bot request timed out", e)
        }

        if (json.isBlank()) throw AiBotServiceException("Empty photo-analyzer response")
        return parsePlantCard(json)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parsePlantCard(json: String): PlantCardDto {
        val map = objectMapper.readValue(json, Map::class.java) as Map<String, Any?>
        return PlantCardDto(
            name = map["name"] as? String,
            shortDescription = map["short_description"] as? String,
            fullDescription = map["full_description"] as? String,
            care = map["care"] as? String,
            error = map["error"] as? String
        )
    }
```

- [ ] **Step 3: Компиляция**

Run: `cd /c/Users/spide/WORK/Florics/backend && mvn -q compile`
Expected: BUILD SUCCESS.

---

### Task 3: Backend — `PhotoAnalyzeService`

**Files:**
- Create: `backend/src/main/kotlin/com/example/webflux/service/PhotoAnalyzeService.kt`

**Interfaces:**
- Consumes: `MediaRepository.loadContent` (есть), `AiBotService.analyzePhotos` (Task 2), `ImageBytes`, `PlantCardDto`, `AiBotServiceException`/`AiBotTimeoutException`.
- Produces: `suspend fun analyze(files: List<FilePart>, mediaIdsJson: String?, urlsJson: String?): PlantCardDto`.

- [ ] **Step 1: Создать `PhotoAnalyzeService.kt`**

```kotlin
package com.example.webflux.service

import com.example.webflux.repository.MediaRepository
import com.example.webflux.service.aibot.AiBotServiceException
import com.example.webflux.service.aibot.AiBotTimeoutException
import com.example.webflux.service.aibot.dto.ImageBytes
import com.example.webflux.service.aibot.dto.PlantCardDto
import tools.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

/**
 * Резолвит фотографии товара (blob-файлы + db по mediaId + внешние по url) в байты и
 * отправляет их боту photo-analyzer. db-байты читаются из media_content, внешние —
 * server-side fetch (CORS не действует). Неудачные внешние — пропускаются (best-effort).
 */
@Service
class PhotoAnalyzeService(
    private val mediaRepository: MediaRepository,
    private val aiBotService: AiBotService,
    private val objectMapper: ObjectMapper
) {
    // Отдельный клиент с поднятым лимитом — внешняя картинка может быть > 256 КБ (дефолт кодека).
    private val externalClient = WebClient.builder()
        .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }
        .build()

    suspend fun analyze(files: List<FilePart>, mediaIdsJson: String?, urlsJson: String?): PlantCardDto {
        val images = mutableListOf<ImageBytes>()

        // 1) Новые blob-файлы — байты от клиента
        for (f in files) {
            val ct = f.headers().contentType?.toString() ?: "image/jpeg"
            images += ImageBytes(f.readBytesJoined(), ct)
        }
        // 2) Сохранённые db-фото — байты из media_content
        parseStringArray(mediaIdsJson).forEach { id ->
            mediaRepository.loadContent(id)?.let { images += ImageBytes(it.content, it.contentType) }
        }
        // 3) Внешние URL — server-side fetch
        parseStringArray(urlsJson).forEach { url ->
            fetchExternal(url)?.let { images += it }
        }

        if (images.isEmpty()) {
            return PlantCardDto(error = "Не удалось получить изображения для анализа")
        }

        return try {
            aiBotService.analyzePhotos(images)
        } catch (e: AiBotServiceException) {
            logger.error("Photo-analyzer call failed", e)
            PlantCardDto(error = "AI-сервис недоступен, попробуйте позже")
        } catch (e: AiBotTimeoutException) {
            logger.error("Photo-analyzer timed out", e)
            PlantCardDto(error = "Анализ занял слишком долго, попробуйте ещё раз")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseStringArray(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            (objectMapper.readValue(json, List::class.java) as List<Any?>).mapNotNull { it as? String }
        } catch (e: Exception) {
            logger.warn("Failed to parse refs JSON: {}", json)
            emptyList()
        }
    }

    private suspend fun fetchExternal(url: String): ImageBytes? = try {
        externalClient.get().uri(url).retrieve()
            .exchangeToMono { resp ->
                resp.bodyToMono(ByteArray::class.java)
                    .map { ImageBytes(it, resp.headers().contentType?.toString() ?: "image/jpeg") }
            }
            .timeout(Duration.ofSeconds(15))
            .awaitFirstOrNull()
    } catch (e: Exception) {
        logger.warn("Failed to fetch external image {}: {}", url, e.message)
        null
    }

    private suspend fun FilePart.readBytesJoined(): ByteArray {
        val buffer = DataBufferUtils.join(content()).awaitSingle()
        val bytes = ByteArray(buffer.readableByteCount())
        buffer.read(bytes)
        DataBufferUtils.release(buffer)
        return bytes
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(PhotoAnalyzeService::class.java)
    }
}
```

- [ ] **Step 2: Компиляция**

Run: `cd /c/Users/spide/WORK/Florics/backend && mvn -q compile`
Expected: BUILD SUCCESS.

---

### Task 4: Backend — `AdminPhotoAnalyzeController`

**Files:**
- Create: `backend/src/main/kotlin/com/example/webflux/controller/AdminPhotoAnalyzeController.kt`

**Interfaces:**
- Consumes: `PhotoAnalyzeService.analyze` (Task 3).
- Produces: `POST /api/admin/goods/analyze-photos` (multipart) → `PlantCardDto`.

- [ ] **Step 1: Создать `AdminPhotoAnalyzeController.kt`**

```kotlin
package com.example.webflux.controller

import com.example.webflux.service.PhotoAnalyzeService
import com.example.webflux.service.aibot.dto.PlantCardDto
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.FormFieldPart
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange

/**
 * Анализ фотографий товара ботом photo-analyzer (вкладка «Фотографии»).
 * Принимает multipart: file-части `files` (blob-фото) + поля `mediaIds`/`urls` (JSON-массивы
 * ссылок на сохранённые/внешние фото). Возвращает PlantCard для автозаполнения формы.
 */
@RestController
@RequestMapping("/api/admin/goods")
@PreAuthorize("hasRole('ADMIN')")
class AdminPhotoAnalyzeController(
    private val photoAnalyzeService: PhotoAnalyzeService
) {
    @PostMapping("/analyze-photos", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun analyze(exchange: ServerWebExchange): PlantCardDto {
        val parts = exchange.multipartData.awaitSingle()
        val files = parts["files"]?.filterIsInstance<FilePart>() ?: emptyList()
        val mediaIds = (parts["mediaIds"]?.firstOrNull() as? FormFieldPart)?.value()
        val urls = (parts["urls"]?.firstOrNull() as? FormFieldPart)?.value()
        return photoAnalyzeService.analyze(files, mediaIds, urls)
    }
}
```

- [ ] **Step 2: Компиляция + существующие тесты**

Run: `cd /c/Users/spide/WORK/Florics/backend && mvn -q test`
Expected: BUILD SUCCESS, существующие тесты зелёные.

---

### Task 5: Frontend — тип `PlantCard` + `adminService.analyzePhotos`

**Files:**
- Modify: `frontend/src/types/admin.ts`
- Modify: `frontend/src/services/adminService.ts`

**Interfaces:**
- Consumes: `PhotoDraft` (тип из `MediaManager`).
- Produces: `PlantCard`; `adminService.analyzePhotos(photos: PhotoDraft[]): Promise<PlantCard>`.

- [ ] **Step 1: Добавить тип `PlantCard` в конец `types/admin.ts`**

```ts

export interface PlantCard {
    name?: string;
    shortDescription?: string;
    fullDescription?: string;
    care?: string;
    error?: string;
}
```

- [ ] **Step 2: Расширить импорты в `adminService.ts`**

Заменить строку импорта типов:

```ts
import { CreateGoodsRequest, UpdateGoodsRequest, PagedResponse, PaginationParams, AdminMedia, MediaReconcileItem, PlantCard } from '../types/admin';
```

И добавить отдельной строкой (type-only импорт `PhotoDraft`, рантайм-цикла нет — `MediaManager` не импортирует `adminService`):

```ts
import type { PhotoDraft } from '../components/admin/MediaManager';
```

- [ ] **Step 3: Добавить метод `analyzePhotos`** (перед закрывающей `};` объекта `adminService`, после `reconcileGoodsMedia` — не забыть запятую после него)

```ts
    ,

    // Анализ фото товара ботом photo-analyzer → автозаполнение карточки
    analyzePhotos: async (photos: PhotoDraft[]): Promise<PlantCard> => {
        const formData = new FormData();
        for (const p of photos) {
            if (p.file) formData.append('files', p.file, 'photo.jpg');
        }
        const mediaIds = photos
            .filter(p => !p.file && p.url?.startsWith('/api/media/') && p.mediaId)
            .map(p => p.mediaId!);
        const urls = photos
            .filter(p => !p.file && p.url && !p.url.startsWith('/api/media/'))
            .map(p => p.url!);
        formData.append('mediaIds', JSON.stringify(mediaIds));
        formData.append('urls', JSON.stringify(urls));
        const response = await axiosInstance.post('/admin/goods/analyze-photos', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        });
        return response.data;
    }
```

> Примечание: `reconcileGoodsMedia` сейчас заканчивается `}` без запятой (последний метод). Добавляемый блок начинается с `,` — это корректно превращает его в новый элемент объекта. Убедиться, что между ними ровно одна запятая.

- [ ] **Step 4: Сборка**

Run: `cd /c/Users/spide/WORK/Florics/frontend && npm run build`
Expected: `✓ built` без ошибок TS.

---

### Task 6: Frontend — кнопка анализа в `MediaManager`

**Files:**
- Modify: `frontend/src/components/admin/MediaManager.tsx`

**Interfaces:**
- Consumes: новые пропсы `onAnalyze: () => void`, `analyzing: boolean` (передаёт `GoodsForm`, Task 7).

- [ ] **Step 1: Импортировать `Spinner`**

Заменить:

```tsx
import { Button, Form } from 'react-bootstrap';
```

на:

```tsx
import { Button, Form, Spinner } from 'react-bootstrap';
```

- [ ] **Step 2: Расширить пропсы**

Заменить интерфейс `Props`:

```tsx
interface Props {
    photos: PhotoDraft[];
    onChange: (next: PhotoDraft[]) => void;
}
```

на:

```tsx
interface Props {
    photos: PhotoDraft[];
    onChange: (next: PhotoDraft[]) => void;
    onAnalyze: () => void;
    analyzing: boolean;
}
```

И сигнатуру компонента:

```tsx
export const MediaManager: React.FC<Props> = ({ photos, onChange, onAnalyze, analyzing }) => {
```

- [ ] **Step 3: Добавить кнопку анализа** (в ряд кнопок добавления, после input'а камеры)

Заменить блок:

```tsx
                <input ref={cameraRef} type="file" accept="image/*" capture="environment" hidden
                       onChange={e => addFiles(e.target.files)} />
            </div>
```

на:

```tsx
                <input ref={cameraRef} type="file" accept="image/*" capture="environment" hidden
                       onChange={e => addFiles(e.target.files)} />
                <Button
                    variant="success"
                    size="sm"
                    disabled={photos.length === 0 || analyzing || busy}
                    onClick={onAnalyze}
                >
                    {analyzing
                        ? <><Spinner animation="border" size="sm" className="me-1" />Анализ фотографий…</>
                        : '🔍 Анализировать фото'}
                </Button>
            </div>
```

- [ ] **Step 4: Сборка**

Run: `cd /c/Users/spide/WORK/Florics/frontend && npm run build`
Expected: `✓ built` без ошибок TS.

---

### Task 7: Frontend — `handleAnalyze` в `GoodsForm` + проброс

**Files:**
- Modify: `frontend/src/components/admin/GoodsForm.tsx`

**Interfaces:**
- Consumes: `adminService.analyzePhotos` (Task 5), `MediaManager` пропсы `onAnalyze`/`analyzing` (Task 6), `categories: Category[]` (есть в форме).

- [ ] **Step 1: Состояние `analyzing`** (после `const [mediaLoading, setMediaLoading] = useState(false);`)

```tsx
    const [analyzing, setAnalyzing] = useState(false);
```

- [ ] **Step 2: Обработчик `handleAnalyze`** (добавить рядом с `handleSubmit`, перед `return (`)

```tsx
    const handleAnalyze = async () => {
        setAnalyzing(true);
        try {
            const card = await adminService.analyzePhotos(photos);
            if (card.error) {
                toast.error(card.error);
                return;
            }
            setFormData(prev => ({
                ...prev,
                name: card.name ?? prev.name,
                description: card.shortDescription ?? prev.description,
                detailedDescription: card.fullDescription ?? prev.detailedDescription,
                careInstructions: card.care ?? prev.careInstructions,
                categoryId: categories.find(c => c.type === 'PLANT')?.id ?? prev.categoryId
            }));
            toast.success('Поля заполнены по фото');
        } catch {
            toast.error('Не удалось проанализировать фото');
        } finally {
            setAnalyzing(false);
        }
    };
```

- [ ] **Step 3: Пробросить пропсы в `MediaManager`**

Заменить:

```tsx
                            : <MediaManager photos={photos} onChange={setPhotos} />}
```

на:

```tsx
                            : <MediaManager photos={photos} onChange={setPhotos} onAnalyze={handleAnalyze} analyzing={analyzing} />}
```

- [ ] **Step 4: Сборка**

Run: `cd /c/Users/spide/WORK/Florics/frontend && npm run build`
Expected: `✓ built` без ошибок TS.

---

### Task 8: Сборка, рестарт, сквозной QA на :8080

**Files:** — (только верификация)

- [ ] **Step 1: Полная сборка фронта + рестарт бэка**

```bash
cd /c/Users/spide/WORK/Florics/frontend && npm run build
pid=$(netstat -ano | grep LISTENING | grep ':8080 ' | awk '{print $5}' | head -1); [ -n "$pid" ] && MSYS_NO_PATHCONV=1 taskkill /F /PID $pid
cd /c/Users/spide/WORK/Florics/backend && mvn spring-boot:run   # в фоне, дождаться "Started ApplicationKt"
```
Также убедиться, что **AI Agent запущен на :8081** с агентом `photo-analyzer`.

- [ ] **Step 2: Браузерный QA (аккаунт ADMIN `spiderru5597@gmail.com`)**

- Создать товар → вкладка «Фотографии» → загрузить фото растения → кнопка «🔍 Анализировать фото» активна → клик → показывается спиннер «Анализ фотографий…», кнопка заблокирована → по завершении поля **Название/Описание/Детальное/Уход** заполнены, категория стала «Растения» (PLANT), тост «Поля заполнены по фото».
- Проверить смесь фото (новый blob + сохранённое db `/api/media/{id}` + внешний URL) — все доходят (db/внешние резолвятся на сервере). В сетевой панели — один `POST /api/admin/goods/analyze-photos` (multipart) → `200` с JSON PlantCard.
- Не-растение / пустой ответ бота с `error` → тост текста ошибки, поля НЕ изменены.
- Кнопка неактивна, когда фото нет.
- (Если AI Agent на :8081 не запущен) → тост «AI-сервис недоступен, попробуйте позже», поля не тронуты.

- [ ] **Step 3: Регресс-сборки**

```bash
cd /c/Users/spide/WORK/Florics/backend && mvn -q test
cd /c/Users/spide/WORK/Florics/frontend && npm run build
```
Expected: backend тесты зелёные; фронт собирается.

---

## Self-Review

**Spec coverage:**
- Кнопка во вкладке, активна при ≥1 фото, спиннер → Task 6. ✓
- Перезапись 4 полей + категория PLANT → Task 7. ✓
- Маппинг name/short/full/care → Task 7 (setFormData) + Task 2 (parsePlantCard). ✓
- Все фото; blob байтами / db по mediaId / внешние по url → Task 5 (adminService) + Task 3 (резолв) + Task 4 (контроллер). ✓
- Двухшаговый вызов бота + единственное SSE-событие → Task 2. ✓
- photoAnalyzerAgentType → Task 1. ✓
- Парсинг snake_case через Map → Task 2/3. ✓
- Внешний fetch с лимитом/таймаутом, best-effort → Task 3. ✓
- Ошибки (error-карточка / недоступность / нет фото) → Task 3 (бэк) + Task 7 (тосты). ✓
- Переиспользование `aiAgentWebClient` (тот же базовый путь) → Task 2. ✓

**Placeholder scan:** нет TODO/«добавить обработку» без кода — все шаги с конкретным кодом/командами. ✓

**Type consistency:**
- `PlantCardDto(name, shortDescription, fullDescription, care, error)` ↔ фронт `PlantCard` (camelCase) ↔ `card.name/shortDescription/...` в `handleAnalyze`. ✓
- `ImageBytes(bytes, contentType)` — производится в Task 3, потребляется `analyzePhotos` (Task 2). ✓
- `analyze(files, mediaIdsJson, urlsJson)` (Task 3) ↔ вызов из контроллера (Task 4). ✓
- `analyzePhotos(photos: PhotoDraft[])` (Task 5) ↔ `handleAnalyze` (Task 7); поля `PhotoDraft.file/url/mediaId` существуют. ✓
- multipart-поля `files`/`mediaIds`/`urls` совпадают между Task 5 (клиент) и Task 4 (сервер). ✓

## Execution Handoff

После сохранения плана — выбор способа исполнения.
