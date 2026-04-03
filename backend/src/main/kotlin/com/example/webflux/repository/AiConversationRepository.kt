package com.example.webflux.repository

import com.example.webflux.entity.AiConversationEntity
import com.example.webflux.repository.r2dbc.AiConversationR2dbcRepository
import org.springframework.stereotype.Repository
import java.util.*

/**
 * R2DBC репозиторий для хранения маппинга AI разговоров к пользователям
 *
 * Хранит связи (userId, goodsId) → conversationId - привязка разговоров к товарам
 *
 * Используется для изоляции разговоров между пользователями и создания отдельных
 * conversation для каждого товара.
 */
@Repository
class AiConversationRepository(
    private val aiConversationR2dbcRepository: AiConversationR2dbcRepository
) {

    /**
     * Сохранить связь conversation → user (deprecated, use saveGoodsConversation)
     */
    suspend fun save(conversationId: UUID, userId: Long) {
        // Deprecated - use saveGoodsConversation instead
    }

    /**
     * Найти владельца разговора
     *
     * @param conversationId UUID разговора
     * @return ID пользователя-владельца или null если не найдено
     */
    suspend fun findByConversationId(conversationId: String): String? {
        val entity = aiConversationR2dbcRepository.findByConversationId(conversationId)
        return entity?.userId
    }

    /**
     * Найти все разговоры пользователя
     *
     * @param userId ID пользователя (UUID string)
     * @return Список UUID разговоров, принадлежащих пользователю
     */
    suspend fun findByUserId(userId: String): List<String> {
        val result = mutableListOf<String>()
        aiConversationR2dbcRepository.findByUserId(userId)
            .collect { entity -> result.add(entity.conversationId) }
        return result
    }

    /**
     * Сохранить связь conversation → (user, goods)
     *
     * @param userId ID пользователя (UUID string)
     * @param goodsId ID товара (UUID string)
     * @param conversationId UUID разговора
     */
    suspend fun saveGoodsConversation(userId: String, goodsId: String, conversationId: String) {
        val entity = AiConversationEntity(
            userId = userId,
            goodsId = goodsId,
            conversationId = conversationId
        )
        aiConversationR2dbcRepository.save(entity)
    }

    /**
     * Найти conversation для конкретного пользователя и товара
     *
     * @param userId ID пользователя (UUID string)
     * @param goodsId ID товара (UUID string)
     * @return UUID разговора или null если не найдено
     */
    suspend fun findConversationByUserAndGoods(userId: String, goodsId: String): String? {
        val entity = aiConversationR2dbcRepository.findByUserIdAndGoodsId(userId, goodsId)
        return entity?.conversationId
    }

    /**
     * Удалить маппинг при удалении разговора
     *
     * @param conversationId UUID разговора для удаления
     * @return true если маппинг был удален, false если не существовал
     */
    suspend fun deleteByConversationId(conversationId: String): Boolean {
        val exists = aiConversationR2dbcRepository.findByConversationId(conversationId) != null
        if (!exists) return false

        aiConversationR2dbcRepository.deleteByConversationId(conversationId)
        return true
    }

    /**
     * Проверить существование маппинга
     *
     * @param conversationId UUID разговора
     * @return true если маппинг существует
     */
    suspend fun exists(conversationId: String): Boolean {
        return aiConversationR2dbcRepository.findByConversationId(conversationId) != null
    }
}
