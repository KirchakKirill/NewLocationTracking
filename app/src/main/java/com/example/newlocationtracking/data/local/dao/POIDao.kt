package com.example.newlocationtracking.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.newlocationtracking.data.local.entity.POIEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface POIDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPOI(poiEntity: POIEntity): Long

    @Query("""
        SELECT * FROM poi_table
    """)
    fun getAllPOI(): Flow<List<POIEntity>>
}
