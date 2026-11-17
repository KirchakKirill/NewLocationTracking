package com.example.newlocationtracking.domain.usecase

import com.example.newlocationtracking.domain.models.ARPlacementModel
import com.example.newlocationtracking.domain.repository.ARPlacementRepository
import kotlinx.coroutines.flow.Flow

class ObserveARPlacements(private val repository: ARPlacementRepository) {
    operator fun invoke(poiId: Long): Flow<List<ARPlacementModel>> =
        repository.observeForZone(poiId)
}
