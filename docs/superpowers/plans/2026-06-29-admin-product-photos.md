# Вкладка «Фотографии» товара — план реализации

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Добавить во вкладочную модалку товара в админке вкладку «Фотографии» с управлением фото (внешняя ссылка / загрузка файла / съёмка с камеры), хранением загруженных фото в БД (`media_content`), переупорядочиванием и выбором превью.

**Architecture:** Загруженные фото сжимаются на клиенте в JPEG (≤1600px, ≤2 МБ) и сохраняются как `media` (type=IMAGE, url=`/api/media/{id}`) + байты в новой таблице `media_content`. Внешние фото — обычные `media.url`. Медиа полностью владеет новый `MediaService`/`MediaRepository`; персист медиа убирается из `GoodsRepository.save` (устранение латентного бага дублирования). Витрина не меняется — всё URL-driven.

**Tech Stack:** Backend — Spring Boot 4.0.3 WebFlux + Kotlin 2.1.20 + Coroutines, R2DBC Postgres (DatabaseClient для bytea), Flyway. Frontend — React 18 + TS + React-Bootstrap, canvas/createImageBitmap для сжатия.

## Global Constraints

- Kotlin 2.1.20, Spring Boot 4.0.3, Java 21; jvmTarget=21 (системный JDK 25).
- **Unit-тесты не пишем** (решение пользователя; в проекте нет DB-тест-инфры/фронт-раннера). Верификация: backend — `cd backend && mvn test` (компиляция + зелёные существующие тесты) + ручной QA; frontend — `cd frontend && npm run build` + браузерный QA.
- Формат хранимых фото: **JPEG**, даунскейл по длинной стороне **≤ 1600px**, размер **≤ 2 МБ**; mime пишем в `media_content.content_type`.
- URL фото из БД: `/api/media/{mediaId}` (короткий, влезает в `media.url VARCHAR(2000)`).
- Эндпоинт отдачи байтов `/api/media/**` — `permitAll` в SecurityConfig.
- Просмотр на http://localhost:8080 (бэк отдаёт собранный фронт): после фронт-правок — `npm run build` → перезапуск `spring-boot:run` (копирует `frontend/dist`). QA-аккаунт ADMIN: `spiderru5597@gmail.com` / `$Spider74`.
- Bash предпочтительнее PowerShell. Не использовать unit-тесты как gate — gate'ы это компиляция/сборка/браузер.

## File Structure

**Backend**
- Create `backend/src/main/resources/db/migration/V6__create_media_content.sql` — таблица `media_content`.
- Create `backend/src/main/kotlin/com/example/webflux/repository/MediaRepository.kt` — владелец media + media_content (DatabaseClient).
- Create `backend/src/main/kotlin/com/example/webflux/service/MediaService.kt` — list/upload/reconcile/loadContent.
- Create `backend/src/main/kotlin/com/example/webflux/controller/MediaContentController.kt` — публичная отдача байтов.
- Create `backend/src/main/kotlin/com/example/webflux/controller/AdminMediaController.kt` — админ CRUD/порядок/загрузка.
- Modify `backend/src/main/kotlin/com/example/webflux/controller/model/Dtos.kt` — `AdminMediaDto`, `MediaReconcileItemDto`, `MediaReconcileRequest`.
- Modify `backend/src/main/kotlin/com/example/webflux/config/SecurityConfig.kt` — `permitAll` `/api/media/**`.
- Modify `backend/src/main/kotlin/com/example/webflux/repository/GoodsRepository.kt` — `save` без персиста media.

**Frontend**
- Create `frontend/src/utils/imageCompress.ts` — `compressImage`.
- Create `frontend/src/components/admin/MediaManager.tsx` — UI вкладки + тип `PhotoDraft`.
- Modify `frontend/src/types/admin.ts` — `AdminMedia`, `MediaReconcileItem`.
- Modify `frontend/src/services/adminService.ts` — `getGoodsMedia`/`uploadGoodsPhoto`/`reconcileGoodsMedia`.
- Modify `frontend/src/components/admin/GoodsForm.tsx` — вкладка «Фотографии», состояние, загрузка при редактировании, оркестрация сохранения.
- Modify `frontend/src/App.css` — стили `.media-grid`/`.media-tile`.

---

### Task 1: Миграция БД `media_content`

**Files:**
- Create: `backend/src/main/resources/db/migration/V6__create_media_content.sql`

**Interfaces:**
- Produces: таблица `media_content(media_id PK→media.id, content bytea, content_type, size_bytes, created_at)`.

- [ ] **Step 1: Создать миграцию**

```sql
-- V6__create_media_content.sql
-- Бинарное содержимое фото, хранимых в БД (привязано к строке media).
CREATE TABLE media_content (
    media_id     VARCHAR(36) PRIMARY KEY REFERENCES media(id) ON DELETE CASCADE,
    content      BYTEA        NOT NULL,
    content_type VARCHAR(100) NOT NULL,   -- mime, напр. image/jpeg
    size_bytes   INT          NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE media_content IS 'Бинарное содержимое фото, хранимых в БД (1:1 к media)';
```

- [ ] **Step 2: Запустить бэкенд и убедиться, что Flyway применил V6**

