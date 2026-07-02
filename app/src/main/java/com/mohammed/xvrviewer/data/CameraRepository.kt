package com.mohammed.xvrviewer.data

import kotlinx.coroutines.flow.Flow

class CameraRepository(private val db: AppDatabase) {

    val allCameras: Flow<List<Camera>> = db.cameraDao().getAllCameras()
    val allRecordings: Flow<List<Recording>> = db.recordingDao().getAllRecordings()

    suspend fun addCamera(camera: Camera): Long = db.cameraDao().insert(camera)
    suspend fun updateCamera(camera: Camera) = db.cameraDao().update(camera)
    suspend fun deleteCamera(camera: Camera) = db.cameraDao().delete(camera)
    suspend fun getCamera(id: Long): Camera? = db.cameraDao().getCameraById(id)
    suspend fun getRecordingEnabledCameras(): List<Camera> = db.cameraDao().getRecordingEnabledCameras()

    fun recordingsForCamera(cameraId: Long): Flow<List<Recording>> =
        db.recordingDao().getRecordingsForCamera(cameraId)

    suspend fun addRecording(recording: Recording): Long = db.recordingDao().insert(recording)
    suspend fun updateRecording(recording: Recording) = db.recordingDao().update(recording)
    suspend fun deleteRecording(recording: Recording) = db.recordingDao().delete(recording)
    suspend fun totalSizeForCamera(cameraId: Long): Long = db.recordingDao().getTotalSizeForCamera(cameraId)
    suspend fun oldestFirstRecordings(cameraId: Long): List<Recording> =
        db.recordingDao().getRecordingsForCameraOrderedAsc(cameraId)
}
