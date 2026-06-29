# Спецификация: вкладка «Фотографии» в админ-модалке товара

**Дата:** 2026-06-29
**Статус:** утверждён дизайн, ожидается план реализации

## Цель

Добавить во вкладочную модалку создания/редактирования товара (`GoodsForm`) новую вкладку **«Фотографии»**. Админ может управлять фотографиями товара прямо здесь:

- добавлять фото **по внешней ссылке** (URL);
- **загружать файлы** из медиа-библиотеки устройства;
- **снимать фото на камеру** (нативная камера на телефоне/планшете);
- менять **порядок** фото (первое = превью товара в каталоге);
- удалять фото.

Загружаемые файлы хранятся **в БД** в новой таблице `media_content` (bytea), привязанной к строке существующей таблицы `media`. Внешние фото остаются как URL в `media.url`.

## Зафиксированные решения

| Развилка | Решение |
|---|---|
| Камера | Нативная через `<input type="file" accept="image/*" capture="environment">` (+ обычная загрузка файлов) |
| Сжатие фото | На клиенте: даунскейл по длинной стороне ≤ 1600px, экспорт **JPEG** q≈0.8, при > 2 МБ понижать quality до укладывания в лимит |
| Формат хранения | JPEG; в `media_content` дополнительно хранить `content_type` (mime) |
| Порядок/превью | Можно менять порядок (стрелки ↑/↓ + «сделать превью»); первое фото = превью |
| Момент сохранения | **Коммит по кнопке «Сохранить»** (вариант C, см. ниже) |

### Почему JPEG, а не WebP/PNG
- PNG для фото в 3–5× тяжелее JPEG (нет «ручки качества» в canvas) — бьёт по размеру bytea в Postgres.
- WebP меньше JPEG, но его поддержка **на вход локальной vision-модели (Gemma 3 через Ollama)** нестабильна и зависит от версии декодера (stb_image исторически без WebP). JPEG декодируется везде гарантированно — если фото когда-нибудь пойдут в RAG/эксперт-чат, формат не подведёт. `content_type` хранится явно.

### Момент сохранения — выбран вариант C
- **A (media в payload товара, base64 в JSON):** раздувает тело запроса (2 МБ → ~2.7 МБ base64), не переиспользует multipart. Отклонено.
- **B (немедленное сохранение каждого действия):** в режиме «создание» нет `goods_id` → пришлось бы блокировать вкладку до первого сохранения. Непоследовательно с единой кнопкой. Отклонено.
- **C (коммит по «Сохранить»):** вкладка держит рабочий список в состоянии формы; при «Сохранить» — создаём/обновляем товар → получаем `goods.id` → догружаем новые файлы multipart'ом → отправляем финальный упорядоченный список. Единая кнопка делает всё, одинаково для создания/редактирования, байты идут multipart (не base64). **Выбрано.**

## Схема БД

Новая миграция `V6__create_media_content.sql`:

```sql
CREATE TABLE media_content (
    media_id     VARCHAR(36) PRIMARY KEY REFERENCES media(id) ON DELETE CASCADE,
    content      BYTEA        NOT NULL,
    content_type VARCHAR(100) NOT NULL,   -- mime, напр. image/jpeg
    size_bytes   INT          NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE media_content IS 'Бинарное содержимое фото, хранимых в БД (привязано к media)';
```

- Один-к-одному с `media`: внешнее фото — строка `media` без `media_content`; фото из БД — `media` (type='IMAGE') + `media_content`.
- Для фото из БД `media.url = '/api/media/{mediaId}'` — короткий стабильный путь (укладывается в `VARCHAR(2000)`).
- `media` остаётся soft-delete. При soft-delete родителя `media_content` физически сохраняется (согласуется с философией проекта; `ON DELETE CASCADE` сработает лишь при редком hard-delete).
- Различение внутреннее/внешнее: по наличию `media_content` (или по префиксу url `/api/media/`).

## Бэкенд

### Публичная отдача байтов
`MediaContentController`:
- `GET /api/media/{id}` → читает `media_content` по `media_id`, возвращает `ResponseEntity<ByteArray>` с заголовками `Content-Type: <content_type>` и `Cache-Control: public, max-age=31536000, immutable` (байты по id неизменны). 404, если контента нет.
- `SecurityConfig`: добавить `/api/media/**` → `permitAll()` (иначе `<img src>` без Bearer-токена → 401).