Run: `cd /c/Users/spide/WORK/Florics/backend && mvn spring-boot:run` (в фоне), дождаться `Started ApplicationKt`, проверить в логах строку `Migrating schema ... to version "6 - create media content"` (или отсутствие ошибок Flyway). Остановить процесс.
Expected: приложение стартует без ошибок миграции; таблица `media_content` создана.

- [ ] **Step 3: Commit**

```bash
cd /c/Users/spide/WORK/Florics
git add backend/src/main/resources/db/migration/V6__create_media_content.sql
git commit -m "feat(media): миграция media_content для хранения фото в БД"
```

---

### Task 2: `MediaRepository` (media + media_content через DatabaseClient)

**Files:**
- Create: `backend/src/main/kotlin/com/example/webflux/repository/MediaRepository.kt`

**Interfaces:**
- Consumes: `MediaR2dbcRepository` (findByGoodsId, softDelete, save), `DatabaseClient`.
- Produces:
  - `suspend fun findByGoodsId(goodsId: String): List<MediaEntity>`
  - `suspend fun nextOrder(goodsId: String): Int`
  - `suspend fun insertDbPhoto(goodsId: String, bytes: ByteArray, contentType: String, order: Int): MediaEntity`
  - `suspend fun insertExternal(goodsId: String, url: String, order: Int): MediaEntity`
  - `suspend fun updateOrder(mediaId: String, order: Int)`
  - `suspend fun softDelete(mediaId: String)`
  - `suspend fun loadContent(mediaId: String): MediaContent?`
  - `data class MediaContent(val content: ByteArray, val contentType: String)`

- [ ] **Step 1: Создать `MediaRepository.kt`**

```kotlin
package com.example.webflux.repository

import com.example.webflux.entity.MediaEntity
import com.example.webflux.repository.r2dbc.MediaR2dbcRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import java.util.UUID

/** Содержимое фото из БД для отдачи байтов. */
data class MediaContent(val content: ByteArray, val contentType: String)

/**
 * Владелец медиа товара: строки таблицы media + бинарное содержимое media_content.
 * Запись media_content и вставка media с заранее известным id делаются через DatabaseClient,
 * чтобы обойти семантику CrudRepository.save для не-генерируемых PK и маппинг bytea.
 */
@Repository
class MediaRepository(
    private val mediaR2dbcRepository: MediaR2dbcRepository,
    private val databaseClient: DatabaseClient
) {
    suspend fun findByGoodsId(goodsId: String): List<MediaEntity> =
        mediaR2dbcRepository.findByGoodsId(goodsId).toList()

    suspend fun nextOrder(goodsId: String): Int {
        val max = databaseClient.sql(
            "SELECT COALESCE(MAX(display_order), -1) FROM media WHERE goods_id = :gid AND deleted_at IS NULL"
        ).bind("gid", goodsId)
            .map { row, _ -> (row.get(0, java.lang.Integer::class.java) ?: -1).toInt() }
            .one().awaitSingle()
        return max + 1
    }

    suspend fun insertDbPhoto(goodsId: String, bytes: ByteArray, contentType: String, order: Int): MediaEntity {
        val id = UUID.randomUUID().toString()
        val url = "/api/media/$id"
        databaseClient.sql(
            "INSERT INTO media (id, goods_id, type, url, display_order) VALUES (:id, :gid, 'IMAGE', :url, :ord)"
        ).bind("id", id).bind("gid", goodsId).bind("url", url).bind("ord", order)
            .fetch().rowsUpdated().awaitSingle()
        databaseClient.sql(
            "INSERT INTO media_content (media_id, content, content_type, size_bytes) " +
                "VALUES (:mid, :content, :ct, :sz)"
        ).bind("mid", id).bind("content", bytes).bind("ct", contentType).bind("sz", bytes.size)
            .fetch().rowsUpdated().awaitSingle()
        return MediaEntity(id = id, goodsId = goodsId, type = "IMAGE", url = url, displayOrder = order)
    }

    suspend fun insertExternal(goodsId: String, url: String, order: Int): MediaEntity =
        mediaR2dbcRepository.save(
            MediaEntity(id = null, goodsId = goodsId, type = "IMAGE", url = url, displayOrder = order)
        )

    suspend fun updateOrder(mediaId: String, order: Int) {
        databaseClient.sql("UPDATE media SET display_order = :ord WHERE id = :id AND deleted_at IS NULL")
            .bind("ord", order).bind("id", mediaId)
            .fetch().rowsUpdated().awaitSingle()
    }

    suspend fun softDelete(mediaId: String) = mediaR2dbcRepository.softDelete(mediaId)

    suspend fun loadContent(mediaId: String): MediaContent? =
        databaseClient.sql("SELECT content, content_type FROM media_content WHERE media_id = :id")
            .bind("id", mediaId)
            .map { row, _ ->
                MediaContent(
                    content = row.get("content", ByteArray::class.java)!!,
                    contentType = row.get("content_type", String::class.java)!!
                )
            }
            .one().awaitFirstOrNull()
}
```

- [ ] **Step 2: Компиляция**

Run: `cd /c/Users/spide/WORK/Florics/backend && mvn -q compile`
Expected: BUILD SUCCESS (нет ошибок компиляции).

> Если r2dbc-postgresql вернёт bytea как `ByteBuffer`, заменить чтение на `row.get("content", java.nio.ByteBuffer::class.java)!!.let { val a = ByteArray(it.remaining()); it.get(a); a }`. Проверится в Task 11 при реальной отдаче.

