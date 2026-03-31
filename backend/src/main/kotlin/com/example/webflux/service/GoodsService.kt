package com.example.webflux.service

import com.example.webflux.domain.model.Goods
import com.example.webflux.domain.model.Category
import com.example.webflux.repository.GoodsRepository
import com.example.webflux.repository.CategoryRepository
import org.springframework.stereotype.Service

@Service
class GoodsService(
    private val goodsRepository: GoodsRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend fun getAllGoods(): List<Goods> {
        return goodsRepository.findAll()
    }

    suspend fun getGoodsById(id: Long): Goods? {
        return goodsRepository.findById(id)
    }

    suspend fun getCategoryForGoods(goods: Goods): Category? {
        return categoryRepository.findById(goods.categoryId)
    }
}