### Админ-управление медиа
`AdminMediaController` `/api/admin/goods/{goodsId}/media`, все методы `@PreAuthorize("hasRole('ADMIN')")`:
- `GET` → `List<AdminMediaDto{ id, type, url, order }>` (с **id** — нужен фронту для удаления/порядка; storefront-DTO `MediaDto` не трогаем).
- `POST /upload` (multipart, `@RequestPart("file") FilePart`) → принимает сжатый JPEG → создаёт `media`(type='IMAGE', url=`/api/media/{id}`, order = max+1) + `media_content`(bytes, content_type, size) → `AdminMediaDto`.
- `PUT` (JSON: упорядоченный список `{ id?, url?, order }`) → реконсиляция набора медиа товара:
  - soft-delete строк `media`, которых нет в присланном списке;
  - insert новых внешних (элементы с `url` без `id`, type='IMAGE');
  - update `display_order` существующих по `id`;
  - возвращает финальный `List<AdminMediaDto>`.

### Сервис/репозиторий
- `MediaContentEntity(@Table("media_content"))` с полем `content: ByteArray`, `contentType: String`, `sizeBytes: Int`, `mediaId: String` (@Id).
- `MediaContentR2dbcRepository : CoroutineCrudRepository<MediaContentEntity, String>`.
- `MediaService` + `MediaRepository` (обёртка над `MediaR2dbcRepository` + `MediaContentR2dbcRepository`): `listByGoods`, `addUrl`, `uploadFile`, `reconcile(orderedList)`, `loadContent(mediaId)`.
- r2dbc-postgresql маппит BYTEA ↔ ByteArray нативно; при несовпадении (ByteBuffer) — добавить конвертер в `R2dbcConfig`. Data class с `ByteArray` не полагается на equals/hashCode.

### Фикс латентного бага
Сейчас `GoodsRepository.save` всегда `saveAll(goods.media.map { toEntity(it, id, id=null) })` → при апдейте товара пере-вставлял бы (дублировал) строки `media`. Баг дремлет, т.к. админ-товары создаются с пустым media.

Решение: медиа теперь полностью владеет `MediaService`. **Убрать персист media из `GoodsRepository.save` и из `GoodsService.createGoods/updateGoods`.** Чтение media в `findById/findByType/findByTypePaged/findAllPaged` (через `mediaR2dbcRepository`) остаётся без изменений. Это чистое разделение ответственности и устранение бага.

> Проверка: `GoodsRepository.save` используется только админ-флоу (Explore подтвердил), сид-данные грузятся SQL'ом, так что удаление media-персиста из save безопасно.

## Фронтенд

### Вкладка «Фотографии» в `GoodsForm.tsx`
- 4-й `Nav.Link eventKey="photos"` → «Фотографии» → компонент `<MediaManager>`.
- Состояние формы расширяется списком `photos: PhotoDraft[]`, где draft — один из:
  - `{ kind: 'url', url, order }` — внешний (новый или существующий);
  - `{ kind: 'db', mediaId, url: '/api/media/{id}', order }` — существующий из БД;
  - `{ kind: 'db-new', file: Blob, previewUrl: objectURL, order }` — новый загруженный/снятый (сжатый JPEG, ещё не на сервере).

### Компонент `<MediaManager>`
- **Сетка превью**: миниатюры; на каждой — ✕ (удалить), стрелки ↑/↓ (порядок); на первой — бейдж «Превью»; на остальных — кнопка «сделать превью» (переместить в начало).
- **Добавление**:
  - «Загрузить файлы» — `<input type="file" accept="image/*" multiple>`;
  - «Сделать фото» — `<input type="file" accept="image/*" capture="environment">`;
  - «По ссылке» — текстовое поле + «Добавить» (валидация http(s)-URL).
- При выборе файлов: `compressImage(file)` → добавить `db-new` draft с objectURL-превью. Освобождать objectURL при размонтировании/удалении.

