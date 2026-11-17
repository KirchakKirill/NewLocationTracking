package com.example.newlocationtracking.domain.usecase

import com.example.newlocationtracking.domain.models.ARPlacementModel
import com.example.newlocationtracking.domain.repository.ARPlacementRepository

class SaveARPlacement(private val repository: ARPlacementRepository) {
    suspend operator fun invoke(placement: ARPlacementModel): Long = repository.save(placement)
}
