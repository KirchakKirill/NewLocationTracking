package com.example.newlocationtracking.domain.usecase

import com.example.newlocationtracking.domain.repository.LocationRepository
import com.example.newlocationtracking.domain.repository.POIRepository
import com.example.newlocationtracking.domain.utils.LocationOperationStatus
import com.example.newlocationtracking.domain.utils.LocationAccuracyExceedsThresholdException
import com.example.newlocationtracking.domain.utils.LocationAccuracyUnavailableException
import com.example.newlocationtracking.domain.utils.LocationResult
import com.example.newlocationtracking.domain.utils.toPOIModel

class AddPoiHere(
    private val locationRepository: LocationRepository,
    private val poiRepository: POIRepository
) {
    suspend fun invoke(maxAccuracyMeters: Float): LocationOperationStatus {
        return when (val locationResult = locationRepository.getCurrentLocation()) {
            is LocationResult.Success -> {
                val accuracy = locationResult.data.accuracy
                if (accuracy == null || !accuracy.isFinite() || accuracy <= 0f) {
                    return LocationOperationStatus.Failed(LocationAccuracyUnavailableException())
                }
                if (accuracy > maxAccuracyMeters) {
                    return LocationOperationStatus.Failed(
                        LocationAccuracyExceedsThresholdException(accuracy, maxAccuracyMeters)
                    )
                }

                val poi = locationResult.data.toPOIModel()
                val id = poiRepository.savePOI(poi)
                LocationOperationStatus.Success(data = id)
            }

            is LocationResult.Failed -> {
                LocationOperationStatus.Failed(locationResult.throwable)
            }

            LocationResult.Cancelled -> LocationOperationStatus.Failed(Exception())
        }
    }
}
