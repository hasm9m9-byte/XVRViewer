package com.mohammed.xvrviewer.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CameraDao {
    @Query("SELECT * FROM cameras ORDER BY createdAt ASC")
    fun getAllCameras(): Flow<List<Camera>>

    @Query("SELECT * FROM cameras WHERE isRecordingEnabled = 1")
    suspend fun getRecordingEnabledCameras(): List<Camera>

    @Query("SELECT * FROM cameras WHERE id = :id")
    suspend fun getCameraById(id: Long): Camera?

    @Insert
    suspend fun insert(camera: Camera): Long

    @Update
    suspend fun update(camera: Camera)

    @Delete
    suspend fun delete(camera: Camera)
}
