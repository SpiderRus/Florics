package com.example.webflux.service

import com.example.webflux.domain.model.Goods
import com.example.webflux.domain.model.GoodsType
import com.example.webflux.domain.model.Category
import com.example.webflux.repository.GoodsRepository
import com.example.webflux.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service

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

    suspend fun getGoodsByType(type: GoodsType): List<Goods> {
        return goodsRepository.findByType(type)
    }

    suspend fun getCategoryForGoods(goods: Goods): Category? {
        return categoryRepository.findById(goods.categoryId)
    }
}
