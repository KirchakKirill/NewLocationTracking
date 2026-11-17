package com.example.newlocationtracking.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = POIEntity.TABLE_NAME)
data class POIEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "longitude")
    val longitude: Double,
    @ColumnInfo(name = "latitude")
    val latitude: Double,
    @ColumnInfo(name = "altitude")
    val altitude: Double,
    @ColumnInfo(name = "accuracy")
    val accuracy: Float? = null,
    @ColumnInfo(name = "radius")
    val radius: Double,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
) {
    companion object {
        const val TABLE_NAME = "poi_table"
    }
}
