package com.example.newlocationtracking.domain.utils

import com.example.newlocationtracking.data.local.entity.ARPlacementEntity
import com.example.newlocationtracking.data.local.entity.POIEntity
import com.example.newlocationtracking.domain.models.ARPlacementModel
import com.example.newlocationtracking.domain.models.GeoPoint
import com.example.newlocationtracking.domain.models.LocationModel
import com.example.newlocationtracking.domain.models.POIModel

fun POIEntity.toPOIModel(): POIModel = POIModel(
    id = id,
    geoPoint = GeoPoint(
        longitude, latitude, altitude
    ),
    createdAt = createdAt,
    radius = radius,
    accuracy = accuracy
)

fun POIModel.toPOIEntity(): POIEntity = POIEntity(
    id = id,
    longitude = geoPoint.longitude,
    latitude = geoPoint.latitude,
    altitude = geoPoint.altitude,
    createdAt = createdAt,
    radius = radius,
    accuracy = accuracy,
)

fun LocationModel.toPOIModel(): POIModel = POIModel(
    geoPoint = geoPoint,
    accuracy = accuracy,
    createdAt = createdAt
)

fun ARPlacementEntity.toModel() = ARPlacementModel(
    id = id,
    poiId = poiId,
    x = x,
    y = y,
    z = z,
    qx = qx,
    qy = qy,
    qz = qz,
    qw = qw,
    createdAt = createdAt
)