- [ ] **Step 3: Commit**

```bash
cd /c/Users/spide/WORK/Florics
git add backend/src/main/kotlin/com/example/webflux/repository/MediaRepository.kt
git commit -m "feat(media): MediaRepository — media + media_content (DatabaseClient)"
```

---

### Task 3: DTO + `MediaService`

**Files:**
- Modify: `backend/src/main/kotlin/com/example/webflux/controller/model/Dtos.kt` (добавить в конец файла)
- Create: `backend/src/main/kotlin/com/example/webflux/service/MediaService.kt`

**Interfaces:**
- Consumes: `MediaRepository` (Task 2).
- Produces:
  - DTO: `AdminMediaDto(id, type, url, order)`, `MediaReconcileItemDto(id?, url?, order)`, `MediaReconcileRequest(items)`.
  - `suspend fun listByGoods(goodsId): List<AdminMediaDto>`
  - `suspend fun uploadPhoto(goodsId, file: FilePart): AdminMediaDto`
  - `suspend fun reconcile(goodsId, items: List<MediaReconcileItemDto>): List<AdminMediaDto>`
  - `suspend fun loadContent(mediaId): MediaContent?`

- [ ] **Step 1: Добавить DTO в конец `Dtos.kt`**

```kotlin

// =====================================================
// ADMIN MEDIA DTO (вкладка «Фотографии» товара)
// =====================================================
data class AdminMediaDto(
    val id: String,
    val type: String,   // IMAGE | VIDEO
    val url: String,
    val order: Int
)

data class MediaReconcileItemDto(
    val id: String? = null,   // есть => существующее медиа (оставить, обновить порядок)
    val url: String? = null,  // есть без id => новое внешнее
    val order: Int
)

data class MediaReconcileRequest(
    val items: List<MediaReconcileItemDto> = emptyList()
)
```

- [ ] **Step 2: Создать `MediaService.kt`**

```kotlin
package com.example.webflux.service

import com.example.webflux.controller.model.AdminMediaDto
import com.example.webflux.controller.model.MediaReconcileItemDto
import com.example.webflux.entity.MediaEntity
import com.example.webflux.repository.MediaContent
import com.example.webflux.repository.MediaRepository
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service

@Service
class MediaService(
    private val mediaRepository: MediaRepository
) {
    suspend fun listByGoods(goodsId: String): List<AdminMediaDto> =
        mediaRepository.findByGoodsId(goodsId).map { it.toAdminDto() }

    suspend fun uploadPhoto(goodsId: String, file: FilePart): AdminMediaDto {
        val bytes = file.readBytes()
        val contentType = file.headers().contentType?.toString() ?: "image/jpeg"
        val order = mediaRepository.nextOrder(goodsId)
        return mediaRepository.insertDbPhoto(goodsId, bytes, contentType, order).toAdminDto()
    }

    /**
     * Привести набор медиа товара к присланному упорядоченному списку:
     * удалить отсутствующие, вставить новые внешние, обновить порядок существующих.
     */
    suspend fun reconcile(goodsId: String, items: List<MediaReconcileItemDto>): List<AdminMediaDto> {
        val existing = mediaRepository.findByGoodsId(goodsId)
        val existingIds = existing.mapNotNull { it.id }.toSet()
        val keptIds = items.mapNotNull { it.id }.toSet()

        (existingIds - keptIds).forEach { mediaRepository.softDelete(it) }

        items.forEach { item ->
            when {
                item.id != null && item.id in existingIds ->
                    mediaRepository.updateOrder(item.id, item.order)
                item.id == null && !item.url.isNullOrBlank() ->
                    mediaRepository.insertExternal(goodsId, item.url.trim(), item.order)
                // item.id, которого нет среди существующих — игнорируем (защита от рассинхрона)
            }
        }
        return listByGoods(goodsId)
    }

    suspend fun loadContent(mediaId: String): MediaContent? = mediaRepository.loadContent(mediaId)

    private fun MediaEntity.toAdminDto() =
        AdminMediaDto(id = id!!, type = type, url = url, order = displayOrder)

    private suspend fun FilePart.readBytes(): ByteArray {
        val buffer = DataBufferUtils.join(content()).awaitSingle()
        val bytes = ByteArray(buffer.readableByteCount())
        buffer.read(bytes)
        DataBufferUtils.release(buffer)
        return bytes
    }
}
```

- [ ] **Step 3: Компиляция**

Run: `cd /c/Users/spide/WORK/Florics/backend && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
cd /c/Users/spide/WORK/Florics
git add backend/src/main/kotlin/com/example/webflux/controller/model/Dtos.kt backend/src/main/kotlin/com/example/webflux/service/MediaService.kt
git commit -m "feat(media): MediaService + DTO (list/upload/reconcile/loadContent)"
```

---

### Task 4: Публичная отдача байтов + SecurityConfig

**Files:**
- Create: `backend/src/main/kotlin/com/example/webflux/controller/MediaContentController.kt`
- Modify: `backend/src/main/kotlin/com/example/webflux/config/SecurityConfig.kt:95`

**Interfaces:**
- Consumes: `MediaService.loadContent`.
- Produces: `GET /api/media/{id}` → байты с `Content-Type` и `Cache-Control: immutable`.

- [ ] **Step 1: Создать `MediaContentController.kt`**

