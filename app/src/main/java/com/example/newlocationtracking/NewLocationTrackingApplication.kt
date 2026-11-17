package com.example.newlocationtracking

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.newlocationtracking.data.local.LocationDB
import com.example.newlocationtracking.data.location.LocationManager
import com.example.newlocationtracking.data.repository.ARPlacementRepository as ARPlacementRepositoryImpl
import com.example.newlocationtracking.data.repository.LocationRepository as LocationRepositoryImpl
import com.example.newlocationtracking.data.repository.POIRepository as POIRepositoryImpl
import com.example.newlocationtracking.domain.usecase.AddPoiHere
import com.example.newlocationtracking.domain.usecase.ObserveARPlacements
import com.example.newlocationtracking.domain.usecase.ObserveNearbyPOI
import com.example.newlocationtracking.domain.usecase.ObserveZoneTransitions
import com.example.newlocationtracking.domain.usecase.SaveARPlacement
import com.example.newlocationtracking.presentation.viewmodel.MainViewModel

class NewLocationTrackingApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }
}

class AppContainer(application: Application) {
    private val database = Room.databaseBuilder(
        application,
        LocationDB::class.java,
        "location_database"
    )
        .build()

    private val locationRepository = LocationRepositoryImpl(
        LocationManager(application).also { it.init() }
    )
    private val poiRepository = POIRepositoryImpl(database.getPOIDao())
    private val arPlacementRepository = ARPlacementRepositoryImpl(database.getARPlacementDao())

    val mainViewModelFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (!modelClass.isAssignableFrom(MainViewModel::class.java)) {
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }

            return MainViewModel(
                addPoiHere = AddPoiHere(locationRepository, poiRepository),
                observeZoneTransitions = ObserveZoneTransitions(locationRepository, poiRepository),
                observeNearbyPOI = ObserveNearbyPOI(locationRepository, poiRepository),
                observeARPlacements = ObserveARPlacements(arPlacementRepository),
                saveARPlacement = SaveARPlacement(arPlacementRepository)
            ) as T
        }
    }
}
