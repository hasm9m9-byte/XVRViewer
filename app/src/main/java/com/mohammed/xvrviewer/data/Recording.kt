package com.mohammed.xvrviewer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cameraId: Long,
    val cameraName: String,
    val filePath: String,
    val startTime: Long,
    var endTime: Long = 0,
    var fileSizeBytes: Long = 0,
    var isComplete: Boolean = false
)
