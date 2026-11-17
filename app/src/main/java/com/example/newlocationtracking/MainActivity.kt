package com.example.newlocationtracking

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.newlocationtracking.presentation.MainScreen
import com.example.newlocationtracking.presentation.viewmodel.MainViewModel
import com.example.newlocationtracking.ui.theme.NewLocationTrackingTheme

class MainActivity : ComponentActivity() {

    private lateinit var mainViewModel: MainViewModel
    private var hasPreciseLocation by mutableStateOf(false)
    private var hasCameraPermission by mutableStateOf(false)

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPreciseLocation = hasPreciseLocationPermission()
        if (hasPreciseLocation) {
            mainViewModel.startLocationMonitoring()
            mainViewModel.showMessage(
                "Геолокация включена. Дождитесь подходящей точности, чтобы добавить зону."
            )
        } else {
            mainViewModel.showMessage(
                "Разрешите точную геолокацию, чтобы добавлять и отслеживать зоны."
            )
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) mainViewModel.showMessage("Для AR-режима нужно разрешение камеры.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainViewModel = ViewModelProvider(
            this,
            (application as NewLocationTrackingApplication).appContainer.mainViewModelFactory
        )[MainViewModel::class.java]

        hasPreciseLocation = hasPreciseLocationPermission()
        hasCameraPermission = isCameraPermissionGranted()
        enableEdgeToEdge()
        setContent {
            NewLocationTrackingTheme {
                MainScreen(
                    mainViewModel = mainViewModel,
                    locationPermissionGranted = hasPreciseLocation,
                    cameraPermissionGranted = hasCameraPermission,
                    onAddZoneClick = ::onAddZoneClick,
                    onRequestCameraPermission = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasPreciseLocation = hasPreciseLocationPermission()
        hasCameraPermission = isCameraPermissionGranted()
        if (hasPreciseLocation) {
            mainViewModel.startLocationMonitoring()
        } else if (::mainViewModel.isInitialized) {
            mainViewModel.stopLocationMonitoring()
        }
    }

    override fun onStop() {
        mainViewModel.stopLocationMonitoring()
        super.onStop()
    }

    private fun onAddZoneClick() {
        if (hasPreciseLocationPermission()) {
            hasPreciseLocation = true
            mainViewModel.startLocationMonitoring()
            mainViewModel.addPoiHere()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun hasPreciseLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

    private fun isCameraPermissionGranted(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
}
