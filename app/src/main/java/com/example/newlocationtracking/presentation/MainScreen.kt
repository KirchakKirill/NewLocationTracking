package com.example.newlocationtracking.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.newlocationtracking.R
import com.example.newlocationtracking.domain.models.NearbyPOIModel
import com.example.newlocationtracking.presentation.viewmodel.MainViewModel
import kotlin.math.roundToInt

@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    locationPermissionGranted: Boolean,
    cameraPermissionGranted: Boolean,
    onAddZoneClick: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nearbyZones by mainViewModel.nearbyPOIs.collectAsStateWithLifecycle()
    val currentLocationAccuracy by mainViewModel.currentLocationAccuracyMeters.collectAsStateWithLifecycle()
    val maxAccuracyForAddingZone by mainViewModel.maxAccuracyForAddingZoneMeters.collectAsStateWithLifecycle()
    val activeZones by mainViewModel.activeZones.collectAsStateWithLifecycle()
    val manualEntryMode by mainViewModel.manualEntryMode.collectAsStateWithLifecycle()
    val isAddingZone by mainViewModel.isAddingZone.collectAsStateWithLifecycle()
    val message by mainViewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentAccuracyIsAcceptable = currentLocationAccuracy?.let { accuracy ->
        accuracy.isFinite() && accuracy > 0f && accuracy <= maxAccuracyForAddingZone
    } == true
    val currentAccuracyLabel = if (currentAccuracyIsAvailable(currentLocationAccuracy)) {
        stringResource(R.string.accuracy_value, currentLocationAccuracy!!.roundToInt())
    } else {
        stringResource(R.string.accuracy_unknown)
    }

    LaunchedEffect(message) {
        val currentMessage = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(currentMessage)
        mainViewModel.clearMessage()
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        val activeZone = activeZones.firstOrNull()
        if (activeZone != null) {
            val placementsFlow = remember(mainViewModel, activeZone.id) {
                mainViewModel.observePlacementsForZone(activeZone.id)
            }
            val placements by placementsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

            if (cameraPermissionGranted) {
                ARZoneScreen(
                    zoneId = activeZone.id,
                    placements = placements,
                    onPlaceCube = mainViewModel::savePlacement,
                    onExitZone = mainViewModel::exitZoneManually,
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                )
            } else {
                CameraPermissionRequired(
                    onRequestPermission = onRequestCameraPermission,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(stringResource(R.string.zones_title), style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.zones_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(
                            R.string.add_zone_accuracy_threshold,
                            maxAccuracyForAddingZone.roundToInt()
                        ),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Slider(
                        value = maxAccuracyForAddingZone,
                        onValueChange = mainViewModel::setMaxAccuracyForAddingZone,
                        valueRange = 1f..10f,
                        steps = 8,
                        enabled = !isAddingZone
                    )
                    Text(
                        text = when {
                            !locationPermissionGranted ->
                                stringResource(R.string.accuracy_permission_needed)
                            !currentAccuracyIsAvailable(currentLocationAccuracy) ->
                                stringResource(R.string.accuracy_waiting)
                            !currentAccuracyIsAcceptable ->
                                stringResource(
                                    R.string.accuracy_below_threshold,
                                    currentAccuracyLabel,
                                    maxAccuracyForAddingZone.roundToInt()
                                )
                            else ->
                                stringResource(R.string.accuracy_sufficient)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onAddZoneClick,
                        enabled = !isAddingZone &&
                            (!locationPermissionGranted || currentAccuracyIsAcceptable),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isAddingZone) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.getting_coordinates))
                        } else {
                            Text(stringResource(R.string.add_zone_here))
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(
                                    if (manualEntryMode) R.string.manual_entry_title
                                    else R.string.automatic_entry_title
                                ),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                if (manualEntryMode) {
                                    stringResource(R.string.manual_entry_description)
                                } else {
                                    stringResource(R.string.automatic_entry_description)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = !manualEntryMode,
                            onCheckedChange = mainViewModel::setAutomaticEntryEnabled
                        )
                    }

                    if (!locationPermissionGranted) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.error_location_permission),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.nearby_zones_title), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(
                            R.string.visibility_radius,
                            MainViewModel.NEARBY_VISIBILITY_RADIUS_METERS.roundToInt()
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = currentLocationAccuracy
                        ?.takeIf { it.isFinite() && it > 0f }
                        ?.let { stringResource(R.string.current_accuracy, it.roundToInt()) }
                        ?: stringResource(R.string.current_accuracy_unknown),
                    modifier = Modifier.padding(horizontal = 20.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                if (nearbyZones.isEmpty()) {
                    Text(
                        text = if (!locationPermissionGranted) {
                            stringResource(R.string.nearby_zones_permission_needed)
                        } else {
                            stringResource(R.string.nearby_zones_empty)
                        },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(nearbyZones, key = { it.poi.id }) { zone ->
                            val isInside = zone.distanceMeters <= zone.poi.radius
                            val canEnterManually = manualEntryMode && isInside
                            NearbyZoneCard(
                                zone = zone,
                                isInside = isInside,
                                canEnterManually = canEnterManually,
                                onClick = { mainViewModel.enterZoneManually(zone.poi.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NearbyZoneCard(
    zone: NearbyPOIModel,
    isInside: Boolean,
    canEnterManually: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canEnterManually, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isInside) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.zone_title, zone.poi.id),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    if (zone.distanceMeters < 1_000) {
                        stringResource(R.string.distance_meters, zone.distanceMeters.roundToInt())
                    } else {
                        stringResource(R.string.distance_kilometers, zone.distanceMeters / 1_000.0)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.zone_radius, zone.poi.radius.roundToInt()),
                style = MaterialTheme.typography.bodyMedium
            )
            if (isInside) {
                Text(
                    if (canEnterManually) {
                        stringResource(R.string.zone_inside_tap_to_enter)
                    } else {
                        stringResource(R.string.zone_inside_by_coordinates)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                stringResource(
                    R.string.zone_coordinates,
                    zone.poi.geoPoint.longitude.toString(),
                    zone.poi.geoPoint.latitude.toString()
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = zone.poi.accuracy
                    ?.takeIf { it.isFinite() && it > 0f }
                    ?.let { stringResource(R.string.zone_creation_accuracy, it.roundToInt()) }
                    ?: stringResource(R.string.zone_creation_accuracy_unknown),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun currentAccuracyIsAvailable(accuracyMeters: Float?): Boolean =
    accuracyMeters != null && accuracyMeters.isFinite() && accuracyMeters > 0f
