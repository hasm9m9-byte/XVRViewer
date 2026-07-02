package com.mohammed.xvrviewer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.mohammed.xvrviewer.service.RecordingService
import com.mohammed.xvrviewer.ui.theme.XvrViewerTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // بغض النظر عن النتيجة نشغّل الخدمة، الإشعار فقط لعرض الحالة
        startRecordingServiceAutomatically()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ensureNotificationPermissionThenStartService()

        setContent {
            XvrViewerTheme {
                val repository = (application as XvrApplication).repository
                XvrNavHost(repository = repository)
            }
        }
    }

    /** يبدأ التسجيل التلقائي المستمر لكل الكاميرات المفعّلة فور فتح التطبيق - بدون أي أمر يدوي */
    private fun ensureNotificationPermissionThenStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                startRecordingServiceAutomatically()
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            startRecordingServiceAutomatically()
        }
    }

    private fun startRecordingServiceAutomatically() {
        RecordingService.start(this)
    }
}
