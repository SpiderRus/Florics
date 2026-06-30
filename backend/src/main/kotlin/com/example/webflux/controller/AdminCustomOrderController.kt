package com.example.webflux.controller

import com.example.webflux.controller.model.CustomOrderDto
import com.example.webflux.controller.model.UpdateCustomOrderRequest
import com.example.webflux.service.CustomOrderService
import com.example.webflux.service.CustomOrderView
import com.example.webflux.service.aibot.dto.MessageResponse
import com.example.webflux.service.aibot.dto.TokenChunk
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * Управление кастомными заказами флорариумов (роль ADMIN).
 * Заказы хранятся в purchases (conversation_id IS NOT NULL); админ проставляет цену и статус.
 */
@RestController
@RequestMapping("/api/admin/custom-orders")
@Tag(name = "Админ - Заказы флорариумов", description = "API управления кастомными заказами флорариумов")
class AdminCustomOrderController(
    private val customOrderService: CustomOrderService
) {

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Список кастомных заказов", description = "Все заказы кастомных флорариумов с данными пользователя")
    suspend fun listOrders(): ResponseEntity<List<CustomOrderDto>> =
        ResponseEntity.ok(customOrderService.listAllOrders().map { it.toCustomOrderDto() })

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Обновить заказ", description = "Проставить цену и/или статус кастомного заказа")
    suspend fun updateOrder(
        @PathVariable id: String,
        @RequestBody request: UpdateCustomOrderRequest
    ): ResponseEntity<CustomOrderDto> {
        return try {
            ResponseEntity.ok(customOrderService.updateOrder(id, request.price, request.status).toCustomOrderDto())
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    // ===== Экспертный чат мастера (florarium-expert) по заказу =====

    /**
     * Открыть/восстановить экспертный чат по заказу: гарантирует сессию в боте (topic = id разговора +
     * url картинки) и возвращает историю. chatId детерминирован от id заказа.
     */
    @PostMapping("/{id}/expert/session")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Открыть чат с экспертом по заказу", description = "Создаёт/восстанавливает сессию florarium-expert и возвращает историю")
    suspend fun openExpertChat(@PathVariable id: String): ResponseEntity<ExpertChatResponse> {
        return try {
            val view = customOrderService.openExpertChat(id)
            ResponseEntity.ok(ExpertChatResponse(view.conversationId, view.messages, view.designImages, view.selectedImageUrl))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * Стрим ответа эксперта на сообщение мастера (SSE текстовых токенов).
     */
    @PostMapping("/{id}/expert/chat/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Сообщение эксперту (стрим)", description = "Отправляет сообщение мастера и стримит ответ эксперта")
    suspend fun streamExpert(
        @PathVariable id: String,
        @RequestBody request: ExpertChatRequest
    ): Flow<TokenChunk> =
        customOrderService.streamExpert(id, request.message).map { TokenChunk(it) }

    data class ExpertChatRequest(val message: String)

    data class ExpertChatResponse(
        val conversationId: String,
        val messages: List<MessageResponse>,
        val designImages: List<String>,
        val selectedImageUrl: String?
    )

    private fun CustomOrderView.toCustomOrderDto(): CustomOrderDto = CustomOrderDto(
        id = order.id!!,
        userId = order.userId,
        userName = userName,
        userEmail = userEmail,
        conversationId = order.conversationId,
        imageUrl = order.imageUrl,
        customerComment = order.customerComment,
        contact = order.contact,
        price = order.price,
        status = order.status,
        purchaseDate = order.purchaseDate
    )
}
