package com.example.newlocationtracking.domain.usecase

import com.example.newlocationtracking.domain.models.NearbyPOISnapshot
import com.example.newlocationtracking.domain.repository.LocationRepository
import com.example.newlocationtracking.domain.repository.POIRepository
import com.example.newlocationtracking.domain.utils.ProximityCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveNearbyPOI(
    private val locationRepository: LocationRepository,
    private val poiRepository: POIRepository
) {
    fun invoke(
        visibilityRadius: Double,
        limit: Int = 10
    ): Flow<NearbyPOISnapshot> =
        combine(poiRepository.getAll(), locationRepository.observeLocation()) { pois, location ->
            NearbyPOISnapshot(
                nearbyPOIs = ProximityCalculator.getNearbyPOI(
                    currentLocation = location.geoPoint,
                    pois = pois,
                    visibilityRadius = visibilityRadius,
                    limit = limit
                ),
                currentLocationAccuracyMeters = location.accuracy
            )
        }
}
