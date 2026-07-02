package com.mohammed.xvrviewer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cameras")
data class Camera(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                 // اسم الكاميرا (مثلا: بوابة الدار)
    val ipAddress: String,             // عنوان IP الخاص بجهاز الـ XVR
    val port: Int = 554,               // منفذ RTSP الافتراضي
    val channel: Int = 1,              // رقم القناة داخل الـ XVR
    val streamPath: String = "",       // مسار السحب الإضافي إن وجد (اختياري، يُبنى تلقائياً إن ترك فارغ)
    val username: String = "",
    val password: String = "",
    val isRecordingEnabled: Boolean = true,  // تسجيل تلقائي مستمر بدون أي تدخل
    val storageQuotaMb: Long = 5120,   // الحد الأقصى للمساحة لكل كاميرا (افتراضي 5 جيجا)
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * يبني رابط RTSP القياسي لأجهزة XVR/DVR (متوافق مع أغلب الأجهزة الصينية
     * التي تدعم بروتوكول Dahua/Hikvision الشائع: rtsp://user:pass@ip:port/cam/realmonitor?channel=X&subtype=0)
     */
    fun buildRtspUrl(): String {
        val auth = if (username.isNotBlank()) "$username:$password@" else ""
        val path = streamPath.ifBlank { "cam/realmonitor?channel=$channel&subtype=0" }
        return "rtsp://$auth$ipAddress:$port/$path"
    }
}
