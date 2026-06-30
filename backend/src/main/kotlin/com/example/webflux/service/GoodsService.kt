package com.example.webflux.service

import com.example.webflux.controller.model.CreateGoodsRequest
import com.example.webflux.controller.model.UpdateGoodsRequest
import com.example.webflux.controller.model.PagedGoodsResponse
import com.example.webflux.controller.model.toGoodsDto
import com.example.webflux.domain.model.Goods
import com.example.webflux.domain.model.GoodsType
import com.example.webflux.domain.model.Category
import com.example.webflux.repository.GoodsRepository
import com.example.webflux.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GoodsService(
    private val goodsRepository: GoodsRepository,
    private val categoryRepository: CategoryRepository
) {
    fun getAllGoods(): Flow<Goods> {
        return goodsRepository.findAll()
    }

    suspend fun getGoodsById(id: String): Goods? {
        return goodsRepository.findById(id)
    }

    fun getGoodsByType(type: GoodsType): Flow<Goods> = goodsRepository.findByType(type)

    suspend fun getCategoryForGoods(goods: Goods): Category? {
        return categoryRepository.findById(goods.categoryId)
    }

    /**
     * Создание нового товара (только для администраторов)
     */
    suspend fun createGoods(request: CreateGoodsRequest): Goods {
        // Проверяем что категория существует
        val category = categoryRepository.findById(request.categoryId)
            ?: throw IllegalArgumentException("Категория с ID ${request.categoryId} не найдена")

        val newGoods = Goods(
            id = null,
            name = request.name,
            description = request.description,
            price = request.price,
            media = emptyList(), // По умолчанию пустой список, можно расширить позже
            categoryId = request.categoryId,
            difficulty = request.difficulty ?: "Средне",
            duration = request.duration,
            videoUrl = request.videoUrl,
            detailedDescription = request.detailedDescription,
            careInstructions = request.careInstructions
        )

        return goodsRepository.save(newGoods)
    }

    /**
     * Обновление существующего товара (только для администраторов)
     */
    suspend fun updateGoods(id: String, request: UpdateGoodsRequest): Goods? {
        val existing = goodsRepository.findById(id) ?: return null

        // Проверяем что категория существует
        val category = categoryRepository.findById(request.categoryId)
            ?: throw IllegalArgumentException("Категория с ID ${request.categoryId} не найдена")

        val updated = existing.copy(
            name = request.name,
            description = request.description,
            price = request.price,
            categoryId = request.categoryId,
            difficulty = request.difficulty ?: existing.difficulty,
            duration = request.duration,
            videoUrl = request.videoUrl,
            detailedDescription = request.detailedDescription,
            careInstructions = request.careInstructions
        )

        return goodsRepository.save(updated)
    }

    /**
     * Soft delete товара (только для администраторов)
     */
    suspend fun deleteGoods(id: String): Boolean {
        return goodsRepository.deleteById(id)
    }

    /**
     * Получить товары с пагинацией и сортировкой (только для администраторов)
     */
    suspend fun getGoodsPaged(
        page: Int,
        size: Int,
        sortBy: String,
        sortOrder: String
    ): PagedGoodsResponse {
        val (goods, totalElements) = goodsRepository.findAllPaged(page, size, sortBy, sortOrder)
        val totalPages = ((totalElements + size - 1) / size).toInt()

        // Загрузить категории для всех товаров
        val goodsDtos = goods.map { goodsItem: Goods -> goodsItem.toGoodsDto(getCategoryForGoods(goodsItem)) }

        return PagedGoodsResponse(
            content = goodsDtos,
            page = page,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages,
            hasNext = page < totalPages - 1,
            hasPrevious = page > 0
        )
    }

    /**
     * Серверные поиск/сортировка/пагинация по типу товара (для публичного каталога)
     */
    suspend fun searchGoodsByType(
        type: GoodsType,
        query: String?,
        sortBy: String,
        sortOrder: String,
        page: Int,
        size: Int
    ): PagedGoodsResponse {
        val (goods, totalElements) = goodsRepository.findByTypePaged(type, query, sortBy, sortOrder, page, size)
        val totalPages = if (size > 0) ((totalElements + size - 1) / size).toInt() else 0

        val goodsDtos = goods.map { goodsItem: Goods -> goodsItem.toGoodsDto(getCategoryForGoods(goodsItem)) }

        return PagedGoodsResponse(
            content = goodsDtos,
            page = page,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages,
            hasNext = page < totalPages - 1,
            hasPrevious = page > 0
        )
    }
}
