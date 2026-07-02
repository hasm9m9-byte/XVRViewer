package com.mohammed.xvrviewer.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mohammed.xvrviewer.MainActivity
import com.mohammed.xvrviewer.R
import com.mohammed.xvrviewer.XvrApplication
import com.mohammed.xvrviewer.data.Camera
import com.mohammed.xvrviewer.data.Recording
import com.mohammed.xvrviewer.rtsp.StreamRecorder
import kotlinx.coroutines.*
import java.io.File

/**
 * خدمة أمامية (Foreground Service) تعمل بشكل دائم في الخلفية،
 * تسجّل فيديو كل الكاميرات المفعّلة تلقائياً بدون أي أمر أو تدخل من المستخدم،
 * وتحذف أقدم التسجيلات تلقائياً عند تجاوز حصة التخزين المخصصة لكل كاميرا (تسجيل دائري).
 */
class RecordingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeRecorders = mutableMapOf<Long, StreamRecorder>()

    companion object {
        const val CHANNEL_ID = "xvr_recording_channel"
        const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, RecordingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RecordingService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("جارِ تشغيل نظام المراقبة..."))
        launchRecordingForAllCameras()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // إعادة تشغيل الخدمة تلقائياً من قبل النظام إن أُغلقت
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun launchRecordingForAllCameras() {
        val repo = (application as XvrApplication).repository
        serviceScope.launch {
            val cameras = repo.getRecordingEnabledCameras()
            cameras.forEach { camera -> startRecorderFor(camera) }
            updateNotification("تسجيل ${cameras.size} كاميرا تلقائياً")
        }
    }

    private fun startRecorderFor(camera: Camera) {
        val repo = (application as XvrApplication).repository
        val cameraDir = File(getExternalFilesDir(null), "recordings/${camera.id}")
        if (!cameraDir.exists()) cameraDir.mkdirs()

        val recorder = StreamRecorder(
            rtspUrl = camera.buildRtspUrl(),
            outputDir = cameraDir,
            onSegmentFinished = { file, start, end ->
                serviceScope.launch {
                    val recording = Recording(
                        cameraId = camera.id,
                        cameraName = camera.name,
                        filePath = file.absolutePath,
                        startTime = start,
                        endTime = end,
                        fileSizeBytes = file.length(),
                        isComplete = true
                    )
                    repo.addRecording(recording)
                    enforceStorageQuota(camera)
                }
            },
            onError = { msg ->
                updateNotification("خطأ بكاميرا ${camera.name}: $msg")
            }
        )
        recorder.start()
        activeRecorders[camera.id] = recorder
    }

    /** يحذف أقدم التسجيلات تلقائياً عند تجاوز حصة التخزين المخصصة للكاميرا (سلوك DVR دائري) */
    private suspend fun enforceStorageQuota(camera: Camera) {
        val repo = (application as XvrApplication).repository
        val quotaBytes = camera.storageQuotaMb * 1024 * 1024
        var total = repo.totalSizeForCamera(camera.id)
        if (total <= quotaBytes) return

        val oldest = repo.oldestFirstRecordings(camera.id)
        for (rec in oldest) {
            if (total <= quotaBytes) break
            val f = File(rec.filePath)
            if (f.exists()) f.delete()
            repo.deleteRecording(rec)
            total -= rec.fileSizeBytes
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activeRecorders.values.forEach { it.stop() }
        activeRecorders.clear()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "تسجيل كاميرات المراقبة", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("نظام المراقبة يعمل")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_camera)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }
}
