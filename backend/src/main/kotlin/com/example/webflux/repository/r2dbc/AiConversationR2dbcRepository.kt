package com.example.webflux.repository.r2dbc

import com.example.webflux.entity.AiConversationEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AiConversationR2dbcRepository : CoroutineCrudRepository<AiConversationEntity, String> {

    @Query("SELECT * FROM ai_conversations WHERE user_id = :userId")
    fun findByUserId(userId: String): Flow<AiConversationEntity>

    @Query("SELECT * FROM ai_conversations WHERE user_id = :userId AND goods_id = :goodsId")
    suspend fun findByUserIdAndGoodsId(userId: String, goodsId: String): AiConversationEntity?

    @Query("SELECT * FROM ai_conversations WHERE conversation_id = :conversationId")
    suspend fun findByConversationId(conversationId: String): AiConversationEntity?

    @Query("DELETE FROM ai_conversations WHERE user_id = :userId AND goods_id = :goodsId")
    suspend fun deleteByUserIdAndGoodsId(userId: String, goodsId: String)

    @Query("DELETE FROM ai_conversations WHERE conversation_id = :conversationId")
    suspend fun deleteByConversationId(conversationId: String)
}
