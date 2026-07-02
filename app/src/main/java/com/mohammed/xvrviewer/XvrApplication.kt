package com.mohammed.xvrviewer

import android.app.Application
import com.mohammed.xvrviewer.data.AppDatabase
import com.mohammed.xvrviewer.data.CameraRepository

class XvrApplication : Application() {
    lateinit var repository: CameraRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(this)
        repository = CameraRepository(db)
    }
}
