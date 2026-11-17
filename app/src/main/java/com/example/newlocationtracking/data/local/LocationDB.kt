package com.example.newlocationtracking.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.newlocationtracking.data.local.dao.ARPlacementDao
import com.example.newlocationtracking.data.local.dao.POIDao
import com.example.newlocationtracking.data.local.entity.ARPlacementEntity
import com.example.newlocationtracking.data.local.entity.POIEntity

@Database(entities = [POIEntity::class, ARPlacementEntity::class], version = 1)
abstract class LocationDB: RoomDatabase() {
    abstract fun getPOIDao(): POIDao
    abstract fun getARPlacementDao(): ARPlacementDao
}
