package com.example.newlocationtracking.presentation.viewmodel

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.newlocationtracking.R
import com.example.newlocationtracking.domain.models.ARPlacementModel
import com.example.newlocationtracking.domain.models.NearbyPOIModel
import com.example.newlocationtracking.domain.models.NearbyPOISnapshot
import com.example.newlocationtracking.domain.models.POIModel
import com.example.newlocationtracking.domain.models.ZoneControlState
import com.example.newlocationtracking.domain.models.ZoneTransition
import com.example.newlocationtracking.domain.usecase.AddPoiHere
import com.example.newlocationtracking.domain.usecase.ObserveARPlacements
import com.example.newlocationtracking.domain.usecase.ObserveNearbyPOI
import com.example.newlocationtracking.domain.usecase.ObserveZoneTransitions
import com.example.newlocationtracking.domain.usecase.SaveARPlacement
import com.example.newlocationtracking.domain.utils.LocationOperationStatus
import com.example.newlocationtracking.domain.utils.LocationAccuracyExceedsThresholdException
import com.example.newlocationtracking.domain.utils.LocationAccuracyUnavailableException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class MainViewModel(
    application: Application,
    private val addPoiHere: AddPoiHere,
    private val observeZoneTransitions: ObserveZoneTransitions,
    private val observeNearbyPOI: ObserveNearbyPOI,
    private val observeARPlacements: ObserveARPlacements,
    private val saveARPlacement: SaveARPlacement
) : AndroidViewModel(application) {

    companion object {
        const val NEARBY_VISIBILITY_RADIUS_METERS = 100.0
        const val MAX_NEARBY_POIS = 10
    }

    private val _nearbyPOIs = MutableStateFlow<List<NearbyPOIModel>>(emptyList())
    val nearbyPOIs: StateFlow<List<NearbyPOIModel>> = _nearbyPOIs.asStateFlow()

    private val _currentLocationAccuracyMeters = MutableStateFlow<Float?>(null)
    val currentLocationAccuracyMeters: StateFlow<Float?> =
        _currentLocationAccuracyMeters.asStateFlow()

    private val _maxAccuracyForAddingZoneMeters = MutableStateFlow(5f)
    val maxAccuracyForAddingZoneMeters: StateFlow<Float> =
        _maxAccuracyForAddingZoneMeters.asStateFlow()

    private val _activeZones = MutableStateFlow<List<POIModel>>(emptyList())
    val activeZones: StateFlow<List<POIModel>> = _activeZones.asStateFlow()

    private val _manualEntryMode = MutableStateFlow(false)
    val manualEntryMode: StateFlow<Boolean> = _manualEntryMode.asStateFlow()

    private val _isAddingZone = MutableStateFlow(false)
    val isAddingZone: StateFlow<Boolean> = _isAddingZone.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var locationMonitoringJob: Job? = null

    fun startLocationMonitoring() {
        if (locationMonitoringJob?.isActive == true) return

        val zoneControlState = combine(_activeZones, _manualEntryMode) { activeZones, manualEntryMode ->
            ZoneControlState(
                activePoiId = activeZones.firstOrNull()?.id,
                manualEntryRequired = manualEntryMode
            )
        }

        locationMonitoringJob = viewModelScope.launch(Dispatchers.IO) {
            launch {
                observeNearbyPOI.invoke(
                    visibilityRadius = NEARBY_VISIBILITY_RADIUS_METERS,
                    limit = MAX_NEARBY_POIS
                )
                    .catch { _ ->
                        _message.value = resourceString(R.string.error_nearby_zones)
                    }
                    .collect { snapshot: NearbyPOISnapshot ->
                        _nearbyPOIs.value = snapshot.nearbyPOIs
                        _currentLocationAccuracyMeters.value = snapshot.currentLocationAccuracyMeters
                    }
            }

            launch {
                observeZoneTransitions.invoke(zoneControlState)
                    .catch { _ ->
                        _message.value = resourceString(R.string.error_zone_tracking)
                    }
                    .collect { transition ->
                        when (transition) {
                            is ZoneTransition.Entered -> {
                                _activeZones.value = listOf(transition.poi)
                                _message.value = resourceString(
                                    R.string.zone_entered,
                                    transition.poi.id
                                )
                            }

                            is ZoneTransition.Exited -> {
                                _activeZones.update { current ->
                                    current.filterNot { it.id == transition.poi.id }
                                }
                                _message.value = resourceString(
                                    R.string.zone_exited,
                                    transition.poi.id
                                )
                            }

                            is ZoneTransition.Switched -> {
                                _activeZones.value = listOf(transition.poi)
                                _message.value = resourceString(
                                    R.string.zone_switched,
                                    transition.poi.id
                                )
                            }
                        }
                    }
            }
        }
    }

    fun stopLocationMonitoring() {
        locationMonitoringJob?.cancel()
        locationMonitoringJob = null
        _activeZones.value = emptyList()
        _currentLocationAccuracyMeters.value = null
    }

    fun setAutomaticEntryEnabled(enabled: Boolean) {
        _manualEntryMode.value = !enabled
        _message.value = if (enabled) {
            resourceString(R.string.automatic_entry_enabled)
        } else {
            resourceString(R.string.manual_entry_enabled)
        }
    }

    fun setMaxAccuracyForAddingZone(meters: Float) {
        _maxAccuracyForAddingZoneMeters.value = meters.coerceIn(1f, 10f)
    }

    fun enterZoneManually(poiId: Long) {
        if (!_manualEntryMode.value) return

        val eligibleZone = _nearbyPOIs.value.firstOrNull { nearby ->
            nearby.poi.id == poiId && nearby.distanceMeters <= nearby.poi.radius
        } ?: return

        _activeZones.value = listOf(eligibleZone.poi)
        _message.value = resourceString(R.string.zone_manually_entered, eligibleZone.poi.id)
    }

    fun exitZoneManually() {
        val activeZone = _activeZones.value.firstOrNull() ?: return
        val stillInsideAnyZone = _nearbyPOIs.value.any { nearby ->
            nearby.distanceMeters <= nearby.poi.radius
        }

        if (stillInsideAnyZone) {
            _manualEntryMode.value = true
        }
        _activeZones.value = emptyList()
        _message.value = if (stillInsideAnyZone) {
            resourceString(R.string.zone_manually_exited_inside, activeZone.id)
        } else {
            resourceString(R.string.zone_manually_exited, activeZone.id)
        }
    }

    fun addPoiHere() {
        if (_isAddingZone.value) return

        val accuracyLimit = _maxAccuracyForAddingZoneMeters.value
        val currentAccuracy = _currentLocationAccuracyMeters.value
        if (!isAccuracyAcceptable(currentAccuracy, accuracyLimit)) {
            _message.value = resourceString(
                R.string.add_zone_accuracy_required,
                accuracyLimit.roundToInt()
            )
            return
        }

        _isAddingZone.value = true

        viewModelScope.launch {
            try {
                when (val result = withContext(Dispatchers.IO) {
                    addPoiHere.invoke(maxAccuracyMeters = accuracyLimit)
                }) {
                    is LocationOperationStatus.Success -> {
                        val id = (result.data as? Number)?.toLong()
                        _message.value = if (id != null) {
                            resourceString(
                                R.string.zone_added_with_radius,
                                id,
                                POIModel.DEFAULT_RADIUS.roundToInt()
                            )
                        } else {
                            resourceString(R.string.zone_added)
                        }
                    }

                    is LocationOperationStatus.Failed -> {
                        _message.value = when (val error = result.throwable) {
                            is LocationAccuracyUnavailableException ->
                                resourceString(R.string.error_accuracy_unavailable)
                            is LocationAccuracyExceedsThresholdException -> resourceString(
                                R.string.error_captured_accuracy_too_low,
                                error.accuracyMeters.roundToInt(),
                                error.thresholdMeters.roundToInt()
                            )
                            is SecurityException -> resourceString(R.string.error_location_permission)
                            else -> resourceString(R.string.error_add_zone)
                        }
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _message.value = resourceString(R.string.error_save_zone)
            } finally {
                _isAddingZone.value = false
            }
        }
    }

    fun showMessage(message: String) {
        _message.value = message
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun isAccuracyAcceptable(accuracyMeters: Float?, maxAccuracyMeters: Float): Boolean =
        accuracyMeters != null &&
            accuracyMeters.isFinite() &&
            accuracyMeters > 0f &&
            accuracyMeters <= maxAccuracyMeters

    fun observePlacementsForZone(poiId: Long): Flow<List<ARPlacementModel>> =
        observeARPlacements(poiId)

    fun savePlacement(placement: ARPlacementModel) {
        if (_activeZones.value.none { it.id == placement.poiId }) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                saveARPlacement(placement)
                _message.value = resourceString(R.string.placement_saved, placement.poiId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _message.value = resourceString(R.string.error_save_placement)
            }
        }
    }

    private fun resourceString(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)
}
