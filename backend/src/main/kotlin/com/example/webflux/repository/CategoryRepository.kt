package com.example.webflux.repository

import com.example.webflux.domain.model.Category
import com.example.webflux.domain.model.GoodsType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.springframework.stereotype.Repository

@Repository
class CategoryRepository {
    private val categories: MutableMap<String, Category> = mutableMapOf(
        "1" to Category(
            id = "1",
            name = "Лианы",
            type = GoodsType.PLANT
        ),
        "2" to Category(
            id = "2",
            name = "Декоративнолиственные",
            type = GoodsType.PLANT
        ),
        "3" to Category(
            id = "3",
            name = "Суккуленты",
            type = GoodsType.PLANT
        ),
        "4" to Category(
            id = "4",
            name = "Папоротники",
            type = GoodsType.PLANT
        ),
        "5" to Category(
            id = "5",
            name = "Цветущие",
            type = GoodsType.PLANT
        ),
        "6" to Category(
            id = "6",
            name = "Флорариум",
            type = GoodsType.TERRARIUM
        ),
        "7" to Category(
            id = "7",
            name = "Мастер-класс",
            type = GoodsType.COURSE
        )
    )

    fun findAll(): Flow<Category> = categories.values.asFlow()

    suspend fun findById(id: String): Category? = categories[id]
}
