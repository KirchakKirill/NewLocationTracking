package com.example.newlocationtracking.domain.repository

import com.example.newlocationtracking.domain.models.POIModel
import kotlinx.coroutines.flow.Flow

interface POIRepository {
    fun getAll(): Flow<List<POIModel>>
    suspend fun savePOI(poiModel: POIModel): Long
}