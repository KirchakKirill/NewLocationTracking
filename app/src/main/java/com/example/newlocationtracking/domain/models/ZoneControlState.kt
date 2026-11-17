package com.example.newlocationtracking.domain.models

/** UI-controlled zone state consumed by the zone transition use case. */
data class ZoneControlState(
    val activePoiId: Long?,
    val manualEntryRequired: Boolean
)
