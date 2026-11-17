package com.example.newlocationtracking.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = ARPlacementEntity.TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = POIEntity::class,
            parentColumns = ["id"],
            childColumns = ["poi_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["poi_id"])]
)
data class ARPlacementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "poi_id")
    val poiId: Long,
    val x: Float,
    val y: Float,
    val z: Float,
    val qx: Float,
    val qy: Float,
    val qz: Float,
    val qw: Float,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TABLE_NAME = "ar_placement_table"
    }
}
