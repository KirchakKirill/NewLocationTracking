package com.example.newlocationtracking.domain.models

data class NearbyPOISnapshot(
    val nearbyPOIs: List<NearbyPOIModel>,
    val currentLocationAccuracyMeters: Float?
)
