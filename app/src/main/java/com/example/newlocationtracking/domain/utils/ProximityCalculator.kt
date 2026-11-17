package com.example.newlocationtracking.domain.utils

import com.example.newlocationtracking.domain.models.GeoPoint
import com.example.newlocationtracking.domain.models.NearbyPOIModel
import com.example.newlocationtracking.domain.models.POIModel
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object ProximityCalculator {

    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun getNearbyPOI(
        currentLocation: GeoPoint,
        pois: List<POIModel>,
        visibilityRadius: Double,
        limit: Int = 10
    ): List<NearbyPOIModel> {
        require(visibilityRadius >= 0.0) { "Visibility radius must not be negative" }
        require(limit >= 0) { "The maximum number of POIs must not be negative" }

        return pois.asSequence().map { poi ->
            NearbyPOIModel(
                poi = poi,
                distanceMeters = distanceMeters(currentLocation, poi.geoPoint)
            )
        }.filter {
            it.distanceMeters <= visibilityRadius
        }.sortedBy {
            it.distanceMeters
        }.take(limit).toList()
    }

    fun distanceMeters(geoPoint: GeoPoint, other: GeoPoint): Double {
        val a = calculateHelpValue(geoPoint, other).coerceIn(0.0, 1.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return c * EARTH_RADIUS_METERS
    }

    private fun calculateHelpValue(geoPoint: GeoPoint, other: GeoPoint): Double {
        val f1 = geoPoint.latitude * PI / 180
        val f2 = other.latitude * PI / 180
        val dL = (geoPoint.longitude - other.longitude) * PI / 180
        val dF = (geoPoint.latitude - other.latitude) * PI / 180
        return (sin(dF / 2)).pow(2) +
                (cos(f1) *
                        cos(f2) *
                        sin(dL / 2).pow(2))
    }
}
