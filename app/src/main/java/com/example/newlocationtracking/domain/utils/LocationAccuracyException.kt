package com.example.newlocationtracking.domain.utils

class LocationAccuracyUnavailableException : Exception()

class LocationAccuracyExceedsThresholdException(
    val accuracyMeters: Float,
    val thresholdMeters: Float
) : Exception()