### Утилита `compressImage(file): Promise<Blob>`
- Читает файл в `<img>`/`createImageBitmap`, рисует на `<canvas>` с даунскейлом по длинной стороне ≤ 1600px (сохраняя пропорции).
- Экспорт `canvas.toBlob(cb, 'image/jpeg', quality)` с quality≈0.8; если результат > 2 МБ — повторить с меньшим quality (напр. шаги 0.7/0.6/0.5) до укладывания в лимит.
- Возвращает `Blob` (image/jpeg).

### Оркестрация сохранения (`handleSubmit`)
1. `create/updateGoods` (скалярные поля, как сейчас) → получить `goodsId` (из ответа create или из `goods.id` при update).
2. Для каждого `db-new`: `adminService.uploadGoodsPhoto(goodsId, blob)` → `POST /api/admin/goods/{goodsId}/media/upload` (multipart) → `{ id, url }`; заменить draft на `db` с полученным id.
3. `adminService.reconcileGoodsMedia(goodsId, orderedList)` → `PUT /api/admin/goods/{goodsId}/media` с финальным упорядоченным списком `{ id?, url?, order }`.
4. `onSuccess()`.
- Кнопка «Сохранить» заблокирована + индикатор на время шагов 2–3.
- Частичный сбой (товар сохранён, фото — частично): тост с ошибкой; скалярные изменения уже в БД, медиа можно дозагрузить, переоткрыв товар в режиме редактирования.

### Загрузка в режиме редактирования
- При открытии редактирования: `GET /api/admin/goods/{id}/media` → заполнить `photos` существующими (внешние `kind:'url'` + из БД `kind:'db'`, с id и order, отсортированными по order).

### Типы
- `types/admin.ts`: `AdminMedia { id, type, url, order }`, `MediaReconcileItem { id?, url?, order }`.
- `adminService.ts`: `getGoodsMedia(goodsId)`, `uploadGoodsPhoto(goodsId, blob)`, `reconcileGoodsMedia(goodsId, items)`.

### Storefront — без изменений
`MediaCarousel`/`LazyImage`/`LargeMediaCarousel`/`MediaModal` читают `media[].url` и `media[].type`. Фото из БД отдаются по `/api/media/{id}` как обычный URL → витрина работает без правок.

## Вне scope (YAGNI)
- Видео в этой вкладке — нет (только фото; видео курсов остаётся через `goods.video_url`).
- Общая медиа-библиотека с переиспользованием фото между товарами — нет.
- Drag-and-drop порядок — опционально; базово стрелки ↑/↓ (надёжно и доступно).
- Серверное пере-сжатие/генерация миниатюр — нет (клиент уже отдаёт ≤1600px JPEG).
- Хард-удаление `media_content` для освобождения места при soft-delete — нет (сохраняем, как везде в проекте).

## Риски / проверки
- **R2DBC BYTEA↔ByteArray**: проверить маппинг на запись/чтение; при ByteBuffer — конвертер в `R2dbcConfig`.
- **Размер multipart**: убедиться, что лимиты Spring WebFlux (`spring.codec.max-in-memory-size` / multipart) допускают ≤2 МБ файлы; при необходимости поднять.
- **Удаление media-персиста из `GoodsRepository.save`**: прогнать создание/обновление товара, убедиться, что media не теряется и не дублируется.
- **Кэш отдачи байтов**: `immutable` корректен, т.к. id↔байты неизменны (новое фото = новый media.id).

## Verification
- `cd frontend && npm run build` и `cd backend && mvn test` зелёные.
- Миграция V6 применяется Flyway без ошибок; таблица `media_content` создана.
- Ручной прогон на http://localhost:8080 (пересборка фронта + рестарт бэка), QA-аккаунт `spiderru5597@gmail.com` (ADMIN):
  - создать товар, добавить фото по ссылке + загрузкой файла + камерой (на мобильном вьюпорте), сменить порядок, сохранить;
  - открыть товар в каталоге → превью = первое фото, карусель показывает все;
  - редактировать: список подтягивается с id/порядком; удалить фото, переставить, сохранить → изменения видны на витрине;
  - `<img src="/api/media/{id}">` грузится без авторизации (publicAll);
  - обновить только скалярные поля товара → media не задублировалась/не пропала.
