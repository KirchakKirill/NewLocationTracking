package com.example.newlocationtracking.data.repository

import android.annotation.SuppressLint
import com.example.newlocationtracking.data.location.LocationManager
import com.example.newlocationtracking.domain.models.LocationModel
import com.example.newlocationtracking.domain.utils.LocationResult
import com.example.newlocationtracking.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow

class LocationRepository(
    private val locationManager: LocationManager
) : LocationRepository {
    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): LocationResult =
        locationManager.getPoI(LocationManager.TypeLocation.CURRENT)

    override fun observeLocation(): Flow<LocationModel> = locationManager.observeLocation()
}
