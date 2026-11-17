package com.example.newlocationtracking.domain.models

data class POIModel(
    val id: Long = 0,
    val geoPoint: GeoPoint,
    val radius: Double = DEFAULT_RADIUS,
    val createdAt: Long,
    val accuracy: Float? = null
) {
    companion object {
        const val DEFAULT_RADIUS = 5.0
    }
}
