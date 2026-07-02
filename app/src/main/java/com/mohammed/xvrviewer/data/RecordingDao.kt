package com.mohammed.xvrviewer.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY startTime DESC")
    fun getAllRecordings(): Flow<List<Recording>>

    @Query("SELECT * FROM recordings WHERE cameraId = :cameraId ORDER BY startTime DESC")
    fun getRecordingsForCamera(cameraId: Long): Flow<List<Recording>>

    @Query("SELECT * FROM recordings WHERE cameraId = :cameraId ORDER BY startTime ASC")
    suspend fun getRecordingsForCameraOrderedAsc(cameraId: Long): List<Recording>

    @Insert
    suspend fun insert(recording: Recording): Long

    @Update
    suspend fun update(recording: Recording)

    @Delete
    suspend fun delete(recording: Recording)

    @Query("SELECT COALESCE(SUM(fileSizeBytes), 0) FROM recordings WHERE cameraId = :cameraId")
    suspend fun getTotalSizeForCamera(cameraId: Long): Long
}