```kotlin
package com.example.webflux.controller

import com.example.webflux.service.MediaService
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class MediaContentController(
    private val mediaService: MediaService
) {
    /** Публичная отдача бинарного содержимого фото из БД. Байты по id неизменны → immutable-кэш. */
    @GetMapping("/api/media/{id}")
    suspend fun get(@PathVariable id: String): ResponseEntity<ByteArray> {
        val c = mediaService.loadContent(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, c.contentType)
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
            .body(c.content)
    }
}
```

- [ ] **Step 2: Разрешить `/api/media/**` в SecurityConfig**

В `SecurityConfig.kt` после строки `.pathMatchers("/uploads/**").permitAll()` добавить:

```kotlin
                    // Бинарные фото из БД (публичная отдача по id)
                    .pathMatchers("/api/media/**").permitAll()
```

- [ ] **Step 3: Компиляция**

Run: `cd /c/Users/spide/WORK/Florics/backend && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
cd /c/Users/spide/WORK/Florics
git add backend/src/main/kotlin/com/example/webflux/controller/MediaContentController.kt backend/src/main/kotlin/com/example/webflux/config/SecurityConfig.kt
git commit -m "feat(media): публичная отдача GET /api/media/{id} + permitAll"
```

---

### Task 5: `AdminMediaController`

**Files:**
- Create: `backend/src/main/kotlin/com/example/webflux/controller/AdminMediaController.kt`

**Interfaces:**
- Consumes: `MediaService` (list/uploadPhoto/reconcile), `MediaReconcileRequest`.
- Produces: `/api/admin/goods/{goodsId}/media` — GET / POST /upload / PUT.

- [ ] **Step 1: Создать `AdminMediaController.kt`**

```kotlin
package com.example.webflux.controller

import com.example.webflux.controller.model.AdminMediaDto
import com.example.webflux.controller.model.MediaReconcileRequest
import com.example.webflux.service.MediaService
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/goods/{goodsId}/media")
@PreAuthorize("hasRole('ADMIN')")
class AdminMediaController(
    private val mediaService: MediaService
) {
    /** Список медиа товара (с id — для удаления/порядка на клиенте). */
    @GetMapping
    suspend fun list(@PathVariable goodsId: String): List<AdminMediaDto> =
        mediaService.listByGoods(goodsId)

    /** Загрузка фото-файла (сжатый JPEG) → media + media_content. */
    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun upload(
        @PathVariable goodsId: String,
        @RequestPart("file") file: FilePart
    ): AdminMediaDto = mediaService.uploadPhoto(goodsId, file)

    /** Привести набор медиа к присланному упорядоченному списку. */
    @PutMapping
    suspend fun reconcile(
        @PathVariable goodsId: String,
        @RequestBody request: MediaReconcileRequest
    ): List<AdminMediaDto> = mediaService.reconcile(goodsId, request.items)
}
```

- [ ] **Step 2: Компиляция**

Run: `cd /c/Users/spide/WORK/Florics/backend && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
cd /c/Users/spide/WORK/Florics
git add backend/src/main/kotlin/com/example/webflux/controller/AdminMediaController.kt
git commit -m "feat(media): AdminMediaController — list/upload/reconcile"
```

---

### Task 6: Развязать media от `GoodsRepository.save` (фикс дубликатов)

**Files:**
- Modify: `backend/src/main/kotlin/com/example/webflux/repository/GoodsRepository.kt:91-101`

**Interfaces:**
- Produces: `save` больше не вставляет media; возвращает товар с актуальными media из БД.

- [ ] **Step 1: Заменить тело `save`**

Заменить блок (строки ~91-101):

```kotlin
    suspend fun save(goods: Goods): Goods {
        val saved = goodsR2dbcRepository.save(GoodsMapper.toEntity(goods))

        val media = mediaR2dbcRepository.saveAll(goods.media
                .map { MediaMapper.toEntity(it, saved.id!!) }
            )
            .map { MediaMapper.toModel(it) }
            .toList()

        return GoodsMapper.toModel(saved, media)
    }
```

на:

```kotlin
    suspend fun save(goods: Goods): Goods {
        // Медиа товара управляется отдельно через MediaService/AdminMediaController.
        // Здесь сохраняем только скалярные поля товара и возвращаем актуальные media из БД,
        // чтобы апдейт не пере-вставлял (не дублировал) строки media.
        val saved = goodsR2dbcRepository.save(GoodsMapper.toEntity(goods))

        val media = mediaR2dbcRepository.findByGoodsId(saved.id!!)
            .map { MediaMapper.toModel(it) }
            .toList()

        return GoodsMapper.toModel(saved, media)
    }
```

- [ ] **Step 2: Компиляция + существующие тесты**

Run: `cd /c/Users/spide/WORK/Florics/backend && mvn -q test`
Expected: BUILD SUCCESS, существующие тесты зелёные.

- [ ] **Step 3: Commit**

```bash
cd /c/Users/spide/WORK/Florics
git add backend/src/main/kotlin/com/example/webflux/repository/GoodsRepository.kt
git commit -m "fix(media): GoodsRepository.save не персистит media (устранение дубликатов)"
```

---

### Task 7: Фронтенд — утилита сжатия `compressImage`

**Files:**
- Create: `frontend/src/utils/imageCompress.ts`

