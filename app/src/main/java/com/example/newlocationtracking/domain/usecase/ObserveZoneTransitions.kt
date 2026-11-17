package com.example.newlocationtracking.domain.usecase

import com.example.newlocationtracking.domain.models.POIModel
import com.example.newlocationtracking.domain.models.ZoneControlState
import com.example.newlocationtracking.domain.models.ZoneTransition
import com.example.newlocationtracking.domain.repository.LocationRepository
import com.example.newlocationtracking.domain.repository.POIRepository
import com.example.newlocationtracking.domain.utils.ProximityCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

class ObserveZoneTransitions(
    private val locationRepository: LocationRepository,
    private val poiRepository: POIRepository
) {
    companion object {
        private const val EXIT_HYSTERESIS_METERS = 2.0
        private const val SWITCH_ADVANTAGE_METERS = 1.0
        private const val REQUIRED_ENTRY_CONFIRMATION_FIXES = 2
        private const val REQUIRED_SWITCH_CONFIRMATION_FIXES = 2
        private const val REQUIRED_EXIT_CONFIRMATION_FIXES = 2
    }

    private data class ZoneDistance(
        val poi: POIModel,
        val distanceMeters: Double
    )

    private data class ZoneSnapshot(
        val locationTimestamp: Long,
        val controlState: ZoneControlState,
        val zones: List<ZoneDistance>
    )

    fun invoke(controlState: Flow<ZoneControlState>): Flow<ZoneTransition> = flow {
        var activePoi: POIModel? = null
        var pendingEntryId: Long? = null
        var pendingEntryCount = 0
        var pendingExitCount = 0
        var pendingSwitchId: Long? = null
        var pendingSwitchCount = 0
        var lastProcessedLocationTimestamp: Long? = null
        var lastControlState: ZoneControlState? = null

        combine(
            poiRepository.getAll(),
            locationRepository.observeLocation(),
            controlState
        ) { pois, location, currentControlState ->
            ZoneSnapshot(
                locationTimestamp = location.createdAt,
                controlState = currentControlState,
                zones = pois.map { poi ->
                    ZoneDistance(
                        poi = poi,
                        distanceMeters = ProximityCalculator.distanceMeters(
                            location.geoPoint,
                            poi.geoPoint
                        )
                    )
                }
            )
        }.collect { measuredZones ->
            val zones = measuredZones.zones
            val isNewLocationFix =
                lastProcessedLocationTimestamp != measuredZones.locationTimestamp
            val controlChanged = lastControlState != measuredZones.controlState
            lastControlState = measuredZones.controlState

            if (controlChanged) {
                val requestedPoi = measuredZones.controlState.activePoiId?.let { requestedId ->
                    zones.firstOrNull { it.poi.id == requestedId }?.poi
                }
                if (requestedPoi?.id != activePoi?.id) {
                    activePoi = requestedPoi
                    pendingEntryId = null
                    pendingEntryCount = 0
                    pendingExitCount = 0
                    pendingSwitchId = null
                    pendingSwitchCount = 0
                } else if (requestedPoi != null) {
                    activePoi = requestedPoi
                }
            }
            
            if (!isNewLocationFix) return@collect
            lastProcessedLocationTimestamp = measuredZones.locationTimestamp

            val previousCurrent = activePoi?.let { active ->
                zones.firstOrNull { it.poi.id == active.id }
            }

            if (activePoi != null && previousCurrent == null) {
                emit(ZoneTransition.Exited(activePoi!!))
                activePoi = null
                pendingExitCount = 0
                pendingSwitchId = null
                pendingSwitchCount = 0
                pendingEntryId = null
                pendingEntryCount = 0
            }

            val nearestInside = zones
                .asSequence()
                .filter { it.distanceMeters <= it.poi.radius }
                .minByOrNull { it.distanceMeters }

            if (activePoi == null) {
                if (measuredZones.controlState.manualEntryRequired) {
                    pendingEntryId = null
                    pendingEntryCount = 0
                    return@collect
                }

                if (nearestInside == null) {
                    pendingEntryId = null
                    pendingEntryCount = 0
                    return@collect
                }

                if (pendingEntryId == nearestInside.poi.id) {
                    pendingEntryCount++
                } else {
                    pendingEntryId = nearestInside.poi.id
                    pendingEntryCount = 1
                }

                if (pendingEntryCount >= REQUIRED_ENTRY_CONFIRMATION_FIXES) {
                    activePoi = nearestInside.poi
                    emit(ZoneTransition.Entered(nearestInside.poi))
                    pendingEntryId = null
                    pendingEntryCount = 0
                }
                return@collect
            }

            val current = zones.firstOrNull { it.poi.id == activePoi!!.id }
                ?: return@collect
            val challenger = nearestInside?.takeIf {
                it.poi.id != current.poi.id && !measuredZones.controlState.manualEntryRequired
            }
            val currentOutsideRadius = current.distanceMeters > current.poi.radius
            val currentBeyondExitBoundary =
                current.distanceMeters > current.poi.radius + EXIT_HYSTERESIS_METERS

            if (challenger != null &&
                (currentOutsideRadius ||
                    challenger.distanceMeters + SWITCH_ADVANTAGE_METERS < current.distanceMeters)
            ) {
                if (pendingSwitchId == challenger.poi.id) {
                    pendingSwitchCount++
                } else {
                    pendingSwitchId = challenger.poi.id
                    pendingSwitchCount = 1
                }
                pendingExitCount = 0

                if (pendingSwitchCount >= REQUIRED_SWITCH_CONFIRMATION_FIXES) {
                    emit(ZoneTransition.Switched(current.poi, challenger.poi))
                    activePoi = challenger.poi
                    pendingSwitchId = null
                    pendingSwitchCount = 0
                }
            } else if (currentBeyondExitBoundary) {
                pendingExitCount++
                pendingSwitchId = null
                pendingSwitchCount = 0
                if (pendingExitCount >= REQUIRED_EXIT_CONFIRMATION_FIXES) {
                    emit(ZoneTransition.Exited(current.poi))
                    activePoi = null
                    pendingExitCount = 0
                    pendingEntryId = null
                    pendingEntryCount = 0
                }
            } else {
                pendingExitCount = 0
                pendingSwitchId = null
                pendingSwitchCount = 0
            }
        }
    }
}
