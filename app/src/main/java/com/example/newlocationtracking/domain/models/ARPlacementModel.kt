package com.example.newlocationtracking.domain.models

data class ARPlacementModel(
    val id: Long = 0,
    val poiId: Long,
    val x: Float,
    val y: Float,
    val z: Float,
    val qx: Float,
    val qy: Float,
    val qz: Float,
    val qw: Float,
    val createdAt: Long = System.currentTimeMillis()
)
