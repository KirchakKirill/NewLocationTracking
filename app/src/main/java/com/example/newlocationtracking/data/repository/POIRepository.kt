package com.example.newlocationtracking.data.repository

import com.example.newlocationtracking.data.local.dao.POIDao
import com.example.newlocationtracking.domain.utils.toPOIEntity
import com.example.newlocationtracking.domain.utils.toPOIModel
import com.example.newlocationtracking.domain.models.POIModel
import com.example.newlocationtracking.domain.repository.POIRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class POIRepository(
    private val poiDao: POIDao
) : POIRepository {
    override fun getAll(): Flow<List<POIModel>> {
        return poiDao.getAllPOI().map {
            it.map { it.toPOIModel() }
        }.flowOn(Dispatchers.Default)
    }

    override suspend fun savePOI(poiModel: POIModel): Long {
        return poiDao.insertPOI(poiModel.toPOIEntity())
    }
}