**Interfaces:**
- Produces: `compressImage(file: File, opts?): Promise<Blob>` (JPEG, ≤maxDim, ≤maxBytes).

- [ ] **Step 1: Создать `imageCompress.ts`**

```ts
export interface CompressOptions {
    maxDim?: number;          // длинная сторона, по умолчанию 1600
    maxBytes?: number;        // по умолчанию 2 МБ
    qualitySteps?: number[];  // по умолчанию [0.8, 0.7, 0.6, 0.5]
}

/**
 * Уменьшает изображение по длинной стороне до maxDim (сохраняя пропорции)
 * и кодирует в JPEG, понижая quality пока не уложится в maxBytes.
 * Возвращает JPEG Blob. Источник может быть JPEG/PNG/WebP (декодит браузер).
 */
export async function compressImage(file: File, opts: CompressOptions = {}): Promise<Blob> {
    const maxDim = opts.maxDim ?? 1600;
    const maxBytes = opts.maxBytes ?? 2 * 1024 * 1024;
    const qualitySteps = opts.qualitySteps ?? [0.8, 0.7, 0.6, 0.5];

    const bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' });
    const scale = Math.min(1, maxDim / Math.max(bitmap.width, bitmap.height));
    const w = Math.round(bitmap.width * scale);
    const h = Math.round(bitmap.height * scale);

    const canvas = document.createElement('canvas');
    canvas.width = w;
    canvas.height = h;
    const ctx = canvas.getContext('2d');
    if (!ctx) {
        bitmap.close();
        throw new Error('Canvas 2D context недоступен');
    }
    ctx.drawImage(bitmap, 0, 0, w, h);
    bitmap.close();

    const toBlob = (q: number) =>
        new Promise<Blob | null>(resolve => canvas.toBlob(resolve, 'image/jpeg', q));

    let blob: Blob | null = null;
    for (const q of qualitySteps) {
        blob = await toBlob(q);
        if (blob && blob.size <= maxBytes) return blob;
    }
    if (!blob) throw new Error('Не удалось закодировать изображение');
    return blob; // последняя (наименьшая) попытка, даже если чуть превышает лимит
}
```

- [ ] **Step 2: Сборка фронтенда (typecheck)**

Run: `cd /c/Users/spide/WORK/Florics/frontend && npm run build`
Expected: `✓ built` без ошибок TS.

- [ ] **Step 3: Commit**

```bash
cd /c/Users/spide/WORK/Florics
git add frontend/src/utils/imageCompress.ts
git commit -m "feat(media): утилита compressImage (даунскейл + JPEG ≤2МБ)"
```

---

### Task 8: Фронтенд — типы + методы `adminService`

**Files:**
- Modify: `frontend/src/types/admin.ts` (добавить в конец)
- Modify: `frontend/src/services/adminService.ts`

**Interfaces:**
- Produces: `AdminMedia`, `MediaReconcileItem`; `adminService.getGoodsMedia/uploadGoodsPhoto/reconcileGoodsMedia`.

- [ ] **Step 1: Добавить типы в конец `types/admin.ts`**

```ts

export interface AdminMedia {
    id: string;
    type: string;   // IMAGE | VIDEO
    url: string;
    order: number;
}

export interface MediaReconcileItem {
    id?: string;   // есть => существующее медиа (оставить, обновить порядок)
    url?: string;  // есть без id => новое внешнее
    order: number;
}
```

- [ ] **Step 2: Добавить методы в `adminService.ts`**

Импорт сверху расширить:

```ts
import { CreateGoodsRequest, UpdateGoodsRequest, PagedResponse, PaginationParams, AdminMedia, MediaReconcileItem } from '../types/admin';
```

Перед закрывающей `};` объекта `adminService` (после `uploadFile`) добавить запятую к `uploadFile` и вставить:

```ts
    // Media товара (вкладка «Фотографии»)
    getGoodsMedia: async (goodsId: string): Promise<AdminMedia[]> => {
        const response = await axiosInstance.get(`/admin/goods/${goodsId}/media`);
        return response.data;
    },

    uploadGoodsPhoto: async (goodsId: string, blob: Blob): Promise<AdminMedia> => {
        const formData = new FormData();
        formData.append('file', blob, 'photo.jpg');
        const response = await axiosInstance.post(`/admin/goods/${goodsId}/media/upload`, formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        });
        return response.data;
    },

    reconcileGoodsMedia: async (goodsId: string, items: MediaReconcileItem[]): Promise<AdminMedia[]> => {
        const response = await axiosInstance.put(`/admin/goods/${goodsId}/media`, { items });
        return response.data;
    }
```

> Убедиться, что у `uploadFile` теперь стоит хвостовая запятая `},` перед новыми методами.

- [ ] **Step 3: Сборка**

Run: `cd /c/Users/spide/WORK/Florics/frontend && npm run build`
Expected: `✓ built` без ошибок TS.

- [ ] **Step 4: Commit**

```bash
cd /c/Users/spide/WORK/Florics
git add frontend/src/types/admin.ts frontend/src/services/adminService.ts
git commit -m "feat(media): типы AdminMedia + методы adminService для фото"
```

---

### Task 9: Фронтенд — компонент `MediaManager` + стили

**Files:**
- Create: `frontend/src/components/admin/MediaManager.tsx`
- Modify: `frontend/src/App.css` (добавить в секцию админ-адаптива)

