# Кнопка «Заполнить по названию» в карточке товара

Дата: 2026-06-29

## Цель

В форме создания/редактирования товара (`GoodsForm`) добавить небольшую кнопку рядом с
меткой поля «Название». Кнопка активна только когда поле названия непустое. По нажатию она
предзаполняет поля карточки товара — аналогично кнопке «Анализировать фото» во вкладке
«Фотографии», но боту передаются **не изображения, а название товара в тексте сообщения**.

## Контекст (существующий поток «Анализировать фото»)

1. `MediaManager` собирает фотографии, кнопка «🔍 Анализировать фото» зовёт `onAnalyze`.
2. `GoodsForm.handleAnalyze` → `adminService.analyzePhotos(photos)`.
3. `POST /api/admin/goods/analyze-photos` (multipart) → `AdminPhotoAnalyzeController.analyze`.
4. `PhotoAnalyzeService.analyze` резолвит картинки в байты и зовёт `AiBotService.analyzePhotos`.
5. `AiBotService.analyzePhotos` (агент `photo-analyzer`) шлёт `{message: "", images: dataUrls}`
   на `/{chatId}/chat/stream-with-images`, читает SSE, парсит в `PlantCardDto` через `parsePlantCard`.
6. `GoodsForm.handleAnalyze` заполняет `name`, `description`, `detailedDescription`,
   `careInstructions`, `categoryId` (первая категория типа `PLANT`).

## Решения (согласованы с пользователем)

- **Агент:** тот же `photo-analyzer`. Боту передаётся название в `message`, `images` — пустой массив.
- **Поле «Название»:** **не перезаписывается** ответом бота (пользователь ввёл его сам).
- **Категория:** автозаполняется типом `PLANT`, как в анализе по фото.

## Дизайн

### Backend

1. **`AiBotService.kt`**
   - Выделить из текущего `analyzePhotos` общий приватный хелпер
     `private suspend fun callPhotoAnalyzer(message: String, dataUrls: List<String>): PlantCardDto`,
     который делает POST на `/{chatId}/chat/stream-with-images` с телом
     `{message, images: dataUrls}`, читает SSE (`sseDataFlow`) и парсит `parsePlantCard`.
   - `analyzePhotos(images)` остаётся публичным, но делегирует в `callPhotoAnalyzer("", dataUrls)`.
   - Добавить `suspend fun analyzeByName(name: String): PlantCardDto`, который зовёт
     `callPhotoAnalyzer(name, emptyList())`.
   - Тип агента — `properties.photoAnalyzerAgentType` (как сейчас в `analyzePhotos`).

2. **`PhotoAnalyzeService.kt`**
   - Добавить `suspend fun analyzeByName(name: String): PlantCardDto`:
     - если `name` пустое/blank → `PlantCardDto(error = "Введите название для анализа")`;
     - иначе `aiBotService.analyzeByName(name)` с тем же перехватом, что и в `analyze`:
       `AiBotServiceException` → `PlantCardDto(error = "AI-сервис недоступен, попробуйте позже")`,
       `AiBotTimeoutException` → `PlantCardDto(error = "Анализ занял слишком долго, попробуйте ещё раз")`.

3. **`AdminPhotoAnalyzeController.kt`**
   - Новый эндпоинт `POST /api/admin/goods/analyze-name`, тело JSON `AnalyzeByNameRequest`,
     возвращает `PlantCardDto`. Роль `ADMIN` наследуется от `@PreAuthorize` на классе.

4. **`controller/model/Dtos.kt`**
   - `data class AnalyzeByNameRequest(val name: String)`.

### Frontend

5. **`adminService.ts`**
   - `analyzeByName: async (name: string): Promise<PlantCard>` →
     `POST /admin/goods/analyze-name` с телом `{ name }`, возвращает `response.data`.

6. **`GoodsForm.tsx`**
   - Новое состояние `analyzingName` (отдельно от `analyzing`).
   - `handleAnalyzeByName()`:
     - `setAnalyzingName(true)`; в `finally` — `false`;
     - `const card = await adminService.analyzeByName(formData.name.trim())`;
     - при `card.error` → `toast.error(card.error)` и выход;
     - иначе `setFormData` с маппингом: `description ← card.shortDescription`,
       `detailedDescription ← card.fullDescription`, `careInstructions ← card.care`,
       `categoryId ← categories.find(c => c.type === 'PLANT')?.id` (каждое с фолбэком `?? prev.*`)
       — **поле `name` не меняется**;
     - `toast.success('Поля заполнены по названию')`;
     - `catch` → `toast.error('Не удалось проанализировать название')`.
   - Разметка строки метки «Название *»: обернуть метку и кнопку в flex-контейнер
     (`d-flex align-items-center justify-content-between`), метке снять нижний отступ.
     Справа — кнопка `variant="outline-success" size="sm"`,
     `disabled={!formData.name.trim() || analyzingName || loading}`.
     Текст: «✨ Заполнить по названию»; во время запроса — `<Spinner size="sm" />Анализ…`.

## Обработка ошибок

- Пустое имя на бэке → `PlantCardDto(error)`; на фронте кнопка и так заблокирована при пустом поле.
- Ошибки связи/таймаута бота → `PlantCardDto(error)` (как в анализе по фото) → toast.
- Сетевой сбой запроса → `catch` во фронтовом хендлере → toast.

## Тестирование

Ручной QA в браузере под админ-аккаунтом: ввести название, нажать кнопку, убедиться, что
описание/детали/уход/категория заполнились, а название осталось прежним; проверить, что
кнопка неактивна при пустом названии. Автоматических тестов для этих потоков в проекте нет.

## Вне области (YAGNI)

- Отдельный тип агента/промпт под анализ по названию — используем существующий `photo-analyzer`.
- Перезапись поля «Название».
- Изменения в боте `AIAgentNew` (внешний проект).
