package com.example.webflux.service

import com.example.webflux.domain.model.Category
import com.example.webflux.repository.CategoryRepository
import org.springframework.stereotype.Service

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository
) {
    suspend fun getAllCategories(): List<Category> {
        return categoryRepository.findAll()
    }

    suspend fun getCategoryById(id: String): Category? {
        return categoryRepository.findById(id)
    }
}