**Interfaces:**
- Consumes: `compressImage` (Task 7).
- Produces: `export interface PhotoDraft {...}`, `export const MediaManager: React.FC<{ photos: PhotoDraft[]; onChange: (next: PhotoDraft[]) => void }>`.

- [ ] **Step 1: Создать `MediaManager.tsx`**

```tsx
import React, { useEffect, useRef, useState } from 'react';
import { Button, Form } from 'react-bootstrap';
import { toast } from 'react-toastify';
import { compressImage } from '../../utils/imageCompress';

export interface PhotoDraft {
    key: string;          // стабильный локальный ключ
    mediaId?: string;     // есть => уже сохранено (внешнее или из БД)
    url?: string;         // внешний URL (новое внешнее) или url существующего (для показа)
    file?: Blob;          // есть => новый файл/снимок (сжатый JPEG) для POST
    previewUrl: string;   // что показывает <img src>
}

interface Props {
    photos: PhotoDraft[];
    onChange: (next: PhotoDraft[]) => void;
}

export const MediaManager: React.FC<Props> = ({ photos, onChange }) => {
    const [urlInput, setUrlInput] = useState('');
    const [busy, setBusy] = useState(false);
    const fileRef = useRef<HTMLInputElement>(null);
    const cameraRef = useRef<HTMLInputElement>(null);

    // Освобождать objectURL новых файлов при размонтировании (закрытие модалки)
    const photosRef = useRef(photos);
    photosRef.current = photos;
    useEffect(() => () => {
        photosRef.current.forEach(p => { if (p.file) URL.revokeObjectURL(p.previewUrl); });
    }, []);

    const addFiles = async (files: FileList | null) => {
        if (!files || files.length === 0) return;
        setBusy(true);
        try {
            const added: PhotoDraft[] = [];
            for (const file of Array.from(files)) {
                if (!file.type.startsWith('image/')) {
                    toast.error(`«${file.name}» — не изображение`);
                    continue;
                }
                try {
                    const blob = await compressImage(file);
                    added.push({ key: crypto.randomUUID(), file: blob, previewUrl: URL.createObjectURL(blob) });
                } catch {
                    toast.error(`Не удалось обработать «${file.name}»`);
                }
            }
            if (added.length) onChange([...photos, ...added]);
        } finally {
            setBusy(false);
            if (fileRef.current) fileRef.current.value = '';
            if (cameraRef.current) cameraRef.current.value = '';
        }
    };

    const addUrl = () => {
        const u = urlInput.trim();
        if (!u) return;
        if (!/^https?:\/\//i.test(u)) {
            toast.error('Ссылка должна начинаться с http:// или https://');
            return;
        }
        onChange([...photos, { key: crypto.randomUUID(), url: u, previewUrl: u }]);
        setUrlInput('');
    };

    const remove = (idx: number) => {
        const p = photos[idx];
        if (p.file) URL.revokeObjectURL(p.previewUrl);
        onChange(photos.filter((_, i) => i !== idx));
    };

    const move = (idx: number, dir: -1 | 1) => {
        const j = idx + dir;
        if (j < 0 || j >= photos.length) return;
        const next = [...photos];
        [next[idx], next[j]] = [next[j], next[idx]];
        onChange(next);
    };

    const makePreview = (idx: number) => {
        if (idx === 0) return;
        const next = [...photos];
        const [item] = next.splice(idx, 1);
        next.unshift(item);
        onChange(next);
    };

    return (
        <div>
            <div className="d-flex flex-wrap gap-2 mb-3">
                <Button variant="outline-success" size="sm" disabled={busy} onClick={() => fileRef.current?.click()}>
                    📁 Загрузить файлы
                </Button>
                <Button variant="outline-success" size="sm" disabled={busy} onClick={() => cameraRef.current?.click()}>
                    📷 Сделать фото
                </Button>
                <input ref={fileRef} type="file" accept="image/*" multiple hidden
                       onChange={e => addFiles(e.target.files)} />
                <input ref={cameraRef} type="file" accept="image/*" capture="environment" hidden
                       onChange={e => addFiles(e.target.files)} />
            </div>

            <div className="d-flex gap-2 mb-3">
                <Form.Control
                    type="url"
                    placeholder="Или вставьте ссылку на изображение (https://…)"
                    value={urlInput}
                    onChange={e => setUrlInput(e.target.value)}
                    onKeyDown={e => { if (e.key === 'Enter') { e.preventDefault(); addUrl(); } }}
                />
                <Button variant="outline-secondary" onClick={addUrl}>Добавить</Button>
            </div>

            {busy && <div className="text-muted small mb-2">Обработка изображений…</div>}

            {photos.length === 0 ? (
                <div className="text-muted text-center py-4">
                    Фотографий пока нет. Добавьте файл, снимок или ссылку.
                </div>
            ) : (
                <div className="media-grid">
                    {photos.map((p, idx) => (
                        <div key={p.key} className="media-tile">
                            <img
                                src={p.previewUrl}
                                alt={`Фото ${idx + 1}`}
                                className="media-tile-img"
                                onError={e => { (e.currentTarget as HTMLImageElement).style.opacity = '0.3'; }}
                            />
                            {idx === 0 && <span className="media-badge">Превью</span>}
                            <div className="media-tile-actions">
                                <button type="button" title="Левее" disabled={idx === 0}
                                        onClick={() => move(idx, -1)}>←</button>
                                <button type="button" title="Правее" disabled={idx === photos.length - 1}
                                        onClick={() => move(idx, 1)}>→</button>
                                {idx !== 0 && (
                                    <button type="button" title="Сделать превью"
                                            onClick={() => makePreview(idx)}>★</button>
                                )}
                                <button type="button" title="Удалить" className="media-del"
                                        onClick={() => remove(idx)}>✕</button>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default MediaManager;
```

