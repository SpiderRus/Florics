package com.example.webflux.repository

import com.example.webflux.domain.model.GoodsType
import com.example.webflux.domain.model.GoodsTypeDocument
import com.example.webflux.mapper.GoodsTypeDocumentMapper
import com.example.webflux.repository.r2dbc.GoodsTypeDocumentR2dbcRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Repository

/**
 * Domain репозиторий для управления связями типов товаров с документами
 *
 * Предоставляет высокоуровневые операции для связывания документов AI Agent с типами товаров.
 * Без кэширования, т.к. связи меняются часто.
 */
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
        r2dbcRepository.findByGoodsType(goodsType.name)
            .map { GoodsTypeDocumentMapper.toModel(it) }

    /**
     * Найти все документы для нескольких типов товаров
     *
     * @param goodsTypes Список типов товаров для поиска
     * @return Flow документов
     */
    fun findByGoodsTypes(goodsTypes: List<GoodsType>): Flow<GoodsTypeDocument> =
        r2dbcRepository.findByGoodsTypeIn(goodsTypes.map { it.name }.toTypedArray())
            .map { GoodsTypeDocumentMapper.toModel(it) }

    /**
     * Найти конкретный документ по его ID
     *
     * @param documentId UUID документа
     * @return Документ если найден, null в противном случае
     */
    suspend fun findByDocumentId(documentId: String): GoodsTypeDocument? =
        r2dbcRepository.findById(documentId)
            ?.let { GoodsTypeDocumentMapper.toModel(it) }

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
