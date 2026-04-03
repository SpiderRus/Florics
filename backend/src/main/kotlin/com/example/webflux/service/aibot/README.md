# AI Bot Service Integration

Reactive WebClient интеграция с AI Agent чат-ботом.

## Компоненты

- **AiBotService** - основной сервис для взаимодействия с AI Agent
- **AiConversationRepository** - in-memory хранилище маппинга conversationId → userId
- **DTOs** - модели данных, совпадающие с AI Agent API
- **Exceptions** - кастомные исключения для обработки ошибок

## Использование

### Injection

```kotlin
@Service
class MyService(private val aiBotService: AiBotService) {
    // ...
}
```

### Создание разговора

```kotlin
suspend fun createChat() {
    val userId = SecurityUtils.getAuthenticatedUserId()
    val conversation = aiBotService.createConversation(userId, "Консультация по товарам")
    println("Created conversation: ${conversation.id}")
}
```

### Отправка сообщения

```kotlin
suspend fun askAi(conversationId: UUID) {
    val userId = SecurityUtils.getAuthenticatedUserId()
    
    val response = aiBotService.sendMessage(
        userId = userId,
        conversationId = conversationId,
        message = "Какие флорариумы лучше для начинающих?",
        useRag = true  // true для использования документов из векторной БД, false для базовых знаний LLM
    )
    
    println("AI ответ: ${response.response}")
    println("Время ответа: ${response.timestamp}")
}
```

### Получение истории

```kotlin
suspend fun getHistory(conversationId: UUID) {
    val userId = SecurityUtils.getAuthenticatedUserId()
    val messages = aiBotService.getMessages(userId, conversationId, limit = 50)
    
    messages.forEach { msg ->
        println("[${msg.role}] ${msg.content}")
    }
}
```

### Список разговоров пользователя

```kotlin
suspend fun listMyChats() {
    val userId = SecurityUtils.getAuthenticatedUserId()
    val conversations = aiBotService.listUserConversations(userId)
    
    conversations.forEach { conv ->
        println("${conv.title} (${conv.messageCount} messages)")
    }
}
```

### Удаление разговора

```kotlin
suspend fun deleteChat(conversationId: UUID) {
    val userId = SecurityUtils.getAuthenticatedUserId()
    aiBotService.deleteConversation(userId, conversationId)
}
```

## Изоляция пользователей

Сервис автоматически проверяет ownership разговоров:
- При создании conversation маппинг сохраняется в AiConversationRepository
- При доступе к conversation проверяется что текущий userId совпадает с владельцем
- Если пользователь пытается получить доступ к чужому conversation → `ConversationAccessDeniedException`

## Обработка ошибок

Все методы могут выбросить:
- `ConversationNotFoundException` - разговор не найден
- `ConversationAccessDeniedException` - доступ запрещен (не владелец)
- `AiBotServiceException` - ошибка связи с AI Agent или серверная ошибка
- `AiBotTimeoutException` - таймаут запроса к AI Agent

## Конфигурация

В `application.yml`:
```yaml
ai-agent:
  base-url: http://localhost:8081
  base-path: /api/v1
  connect-timeout: 5000
  read-timeout: 30000
```

## Требования

- AI Agent должен быть запущен на http://localhost:8081
- Пользователь должен быть авторизован (для получения userId через SecurityUtils)
