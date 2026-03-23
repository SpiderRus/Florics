# Миграция на Spring Boot 4.0.3

Документ описывает процесс миграции проекта Plant Shop с Spring Boot 3.2.2 на Spring Boot 4.0.3.

## Обзор изменений

### Версии зависимостей

| Компонент | Версия 3.x | Версия 4.x |
|-----------|-----------|-----------|
| Spring Boot | 3.2.2 | 4.0.3 |
| Spring Framework | 6.x | 7.0.5 |
| Spring Security | 6.x | 7.x |
| Kotlin | 1.9.22 | 2.1.0 |
| Jackson | 2.x | 3.x |
| SpringDoc OpenAPI | 2.3.0 | 3.0.1 |
| Micrometer Context Propagation | 1.1.0 | 1.2.0 |
| Java | 17 | 17+ (поддержка до Java 21+) |

## Выполненные изменения

### 1. Обновление версий в pom.xml

**Родительский pom.xml:**
```xml
<properties>
    <java.version>17</java.version>
    <kotlin.version>2.1.0</kotlin.version>
    <spring-boot.version>4.0.3</spring-boot.version>
</properties>
```

**Backend pom.xml:**
- SpringDoc OpenAPI: `2.3.0` → `3.0.1`
- Micrometer: `1.1.0` → `1.2.0`

### 2. Исправления для Kotlin 2.1.0

**SecurityUtils.kt** - более строгая проверка nullable типов:

```kotlin
// Было:
roles = authentication.authorities.map { it.authority.removePrefix("ROLE_") }.toSet()

// Стало:
roles = authentication.authorities.mapNotNull { it.authority?.removePrefix("ROLE_") }.toSet()
```

**Причина:** Kotlin 2.1.0 требует явной обработки nullable типов. Свойство `authority` имеет тип `String?`, поэтому:
- Используем `mapNotNull` вместо `map` для фильтрации null значений
- Добавляем safe call operator `?.` при вызове `removePrefix()`

### 3. Jackson Configuration (Jackson 3.x)

**JacksonConfiguration.kt** - обновление API для Jackson 3.x:

```kotlin
@Bean
@Primary
fun objectMapper(): ObjectMapper {
    return JsonMapper.builder()  // Новый builder API
        .addModule(kotlinModule())
        .addModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build()
}

@Suppress("DEPRECATION")  // Подавление предупреждений о deprecated API
override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
    val mapper = objectMapper()
    // Старые классы Jackson2JsonEncoder/Decoder всё ещё работают
    configurer.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(mapper))
    configurer.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(mapper))
}
```

**Изменения:**
- `ObjectMapper()` → `JsonMapper.builder()...build()`
- `registerModule()` → `addModule()`
- Классы `Jackson2JsonEncoder`/`Jackson2JsonDecoder` помечены как deprecated, но продолжают работать
- Новые классы `JacksonJsonEncoder`/`JacksonJsonDecoder` требуют только `JsonMapper` (не `ObjectMapper`)

### 4. Security Configuration

Конфигурация Spring Security **не требовала изменений**. Spring Security 7.x обратно совместима с кодом для версии 6.x:

```kotlin
@Bean
fun securityWebFilterChain(
    http: ServerHttpSecurity,
    tokenIntrospector: ReactiveOpaqueTokenIntrospector
): SecurityWebFilterChain {
    return http
        .csrf { it.disable() }
        .httpBasic { it.disable() }
        .formLogin { it.disable() }
        // ... остальная конфигурация без изменений
}
```

## Обратная совместимость

### Что работает без изменений:

✅ **Spring Security** - вся конфигурация через `ServerHttpSecurity`
✅ **WebFlux** - контроллеры с корутинами и Flow
✅ **ReactiveOpaqueTokenIntrospector** - кастомная JWT аутентификация
✅ **OpenAPI/Swagger** - после обновления springdoc до 3.0.1
✅ **Spring Boot Actuator** - все эндпоинты
✅ **Reactor** - Mono, Flux, Flow интеграция

### Что требует внимания:

⚠️ **Jackson кодеки** - используют deprecated API (работает, но с предупреждениями)
⚠️ **Kotlin null-safety** - более строгие проверки в 2.1.0

## Процесс миграции (пошаговая инструкция)

### Шаг 1: Обновление версий
```bash
# Обновить pom.xml с новыми версиями
git diff pom.xml backend/pom.xml
```

### Шаг 2: Исправление Kotlin кода
```bash
# Компиляция покажет все несовместимости
mvn clean compile
```

Исправить ошибки null-safety в Kotlin файлах.

### Шаг 3: Обновление Jackson конфигурации
```bash
# Использовать JsonMapper.builder() вместо ObjectMapper()
# Подавить предупреждения о deprecated через @Suppress
```

### Шаг 4: Сборка и тестирование
```bash
# Полная сборка с тестами
mvn clean install

# Запуск приложения
cd backend && mvn spring-boot:run
```

### Шаг 5: Проверка функциональности
- ✅ Запуск приложения на порту 8080
- ✅ Swagger UI доступен на `/swagger-ui.html`
- ✅ Аутентификация работает (JWT opaque tokens)
- ✅ Все REST API эндпоинты функционируют
- ✅ Frontend интеграция без изменений

## Результаты тестирования

```
Spring Boot ::                (v4.0.3)
Running with Spring Boot v4.0.3, Spring v7.0.5
Netty started on port 8080 (http)
Started ApplicationKt in 1.461 seconds
```

✅ **Тесты:** Все тесты прошли успешно
✅ **Компиляция:** Без ошибок
✅ **Запуск:** Приложение стартует за ~1.5 секунды
✅ **Функциональность:** Все эндпоинты работают

## Известные issues

### Deprecated Jackson API

**Проблема:** Классы `Jackson2JsonEncoder`/`Jackson2JsonDecoder` помечены как deprecated в Spring Boot 4.x.

**Статус:** Работает корректно с подавлением предупреждений через `@Suppress("DEPRECATION")`.

**Долгосрочное решение:** В будущем можно мигрировать на новый API:
```kotlin
// Новый подход (требует только JsonMapper, не ObjectMapper)
configurer.defaultCodecs().jacksonJsonEncoder(JacksonJsonEncoder(jsonMapper))
configurer.defaultCodecs().jacksonJsonDecoder(JacksonJsonDecoder(jsonMapper))
```

## Рекомендации

1. **Тестирование:** Выполнить полный набор интеграционных тестов после миграции
2. **Мониторинг:** Следить за performance metrics после деплоя
3. **Логирование:** Проверить совместимость логов (форматы не изменились)
4. **Зависимости:** Убедиться, что все транзитивные зависимости совместимы

## Ссылки

- [Spring Boot 4.0 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes)
- [Spring Framework 7.0 What's New](https://docs.spring.io/spring-framework/reference/7.0/whatsnew.html)
- [Kotlin 2.1.0 Release](https://kotlinlang.org/docs/whatsnew21.html)
- [Jackson 3.0 Migration Guide](https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.0)

## Контрольный список миграции

- [x] Обновлены версии в pom.xml
- [x] Исправлена null-safety в Kotlin коде
- [x] Обновлена Jackson конфигурация
- [x] Успешная компиляция без ошибок
- [x] Все тесты проходят
- [x] Приложение запускается
- [x] API эндпоинты функционируют
- [x] Обновлена документация (CLAUDE.md)
- [x] Создан документ миграции (MIGRATION-4.0.md)

**Дата миграции:** 2026-03-23
**Исполнитель:** Claude Sonnet 4.6
**Время выполнения:** ~30 минут
