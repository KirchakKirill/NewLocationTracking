package com.example.newlocationtracking.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.example.newlocationtracking.domain.utils.LocationResult
import com.example.newlocationtracking.domain.models.GeoPoint
import com.example.newlocationtracking.domain.models.LocationModel
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationManager(private val context: Context) {
    private var fusedLocationClient: FusedLocationProviderClient? = null

    fun init() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun getPoI(type: TypeLocation): LocationResult {
        val client = fusedLocationClient ?: LocationServices
            .getFusedLocationProviderClient(context)
            .also { fusedLocationClient = it }

        val cancellationTokenSource = if (type == TypeLocation.CURRENT) {
            CancellationTokenSource()
        } else {
            null
        }

        val task = try {
            when (type) {
                TypeLocation.LAST -> client.lastLocation
                TypeLocation.CURRENT -> {
                    client.getCurrentLocation(
                        CurrentLocationRequest.Builder()
                            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                            .setMaxUpdateAgeMillis(1_000L)
                            .setGranularity(Granularity.GRANULARITY_FINE)
                            .setDurationMillis(30_000L)
                            .build(),
                        cancellationTokenSource!!.token
                    )
                }
            }
        } catch (e: SecurityException) {
            return LocationResult.Failed(e)
        }

        return suspendCancellableCoroutine { continuation ->
            cancellationTokenSource?.let { source ->
                continuation.invokeOnCancellation { source.cancel() }
            }

            task.addOnCompleteListener { completedTask ->
                if (!continuation.isActive) return@addOnCompleteListener

                val result = when {
                    completedTask.isCanceled -> LocationResult.Cancelled(
                        "Cancellation of coordinate calculation"
                    )

                    !completedTask.isSuccessful -> LocationResult.Failed(
                        completedTask.exception ?: IllegalStateException(
                            "Location request failed without an exception"
                        )
                    )

                    else -> completedTask.result?.let { location ->
                        LocationResult.Success(
                            LocationModel(
                                geoPoint = GeoPoint(
                                    location.longitude,
                                    location.latitude,
                                    location.altitude
                                ),
                                accuracy = location.accuracy.takeIf { location.hasAccuracy() },
                                createdAt = location.time
                            )
                        )
                    } ?: LocationResult.Failed(
                        IllegalStateException("Location provider returned no location")
                    )
                }

                continuation.resume(result)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun observeLocation(): Flow<LocationModel> = callbackFlow {
        if (fusedLocationClient == null) init()

        val listener = LocationListener { location ->
            trySend(
                LocationModel(
                    geoPoint = GeoPoint(
                        location.longitude,
                        location.latitude,
                        location.altitude
                    ),
                    accuracy = location.accuracy.takeIf { location.hasAccuracy() },
                    createdAt = location.time
                )
            )
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
            .setMinUpdateIntervalMillis(1_000L)
            .setGranularity(Granularity.GRANULARITY_FINE)
            .build()

        fusedLocationClient?.requestLocationUpdates(
            locationRequest,
            ContextCompat.getMainExecutor(context),
            listener
        )

        awaitClose {
            fusedLocationClient?.removeLocationUpdates(listener)
        }
    }

    enum class TypeLocation {
        LAST,
        CURRENT
    }
}