- [ ] **Step 2: Добавить стили в `App.css`** (в конец секции «Админ-панель: адаптив», перед `/* Hero Section */` или в любом месте админ-блока)

```css
/* Сетка фотографий товара (вкладка «Фотографии») */
.media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: var(--space-3);
}
.media-tile {
  position: relative;
  border: 1px solid var(--light-green);
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
}
.media-tile-img {
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
  display: block;
}
.media-badge {
  position: absolute;
  top: 6px;
  left: 6px;
  background: var(--forest-green);
  color: #fff;
  font-size: 0.7rem;
  padding: 2px 6px;
  border-radius: 6px;
}
.media-tile-actions {
  display: flex;
  justify-content: center;
  gap: 4px;
  padding: 4px;
  background: var(--cream);
}
.media-tile-actions button {
  border: 1px solid var(--leaf-green);
  background: #fff;
  border-radius: 6px;
  width: 28px;
  height: 28px;
  line-height: 1;
  cursor: pointer;
}
.media-tile-actions button:disabled { opacity: 0.4; cursor: default; }
.media-tile-actions .media-del { border-color: #dc3545; color: #dc3545; }
```

- [ ] **Step 3: Сборка**

Run: `cd /c/Users/spide/WORK/Florics/frontend && npm run build`
Expected: `✓ built` без ошибок TS.

- [ ] **Step 4: Commit**

```bash
cd /c/Users/spide/WORK/Florics
git add frontend/src/components/admin/MediaManager.tsx frontend/src/App.css
git commit -m "feat(media): компонент MediaManager (загрузка/камера/ссылка/порядок) + стили"
```

---

### Task 10: Фронтенд — вкладка «Фотографии» в `GoodsForm`

**Files:**
- Modify: `frontend/src/components/admin/GoodsForm.tsx`

**Interfaces:**
- Consumes: `MediaManager`, `PhotoDraft` (Task 9); `adminService.getGoodsMedia/uploadGoodsPhoto/reconcileGoodsMedia` (Task 8).

- [ ] **Step 1: Импорты и тип вкладки**

В начале файла добавить импорты:

```tsx
import { MediaManager, PhotoDraft } from './MediaManager';
import type { MediaReconcileItem } from '../../types/admin';
```

Расширить тип вкладок:

```tsx
type FormTab = 'main' | 'details' | 'care' | 'photos';
```

- [ ] **Step 2: Состояние фото + загрузка при редактировании**

После строки `const [activeTab, setActiveTab] = useState<FormTab>('main');` добавить:

```tsx
    const [photos, setPhotos] = useState<PhotoDraft[]>([]);
    const [mediaLoading, setMediaLoading] = useState(false);
```

После существующего `useEffect(() => { loadCategories(); }, []);` добавить загрузку медиа в режиме редактирования:

```tsx
    useEffect(() => {
        if (!goods?.id) return;
        let cancelled = false;
        setMediaLoading(true);
        adminService.getGoodsMedia(goods.id)
            .then(list => {
                if (cancelled) return;
                const drafts: PhotoDraft[] = list
                    .slice()
                    .sort((a, b) => a.order - b.order)
                    .map(m => ({ key: m.id, mediaId: m.id, url: m.url, previewUrl: m.url }));
                setPhotos(drafts);
            })
            .catch(() => { if (!cancelled) toast.error('Не удалось загрузить фотографии товара'); })
            .finally(() => { if (!cancelled) setMediaLoading(false); });
        return () => { cancelled = true; };
    }, [goods?.id]);
```

- [ ] **Step 3: Переписать `handleSubmit` на оркестрацию (товар → загрузка фото → reconcile)**

Заменить весь существующий `handleSubmit` на:

```tsx
    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);

        try {
            // 1) Сохранить скалярные поля товара, получить id
            const saved = goods
                ? await adminService.updateGoods(goods.id, { ...formData, id: goods.id })
                : await adminService.createGoods(formData);
            const goodsId = saved.id!;

            // 2) Догрузить новые файлы, собрать финальный упорядоченный список
            const items: MediaReconcileItem[] = [];
            for (const p of photos) {
                if (p.file) {
                    const created = await adminService.uploadGoodsPhoto(goodsId, p.file);
                    items.push({ id: created.id, order: items.length });
                } else if (p.mediaId) {
                    items.push({ id: p.mediaId, order: items.length });
                } else if (p.url) {
                    items.push({ url: p.url, order: items.length });
                }
            }

            // 3) Привести набор медиа к финальному списку (удаления/вставки/порядок)
            await adminService.reconcileGoodsMedia(goodsId, items);

            toast.success(goods ? 'Товар обновлён' : 'Товар создан');
            onSuccess();
        } catch (error: any) {
            const errorMessage = error.response?.data?.message || 'Ошибка сохранения товара';
            toast.error(errorMessage);
        } finally {
            setLoading(false);
        }
    };
```

