package com.example.newlocationtracking.domain.utils

import com.example.newlocationtracking.domain.models.LocationModel

sealed class LocationResult {
    data class Success(val data: LocationModel) : LocationResult()
    data class Cancelled(val message: String) : LocationResult()
    data class Failed(val throwable: Throwable?) : LocationResult()
}

sealed class LocationOperationStatus {
    data class Success(val data: Any) : LocationOperationStatus()
    data class Failed(val throwable: Throwable?) : LocationOperationStatus()
}