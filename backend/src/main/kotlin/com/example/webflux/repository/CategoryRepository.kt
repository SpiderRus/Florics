package com.example.webflux.repository

import com.example.webflux.domain.model.Category
import com.example.webflux.domain.model.GoodsType
import org.springframework.stereotype.Repository

@Repository
class CategoryRepository {
    private val categories: MutableMap<String, Category> = mutableMapOf(
        "1" to Category(
            id = "1",
            name = "Лианы",
            type = GoodsType.PHYSICAL
        ),
        "2" to Category(
            id = "2",
            name = "Декоративнолиственные",
            type = GoodsType.PHYSICAL
        ),
        "3" to Category(
            id = "3",
            name = "Суккуленты",
            type = GoodsType.PHYSICAL
        ),
        "4" to Category(
            id = "4",
            name = "Папоротники",
            type = GoodsType.PHYSICAL
        ),
        "5" to Category(
            id = "5",
            name = "Цветущие",
            type = GoodsType.PHYSICAL
        ),
        "6" to Category(
            id = "6",
            name = "Флорариум",
            type = GoodsType.PHYSICAL
        ),
        "7" to Category(
            id = "7",
            name = "Мастер-класс",
            type = GoodsType.COURSE
        )
    )

    suspend fun findAll(): List<Category> {
        return categories.values.toList()
    }

    suspend fun findById(id: String): Category? {
        return categories[id]
    }
}
