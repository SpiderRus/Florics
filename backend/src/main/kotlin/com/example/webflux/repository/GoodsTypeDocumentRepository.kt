package com.example.webflux.repository

import com.example.webflux.domain.model.GoodsType
import com.example.webflux.domain.model.GoodsTypeDocument
import com.example.webflux.entity.GoodsTypeDocumentEntity
import com.example.webflux.mapper.GoodsTypeDocumentMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

/**
 * Domain репозиторий для управления связями типов товаров с документами
 *
 * Предоставляет высокоуровневые операции для связывания документов AI Agent с типами товаров.
 * Без кэширования, т.к. связи меняются часто.
 */

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


@Repository
class GoodsTypeDocumentRepository(
    private val r2dbcRepository: GoodsTypeDocumentR2dbcRepository
) {
    /**
     * Сохранить новую связь типа товара с документом
     *
     * @param document Документ типа товара для сохранения
     * @return Сохраненный документ
     * @throws DataIntegrityViolationException если документ уже существует (нарушение PRIMARY KEY)
     */
    suspend fun save(document: GoodsTypeDocument): GoodsTypeDocument =
        GoodsTypeDocumentMapper.toModel(r2dbcRepository.save(GoodsTypeDocumentMapper.toEntity(document)))

    /**
     * Найти все документы для конкретного типа товара
     *
     * Возвращает документы, отсортированные по дате (новые первыми).
     *
     * @param goodsType Тип товара (PLANT, TERRARIUM, COURSE)
     * @return Flow документов для этого типа
     */
    fun findByGoodsType(goodsType: GoodsType): Flow<GoodsTypeDocument> =
        r2dbcRepository.findByGoodsType(goodsType.name).map { GoodsTypeDocumentMapper.toModel(it) }

    /**
     * Найти все документы для нескольких типов товаров
     *
     * @param goodsTypes Список типов товаров для поиска
     * @return Flow документов
     */
    fun findByGoodsTypes(goodsTypes: List<GoodsType>): Flow<GoodsTypeDocument> =
        r2dbcRepository.findByGoodsTypeIn(goodsTypes.map { it.name }.toTypedArray()).map { GoodsTypeDocumentMapper.toModel(it) }

    /**
     * Найти конкретный документ по его ID
     *
     * @param documentId UUID документа
     * @return Документ если найден, null в противном случае
     */
    suspend fun findByDocumentId(documentId: String): GoodsTypeDocument? =
        r2dbcRepository.findById(documentId)?.let { GoodsTypeDocumentMapper.toModel(it) }

    /**
     * Проверить существование документа
     *
     * @param documentId UUID документа
     * @return true если документ существует
     */
    suspend fun existsByDocumentId(documentId: String): Boolean =
        r2dbcRepository.existsByDocumentId(documentId)

    /**
     * Удалить связь документа с типом товара
     * Должен вызываться при удалении документа из AI Agent
     *
     * @param documentId UUID документа
     */
    suspend fun deleteByDocumentId(documentId: String) =
        r2dbcRepository.deleteByDocumentId(documentId)

    /**
     * Удалить все документы для конкретного типа товара
     * Полезно для массовых операций очистки
     *
     * @param goodsType Тип товара
     */
    suspend fun deleteByGoodsType(goodsType: GoodsType) =
        r2dbcRepository.deleteByGoodsType(goodsType.name)

    /**
     * Получить все ID документов для типа товара (для RAG запросов)
     *
     * @param goodsType Тип товара
     * @return Список ID документов
     */
    suspend fun getDocumentIds(goodsType: GoodsType): List<String> =
        findByGoodsType(goodsType).map { it.documentId }.toList()
}
