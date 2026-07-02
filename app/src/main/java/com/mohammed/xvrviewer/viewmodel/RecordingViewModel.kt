package com.mohammed.xvrviewer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mohammed.xvrviewer.data.CameraRepository
import com.mohammed.xvrviewer.data.Recording
import kotlinx.coroutines.launch
import java.io.File

class RecordingViewModel(private val repository: CameraRepository) : ViewModel() {

    val recordings = repository.allRecordings

    fun deleteRecording(recording: Recording) {
        viewModelScope.launch {
            val f = File(recording.filePath)
            if (f.exists()) f.delete()
            repository.deleteRecording(recording)
        }
    }
}

/** مصنع بسيط لإنشاء الـ ViewModels مع تمرير الـ Repository */
class AppViewModelFactory(private val repository: CameraRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(CameraViewModel::class.java) -> CameraViewModel(repository) as T
            modelClass.isAssignableFrom(RecordingViewModel::class.java) -> RecordingViewModel(repository) as T
            else -> throw IllegalArgumentException("ViewModel غير معروف: ${modelClass.name}")
        }
    }
}
