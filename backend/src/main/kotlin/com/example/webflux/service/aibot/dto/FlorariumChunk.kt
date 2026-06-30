package com.example.webflux.service.aibot.dto

/**
 * Единица SSE-потока к фронтенду для чата-дизайнера флорариумов (по аналогии с [TokenChunk]).
 *
 * Сериализуется в JSON внутри поля `data` SSE-события и несёт один из двух видов:
 *  - текст:    {"type":"text","text":"<фрагмент>"}
 *  - картинка: {"type":"image","imageUrl":"/api/aibot/florarium/images/<uuid>"}
 *
 * JSON выбран, чтобы сохранять значимые ведущие пробелы токенов LLM (см. [TokenChunk]).
 * [imageUrl] — это УЖЕ переписанный путь backend-прокси Florics (не исходный URL бота),
 * чтобы браузер тянул картинку через защищённый backend-эндпоинт под ролью BUYER.
 *
 * @property type Тип события: "text" | "image"
 * @property text Фрагмент текста (для type=text)
 * @property imageUrl Путь backend-прокси к картинке (для type=image)
 */
data class FlorariumChunk(
    val type: String,
    val text: String? = null,
    val imageUrl: String? = null
)
