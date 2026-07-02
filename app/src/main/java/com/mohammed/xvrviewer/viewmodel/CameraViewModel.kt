package com.mohammed.xvrviewer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohammed.xvrviewer.data.Camera
import com.mohammed.xvrviewer.data.CameraRepository
import kotlinx.coroutines.launch

class CameraViewModel(private val repository: CameraRepository) : ViewModel() {

    val cameras = repository.allCameras

    fun addCamera(camera: Camera, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.addCamera(camera)
            onDone(id)
        }
    }

    fun updateCamera(camera: Camera) {
        viewModelScope.launch { repository.updateCamera(camera) }
    }

    fun deleteCamera(camera: Camera) {
        viewModelScope.launch { repository.deleteCamera(camera) }
    }
}
