package com.example.newlocationtracking.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.newlocationtracking.data.local.entity.ARPlacementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ARPlacementDao {
    @Insert
    suspend fun insert(placement: ARPlacementEntity): Long

    @Query("SELECT * FROM ar_placement_table WHERE poi_id = :poiId ORDER BY id")
    fun observeForZone(poiId: Long): Flow<List<ARPlacementEntity>>
}
