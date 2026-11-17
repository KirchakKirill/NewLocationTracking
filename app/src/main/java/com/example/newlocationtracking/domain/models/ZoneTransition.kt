package com.example.newlocationtracking.domain.models

sealed interface ZoneTransition {
    val poi: POIModel

    data class Entered(override val poi: POIModel) : ZoneTransition
    data class Exited(override val poi: POIModel) : ZoneTransition
    data class Switched(
        val previousPoi: POIModel,
        override val poi: POIModel
    ) : ZoneTransition
}
