package com.mohammed.xvrviewer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // يشغّل خدمة التسجيل تلقائياً فور إعادة تشغيل الجهاز - بدون أي تدخل من المستخدم
            RecordingService.start(context)
        }
    }
}
