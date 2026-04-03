package com.example.webflux.repository

import org.springframework.stereotype.Repository
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory репозиторий для хранения маппинга AI разговоров к пользователям
 *
 * Хранит две связи:
 * 1. conversationId (UUID из AI Agent) → userId (Long из Florics) - изоляция пользователей
 * 2. (userId, goodsId) → conversationId - привязка разговоров к товарам
 *
 * Используется для изоляции разговоров между пользователями и создания отдельных
 * conversation для каждого товара.
 *
 * Следует паттерну других in-memory репозиториев в проекте
 * (UserRepository, CartRepository, GoodsRepository).
 */
@Repository
class AiConversationRepository {

    /**
     * Маппинг conversationId -> userId
     */
    private val storage = ConcurrentHashMap<UUID, Long>()

    /**
     * Маппинг (userId, goodsId) -> conversationId
     * Используется для создания отдельного conversation для каждого товара
     */
    private val goodsConversations = ConcurrentHashMap<Pair<Long, Long>, UUID>()

    /**
     * Сохранить связь conversation → user
     *
     * @param conversationId UUID разговора из AI Agent
     * @param userId ID пользователя из Florics
     */
    suspend fun save(conversationId: UUID, userId: Long) {
        storage[conversationId] = userId
    }

    /**
     * Найти владельца разговора
     *
     * @param conversationId UUID разговора
     * @return ID пользователя-владельца или null если не найдено
     */
    suspend fun findByConversationId(conversationId: UUID): Long? {
        return storage[conversationId]
    }

    /**
     * Найти все разговоры пользователя
     *
     * @param userId ID пользователя
     * @return Список UUID разговоров, принадлежащих пользователю
     */
    suspend fun findByUserId(userId: Long): List<UUID> {
        return storage.entries
            .filter { it.value == userId }
            .map { it.key }
    }

    /**
     * Сохранить связь conversation → (user, goods)
     *
     * @param userId ID пользователя
     * @param goodsId ID товара
     * @param conversationId UUID разговора
     */
    suspend fun saveGoodsConversation(userId: Long, goodsId: Long, conversationId: UUID) {
        goodsConversations[Pair(userId, goodsId)] = conversationId
    }

    /**
     * Найти conversation для конкретного пользователя и товара
     *
     * @param userId ID пользователя
     * @param goodsId ID товара
     * @return UUID разговора или null если не найдено
     */
    suspend fun findConversationByUserAndGoods(userId: Long, goodsId: Long): UUID? {
        return goodsConversations[Pair(userId, goodsId)]
    }

    /**
     * Удалить маппинг при удалении разговора
     *
     * Очищает оба маппинга: conversationId → userId и (userId, goodsId) → conversationId
     *
     * @param conversationId UUID разговора для удаления
     * @return true если маппинг был удален, false если не существовал
     */
    suspend fun deleteByConversationId(conversationId: UUID): Boolean {
        val removed = storage.remove(conversationId) != null
        // Удалить из goodsConversations все записи с этим conversationId
        goodsConversations.entries.removeIf { it.value == conversationId }
        return removed
    }

    /**
     * Проверить существование маппинга
     *
     * @param conversationId UUID разговора
     * @return true если маппинг существует
     */
    suspend fun exists(conversationId: UUID): Boolean {
        return storage.containsKey(conversationId)
    }
}
