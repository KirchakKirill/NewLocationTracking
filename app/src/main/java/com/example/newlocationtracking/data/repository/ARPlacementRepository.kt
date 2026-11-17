package com.example.newlocationtracking.data.repository

import com.example.newlocationtracking.data.local.dao.ARPlacementDao
import com.example.newlocationtracking.data.local.entity.ARPlacementEntity
import com.example.newlocationtracking.domain.models.ARPlacementModel
import com.example.newlocationtracking.domain.utils.toModel
import com.example.newlocationtracking.domain.repository.ARPlacementRepository as ARPlacementRepositoryContract
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ARPlacementRepository(
    private val dao: ARPlacementDao
) : ARPlacementRepositoryContract {
    override fun observeForZone(poiId: Long): Flow<List<ARPlacementModel>> =
        dao.observeForZone(poiId).map { placements -> placements.map { it.toModel() } }

    override suspend fun save(placement: ARPlacementModel): Long = dao.insert(
        ARPlacementEntity(
            id = placement.id,
            poiId = placement.poiId,
            x = placement.x,
            y = placement.y,
            z = placement.z,
            qx = placement.qx,
            qy = placement.qy,
            qz = placement.qz,
            qw = placement.qw,
            createdAt = placement.createdAt
        )
    )
}
