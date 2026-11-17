package com.example.newlocationtracking.domain.models

data class NearbyPOIModel(
    val poi: POIModel,
    val distanceMeters: Double
)
