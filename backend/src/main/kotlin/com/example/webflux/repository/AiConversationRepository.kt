package com.example.webflux.repository

import com.example.webflux.entity.AiConversationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

interface AiConversationR2dbcRepository : CoroutineCrudRepository<AiConversationEntity, String> {

    @Query("SELECT * FROM ai_conversations WHERE user_id = :userId")
    fun findByUserId(userId: String): Flow<AiConversationEntity>

    @Query("SELECT * FROM ai_conversations WHERE user_id = :userId AND goods_id = :goodsId")
    suspend fun findByUserIdAndGoodsId(userId: String, goodsId: String): AiConversationEntity?

    @Query("DELETE FROM ai_conversations WHERE user_id = :userId AND goods_id = :goodsId")
    suspend fun deleteByUserIdAndGoodsId(userId: String, goodsId: String)

    // Удалены избыточные методы, заменены стандартными CoroutineCrudRepository методами:
    // - findByConversationId -> используйте findById(conversationId)
    // - deleteByConversationId -> используйте deleteById(conversationId)
}

/**
 * R2DBC репозиторий для хранения маппинга AI разговоров к пользователям
 *
 * Хранит связи conversationId → (userId, goodsId?) - привязка внешних разговоров из AI Agent
 * к пользователям и опционально к товарам.
 *
 * Используется для изоляции разговоров между пользователями и создания отдельных
 * conversations для каждого товара (когда goodsId != null).
 */
@Repository
class AiConversationRepository(
    private val aiConversationR2dbcRepository: AiConversationR2dbcRepository,
    private val entityTemplate: R2dbcEntityTemplate
) {

    /**
     * Найти владельца разговора
     *
     * @param conversationId UUID разговора
     * @return ID пользователя-владельца или null если не найдено
     */
    suspend fun findByConversationId(conversationId: String): String? =
            aiConversationR2dbcRepository.findById(conversationId)?.userId

    /**
     * Найти все разговоры пользователя
     *
     * @param userId ID пользователя (UUID string)
     * @return Список UUID разговоров, принадлежащих пользователю
     */
    suspend fun findByUserId(userId: String): List<String> =
        aiConversationR2dbcRepository.findByUserId(userId).map { it.conversationId }.toList()

    /**
     * Сохранить связь conversation → (user, goods)
     *
     * @param userId ID пользователя (UUID string)
     * @param goodsId ID товара (UUID string), может быть null для общих разговоров
     * @param conversationId UUID разговора (PRIMARY KEY)
     * @return Сохраненная entity
     */
    suspend fun saveOrCreateGoodsConversation(userId: String, goodsId: String?, conversationId: String): AiConversationEntity {
        val entity = AiConversationEntity(
            conversationId = conversationId,  // PK первым параметром
            userId = userId,
            goodsId = goodsId  // Nullable - может быть null
        )

        return try {
                entityTemplate.insert(entity)
            } catch (e: DataIntegrityViolationException) {
                entityTemplate.update(entity)
            }.awaitSingle()
    }

    /**
     * Найти conversation для конкретного пользователя и товара
     *
     * @param userId ID пользователя (UUID string)
     * @param goodsId ID товара (UUID string)
     * @return UUID разговора или null если не найдено
     */
    suspend fun findConversationByUserAndGoods(userId: String, goodsId: String): String? =
        aiConversationR2dbcRepository.findByUserIdAndGoodsId(userId, goodsId)?.conversationId

    /**
     * Удалить маппинг при удалении разговора
     *
     * @param conversationId UUID разговора для удаления
     * @return true если маппинг был удален, false если не существовал
     */
    suspend fun deleteByConversationId(conversationId: String): Boolean {
        if (!aiConversationR2dbcRepository.existsById(conversationId)) return false

        return aiConversationR2dbcRepository.deleteById(conversationId).let { true }
    }

    /**
     * Проверить существование маппинга
     *
     * @param conversationId UUID разговора
     * @return true если маппинг существует
     */
    suspend fun exists(conversationId: String): Boolean = aiConversationR2dbcRepository.existsById(conversationId)

    /**
     * Сохранить общий разговор без привязки к товару
     *
     * @param userId ID пользователя (UUID string)
     * @param conversationId UUID разговора (PRIMARY KEY)
     * @return Сохраненная entity
     */
    suspend fun saveGeneralConversation(userId: String, conversationId: String): AiConversationEntity {
        val entity = AiConversationEntity(
            conversationId = conversationId,
            userId = userId,
            goodsId = null  // Общий разговор без привязки к товару
        )

        return aiConversationR2dbcRepository.save(entity)
    }
}
