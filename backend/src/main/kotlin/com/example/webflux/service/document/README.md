# DocumentUploadService

Реактивный сервис для загрузки текстовых документов в AI Agent с автоматической векторизацией и подготовкой для RAG (Retrieval-Augmented Generation).

## Обзор

`DocumentUploadService` предоставляет suspend функции для отправки документов в AI Agent API. Загруженные документы автоматически:
- Разбиваются на chunks через TokenTextSplitter
- Векторизуются через Ollama (модель nomic-embed-text, 768 измерений)
- Сохраняются в pgvector для семантического поиска
- Становятся доступны для RAG в Chat API с параметром `useRag=true`

## Структура

```
document/
├── DocumentServiceException.kt       # Базовое исключение сервиса
├── DocumentUploadTimeoutException.kt # Исключение timeout
├── dto/
│   ├── DocumentUploadRequest.kt     # DTO для запроса загрузки
│   └── DocumentResponse.kt          # DTO для ответа после загрузки
└── README.md                        # Эта документация
```

## Использование

### Базовая загрузка документа

```kotlin
@Service
class MyService(
    private val documentUploadService: DocumentUploadService
) {
    suspend fun uploadProductDescription(productId: Long, description: String) {
        val response = documentUploadService.uploadDocument(
            filename = "product_${productId}.txt",
            content = description
        )
        
        logger.info("Document uploaded: id=${response.id}, chunks=${response.chunksCount}")
    }
}
```

### Загрузка с метаданными

```kotlin
suspend fun uploadWithMetadata() {
    val response = documentUploadService.uploadDocument(
        filename = "faq.txt",
        content = "Q: Как оформить заказ?\nA: Добавьте товары в корзину и нажмите 'Оформить заказ'.",
        metadata = mapOf(
            "type" to "faq",
            "category" to "orders",
            "author" to "admin"
        )
    )
}
```

### Загрузка с типом и автором (convenience метод)

```kotlin
suspend fun uploadArticle() {
    val response = documentUploadService.uploadDocument(
        filename = "terrarium_care.txt",
        content = "Флорариумы требуют минимального ухода...",
        documentType = "article",
        author = "botanist"
    )
}
```

## Обработка ошибок

```kotlin
try {
    val response = documentUploadService.uploadDocument(
        filename = "test.txt",
        content = "Test content"
    )
} catch (e: DocumentUploadTimeoutException) {
    // AI Agent не ответил в течение timeout (30 секунд по умолчанию)
    logger.error("Document upload timed out", e)
} catch (e: DocumentServiceException) {
    // Ошибка сети, 4xx/5xx ответ от AI Agent
    logger.error("Document upload failed", e)
}
```

## REST API

### Эндпоинт загрузки документа

**POST** `/api/documents`

Требует аутентификацию (Bearer token).

**Request:**
```json
{
  "filename": "product_description.txt",
  "content": "Красивый флорариум с суккулентами...",
  "metadata": {
    "type": "product_description",
    "product_id": "123"
  }
}
```

**Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "filename": "product_description.txt",
  "chunksCount": 3,
  "uploadedAt": "2026-04-05T19:15:00"
}
```

**Error Response (400 Bad Request):**
```json
{
  "error": "Invalid document data: content cannot be blank"
}
```

### Удаление документа

```kotlin
suspend fun deleteOldDocument(documentId: String) {
    try {
        documentUploadService.deleteDocument(documentId)
        logger.info("Document deleted: {}", documentId)
    } catch (e: DocumentServiceException) {
        // Ошибка связи с AI Agent или неверный формат ID
        logger.error("Failed to delete document: {}", documentId, e)
    }
}
```

**Примечание**: Операция идемпотентна - повторное удаление того же документа не вызовет ошибку.

## REST API для удаления

**DELETE** `/api/documents/{id}`

Требует аутентификацию (Bearer token).

**Request:**
```bash
curl -X DELETE http://localhost:8080/api/documents/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer <token>"
```

**Response (204 No Content):**
Пустой ответ при успешном удалении.

**Идемпотентность:**
Повторное удаление того же документа также вернет 204 (операция идемпотентна).

**Error Response (400 Bad Request):**
```json
{
  "error": "Invalid document ID format"
}
```

## Интеграция с Chat API

После загрузки документ автоматически доступен для RAG:

```kotlin
// 1. Загрузить документ
documentUploadService.uploadDocument(
    filename = "product_catalog.txt",
    content = "Наш каталог включает флорариумы, террариумы..."
)

// 2. Использовать в чате с RAG
val chatResponse = aiBotService.sendMessage(
    userId = "user123",
    conversationId = conversationId,
    message = "Какие флорариумы есть в наличии?",
    useRag = true  // Включить поиск в загруженных документах
)
```

## Конфигурация

WebClient настроен в `WebClientConfig.kt`:

```yaml
ai-agent:
  base-url: http://localhost:8081
  base-path: /api/v1
  connect-timeout: 5000
  read-timeout: 30000  # 30 секунд для обработки документов
  pool:
    max-connections: 10
    pending-acquire-timeout: 5000
```

## Примеры использования

### Загрузка описаний товаров

```kotlin
@Service
class GoodsService(
    private val documentUploadService: DocumentUploadService,
    private val goodsRepository: GoodsRepository
) {
    suspend fun uploadAllGoodsDescriptions() {
        goodsRepository.findAll().collect { goods ->
            try {
                documentUploadService.uploadDocument(
                    filename = "goods_${goods.id}.txt",
                    content = """
                        Название: ${goods.name}
                        Описание: ${goods.description}
                        Цена: ${goods.price} руб.
                        Категория: ${goods.category}
                    """.trimIndent(),
                    metadata = mapOf(
                        "type" to "product",
                        "goods_id" to goods.id.toString(),
                        "category" to goods.category
                    )
                )
                logger.info("Uploaded description for goods ${goods.id}")
            } catch (e: DocumentServiceException) {
                logger.error("Failed to upload goods ${goods.id}", e)
            }
        }
    }
}
```

### Загрузка FAQ

```kotlin
suspend fun uploadFaq(questions: List<FaqItem>) {
    val content = questions.joinToString("\n\n") { 
        "Q: ${it.question}\nA: ${it.answer}" 
    }
    
    documentUploadService.uploadDocument(
        filename = "faq.txt",
        content = content,
        documentType = "faq"
    )
}
```

## Тестирование

```bash
# Получить токен
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"password123"}'

# Загрузить документ
curl -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "filename": "test.txt",
    "content": "Тестовый документ для RAG",
    "metadata": {"type": "test"}
  }'
```

## Swagger UI

Документация API доступна по адресу:
- http://localhost:8080/swagger-ui.html

## Зависимости

- AI Agent должен быть запущен на http://localhost:8081
- AI Agent требует Ollama с моделью nomic-embed-text
- AI Agent требует PostgreSQL с pgvector extension

## См. также

- `AiBotService` - сервис для отправки сообщений в чат с RAG
- `WebClientConfig` - конфигурация WebClient для AI Agent
- AI Agent DocumentController - исходный контроллер в проекте AIAgent
