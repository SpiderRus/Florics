package com.example.webflux.service

import com.example.webflux.controller.model.Plant
import com.example.webflux.repository.PlantRepository
import org.springframework.stereotype.Service

@Service
class PlantService(
    private val plantRepository: PlantRepository
) {
    suspend fun getAllPlants(): List<Plant> {
        return plantRepository.findAll()
    }

    suspend fun getPlantById(id: Long): Plant? {
        return plantRepository.findById(id)
    }
}
