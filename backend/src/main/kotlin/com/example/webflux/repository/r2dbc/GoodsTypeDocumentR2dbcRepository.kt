package com.example.webflux.repository.r2dbc

import com.example.webflux.entity.GoodsTypeDocumentEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

/**
 * R2DBC репозиторий для связей типов товаров с документами
 *
 * Предоставляет операции с базой данных для таблицы goods_type_documents.
 * Первичный ключ - document_id (каждый документ принадлежит ровно одному типу товара).
 */
@Repository
interface GoodsTypeDocumentR2dbcRepository : CoroutineCrudRepository<GoodsTypeDocumentEntity, String> {

    /**
     * Найти все документы для конкретного типа товара
     *
     * @param goodsType Тип товара (PLANT, TERRARIUM, COURSE)
     * @return Flow документов для этого типа, отсортированных по дате (новые первыми)
     */
    @Query("SELECT * FROM goods_type_documents WHERE goods_type = :goodsType ORDER BY created_at DESC")
    fun findByGoodsType(goodsType: String): Flow<GoodsTypeDocumentEntity>

    /**
     * Найти все документы для нескольких типов товаров
     *
     * @param goodsTypes Массив типов товаров для поиска
     * @return Flow документов, отсортированных по типу и дате создания
     */
    @Query("SELECT * FROM goods_type_documents WHERE goods_type = ANY(:goodsTypes) ORDER BY goods_type, created_at DESC")
    fun findByGoodsTypeIn(goodsTypes: Array<String>): Flow<GoodsTypeDocumentEntity>

    /**
     * Проверить существование документа по его ID
     *
     * @param documentId UUID документа
     * @return true если документ существует
     */
    @Query("SELECT EXISTS(SELECT 1 FROM goods_type_documents WHERE document_id = :documentId)")
    suspend fun existsByDocumentId(documentId: String): Boolean

    /**
     * Удалить связь документа с типом товара
     * Должен вызываться при удалении документа из AI Agent
     *
     * @param documentId UUID документа для удаления
     */
    @Query("DELETE FROM goods_type_documents WHERE document_id = :documentId")
    suspend fun deleteByDocumentId(documentId: String)

    /**
     * Удалить все документы для конкретного типа товара
     * Полезно для массовых операций очистки
     *
     * @param goodsType Тип товара
     */
    @Query("DELETE FROM goods_type_documents WHERE goods_type = :goodsType")
    suspend fun deleteByGoodsType(goodsType: String)
}