- [ ] **Step 4: Добавить вкладку и панель**

В `<Nav variant="tabs" className="admin-tabs mb-3">` после пункта «Уход» добавить:

```tsx
                    <Nav.Item>
                        <Nav.Link eventKey="photos">Фотографии</Nav.Link>
                    </Nav.Item>
```

В `<Tab.Content>` после `<Tab.Pane eventKey="care">…</Tab.Pane>` добавить:

```tsx
                    <Tab.Pane eventKey="photos">
                        {mediaLoading
                            ? <div className="text-muted py-4 text-center">Загрузка фотографий…</div>
                            : <MediaManager photos={photos} onChange={setPhotos} />}
                    </Tab.Pane>
```

- [ ] **Step 5: Сборка**

Run: `cd /c/Users/spide/WORK/Florics/frontend && npm run build`
Expected: `✓ built` без ошибок TS.

- [ ] **Step 6: Commit**

```bash
cd /c/Users/spide/WORK/Florics
git add frontend/src/components/admin/GoodsForm.tsx
git commit -m "feat(media): вкладка «Фотографии» в форме товара + оркестрация сохранения"
```

---

### Task 11: Сборка, бандл, рестарт и сквозной QA на :8080

**Files:** — (только верификация и возможные мелкие фиксы)

- [ ] **Step 1: Полная сборка фронта + рестарт бэка**

```bash
cd /c/Users/spide/WORK/Florics/frontend && npm run build
# остановить старый бэкенд на 8080, если запущен:
pid=$(netstat -ano | grep LISTENING | grep ':8080 ' | awk '{print $5}' | head -1); [ -n "$pid" ] && MSYS_NO_PATHCONV=1 taskkill /F /PID $pid
cd /c/Users/spide/WORK/Florics/backend && mvn spring-boot:run   # в фоне, дождаться "Started ApplicationKt"
```

- [ ] **Step 2: Браузерный QA (вьюпорт мобильный 390×844 и десктоп), аккаунт `spiderru5597@gmail.com`**

Прогнать по чек-листу спецификации:
- Создать товар: вкладка «Фотографии» → добавить фото по ссылке + загрузкой файла + «Сделать фото» (на мобильном вьюпорте откроется выбор камеры); сменить порядок (←/→, ★ «сделать превью»); удалить одно; «Сохранить».
- Открыть товар в каталоге → превью = первое фото, карусель показывает все; внешние и БД-фото грузятся.
- Проверить `GET /api/media/{id}` напрямую (вкладка сети) — 200, корректный `Content-Type: image/jpeg`, грузится без Authorization.
- Редактировать тот же товар → список подтянулся с порядком; удалить/переставить/добавить ещё → «Сохранить» → изменения видны на витрине; дублей в каталоге нет.
- Обновить только текстовые поля товара (без изменения фото) → фото не пропали и не задублировались.

Expected: все пункты проходят. При расхождении bytea (битые байты на отдаче) — применить фолбэк ByteBuffer из Task 2 Step 2, пересобрать бэк, повторить.

- [ ] **Step 3: Регресс-сборки**

```bash
cd /c/Users/spide/WORK/Florics/backend && mvn -q test
cd /c/Users/spide/WORK/Florics/frontend && npm run build
```
Expected: backend тесты зелёные; фронт собирается.

- [ ] **Step 4: Commit (если были фиксы по итогам QA)**

```bash
cd /c/Users/spide/WORK/Florics
git add -A
git commit -m "fix(media): правки по итогам сквозного QA"
```

---

## Self-Review

**Spec coverage:**
- `media_content` (bytea + content_type + size) → Task 1. ✓
- Отдача байтов `/api/media/{id}` + permitAll → Task 4. ✓
- Админ list/upload/reconcile → Task 3 (service) + Task 5 (controller). ✓
- `media.url=/api/media/{id}` для БД-фото → Task 2 (insertDbPhoto). ✓
- Внешние фото как url → Task 2 (insertExternal) + reconcile (Task 3). ✓
- Фикс дубликатов media в save → Task 6. ✓
- Сжатие клиент JPEG ≤1600px/≤2МБ → Task 7. ✓
- Камера (`capture`) + загрузка файлов + по ссылке → Task 9. ✓
- Порядок + первое=превью → Task 9 (move/makePreview) + reconcile order (Task 3/10). ✓
- Вкладка в форме + загрузка при edit + оркестрация сохранения → Task 10. ✓
- Витрина без изменений (URL-driven) → подтверждается в Task 11 QA. ✓
- content_type хранится и отдаётся → Task 2/4. ✓

**Placeholder scan:** нет TODO/«добавить обработку ошибок» без кода — все шаги с конкретным кодом/командами. ✓

**Type consistency:**
- `AdminMediaDto(id,type,url,order)` ↔ фронт `AdminMedia` ↔ `PhotoDraft` маппинг (Task 3/8/10). ✓
- `MediaReconcileItemDto(id?,url?,order)` ↔ `MediaReconcileItem` ↔ items в handleSubmit. ✓
- `MediaRepository` сигнатуры (Task 2) совпадают с вызовами в `MediaService` (Task 3). ✓
- `MediaContent(content,contentType)` возвращается из repo и читается в controller. ✓

## Execution Handoff

После сохранения плана — выбор способа исполнения (см. ниже).
