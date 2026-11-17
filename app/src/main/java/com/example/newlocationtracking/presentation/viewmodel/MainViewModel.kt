package com.example.newlocationtracking.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class MainViewModel(
    private val addPoiHere: AddPoiHere,
    private val observeZoneTransitions: ObserveZoneTransitions,
    private val observeNearbyPOI: ObserveNearbyPOI,
    private val observeARPlacements: ObserveARPlacements,
    private val saveARPlacement: SaveARPlacement
) : ViewModel() {

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
                    .catch { error ->
                        _message.value =
                            "Не удалось получить ближайшие зоны: ${error.localizedMessage ?: "ошибка геолокации"}"
                    }
                    .collect { snapshot: NearbyPOISnapshot ->
                        _nearbyPOIs.value = snapshot.nearbyPOIs
                        _currentLocationAccuracyMeters.value = snapshot.currentLocationAccuracyMeters
                    }
            }

            launch {
                observeZoneTransitions.invoke(zoneControlState)
                    .catch { error ->
                        _message.value =
                            "Не удалось отслеживать зоны: ${error.localizedMessage ?: "ошибка геолокации"}"
                    }
                    .collect { transition ->
                        when (transition) {
                            is ZoneTransition.Entered -> {
                                _activeZones.value = listOf(transition.poi)
                                _message.value =
                                    "Вы в зоне #${transition.poi.id}. Наведитесь на горизонтальную плоскость."
                            }

                            is ZoneTransition.Exited -> {
                                _activeZones.update { current ->
                                    current.filterNot { it.id == transition.poi.id }
                                }
                                _message.value = "Вы вышли из зоны #${transition.poi.id}."
                            }

                            is ZoneTransition.Switched -> {
                                _activeZones.value = listOf(transition.poi)
                                _message.value = "Вы в зоне #${transition.poi.id} — она ближе."
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
            "Автоматический вход включён. Для входа нужны два подтверждения GPS."
        } else {
            "Ручной вход включён. Нажмите на подсвеченную зону."
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
        _message.value = "Ручной вход в зону #${eligibleZone.poi.id}. Наведитесь на горизонтальную плоскость."
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
            "Вы вышли из зоны #${activeZone.id}. Выберите подсвеченную зону или включите автоматический вход."
        } else {
            "Вы вышли из зоны #${activeZone.id}."
        }
    }

    fun addPoiHere() {
        if (_isAddingZone.value) return

        val accuracyLimit = _maxAccuracyForAddingZoneMeters.value
        val currentAccuracy = _currentLocationAccuracyMeters.value
        if (!isAccuracyAcceptable(currentAccuracy, accuracyLimit)) {
            _message.value = "Добавление доступно, когда точность положения будет не хуже " +
                "±${accuracyLimit.toInt()} м."
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
                            "Зона #$id добавлена. Радиус: ${POIModel.DEFAULT_RADIUS.toInt()} м."
                        } else {
                            "Зона добавлена."
                        }
                    }

                    is LocationOperationStatus.Failed -> {
                        _message.value = result.throwable?.localizedMessage
                            ?: "Не удалось добавить зону. Проверьте разрешение на геолокацию."
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _message.value = error.localizedMessage ?: "Не удалось сохранить зону."
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
                _message.value = "Куб сохранён для зоны #${placement.poiId}."
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _message.value = error.localizedMessage ?: "Не удалось сохранить куб."
            }
        }
    }
}
