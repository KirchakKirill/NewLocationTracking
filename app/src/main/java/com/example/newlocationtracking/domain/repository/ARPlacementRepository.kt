package com.example.newlocationtracking.domain.repository

import com.example.newlocationtracking.domain.models.ARPlacementModel
import kotlinx.coroutines.flow.Flow

interface ARPlacementRepository {
    fun observeForZone(poiId: Long): Flow<List<ARPlacementModel>>
    suspend fun save(placement: ARPlacementModel): Long
}
