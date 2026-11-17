package com.example.newlocationtracking.domain.repository

import com.example.newlocationtracking.domain.models.LocationModel
import com.example.newlocationtracking.domain.utils.LocationResult
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    suspend fun getCurrentLocation(): LocationResult

    fun observeLocation(): Flow<LocationModel>
}
