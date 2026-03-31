package com.example.webflux.service

import com.example.webflux.domain.model.Goods
import com.example.webflux.repository.GoodsRepository
import org.springframework.stereotype.Service

@Service
class GoodsService(
    private val goodsRepository: GoodsRepository
) {
    suspend fun getAllGoods(): List<Goods> {
        return goodsRepository.findAll()
    }

    suspend fun getGoodsById(id: Long): Goods? {
        return goodsRepository.findById(id)
    }
}
