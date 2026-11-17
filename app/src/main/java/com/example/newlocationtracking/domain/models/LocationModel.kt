package com.example.newlocationtracking.domain.models

data class LocationModel(
    val geoPoint: GeoPoint,
    val accuracy: Float?,
    val createdAt: Long
)
