package com.mohammed.xvrviewer.rtsp

import java.io.ByteArrayOutputStream

data class AccessUnit(
    val nalUnits: List<ByteArray>, // كل عنصر يبدأ بـ start code 00 00 00 01
    val timestamp: Long,           // RTP timestamp (ساعة 90kHz)
    val isKeyFrame: Boolean
)

/**
 * يحول تدفق حزم RTP الخاصة بـ H264 (RFC 6184) إلى وحدات نفاذ (Access Units)
 * جاهزة للتغذية إلى MediaMuxer.
 */
class H264RtpDepacketizer {

    private var fuBuffer: ByteArrayOutputStream? = null
    private var fuNalHeader: Byte = 0
    private val pendingNals = mutableListOf<ByteArray>()
    private var pendingTimestamp: Long = -1
    private var pendingHasKeyFrame = false

    private val startCode = byteArrayOf(0, 0, 0, 1)

    fun onRtpPacket(payload: ByteArray, length: Int, result: (AccessUnit) -> Unit) {
        if (length < 12) return
        val marker = (payload[1].toInt() and 0x80) != 0
        val timestamp =
            ((payload[4].toLong() and 0xFF) shl 24) or
            ((payload[5].toLong() and 0xFF) shl 16) or
            ((payload[6].toLong() and 0xFF) shl 8) or
            (payload[7].toLong() and 0xFF)

        val csrcCount = payload[0].toInt() and 0x0F
        var offset = 12 + csrcCount * 4
        if (offset >= length) return

        // امتداد الرأس RTP إن وجد
        val hasExtension = (payload[0].toInt() and 0x10) != 0
        if (hasExtension && offset + 4 <= length) {
            val extLen = ((payload[offset + 2].toInt() and 0xFF) shl 8) or (payload[offset + 3].toInt() and 0xFF)
            offset += 4 + extLen * 4
        }
        if (offset >= length) return

        if (pendingTimestamp == -1L) pendingTimestamp = timestamp

        val nalHeader = payload[offset]
        val nalType = nalHeader.toInt() and 0x1F

        when (nalType) {
            in 1..23 -> {
                // وحدة NAL مفردة كاملة
                addNal(payload, offset, length - offset, nalType)
            }
            24 -> {
                // STAP-A: عدة وحدات NAL مجمّعة
                var p = offset + 1
                while (p + 2 <= length) {
                    val size = ((payload[p].toInt() and 0xFF) shl 8) or (payload[p + 1].toInt() and 0xFF)
                    p += 2
                    if (p + size > length) break
                    val innerType = payload[p].toInt() and 0x1F
                    addNal(payload, p, size, innerType)
                    p += size
                }
            }
            28 -> {
                // FU-A: تجزئة وحدة NAL كبيرة عبر عدة حزم
                if (offset + 2 > length) return
                val fuHeader = payload[offset + 1]
                val start = (fuHeader.toInt() and 0x80) != 0
                val end = (fuHeader.toInt() and 0x40) != 0
                val originalType = fuHeader.toInt() and 0x1F

                if (start) {
                    fuBuffer = ByteArrayOutputStream()
                    fuNalHeader = ((nalHeader.toInt() and 0xE0) or originalType).toByte()
                    fuBuffer?.write(fuNalHeader.toInt())
                }
                fuBuffer?.write(payload, offset + 2, length - offset - 2)

                if (end && fuBuffer != null) {
                    val nal = fuBuffer!!.toByteArray()
                    registerNal(nal, originalType)
                    fuBuffer = null
                }
            }
        }

        if (marker) {
            // نهاية الإطار (access unit) - أرسل ما تم تجميعه
            if (pendingNals.isNotEmpty()) {
                result(AccessUnit(pendingNals.toList(), pendingTimestamp, pendingHasKeyFrame))
            }
            pendingNals.clear()
            pendingTimestamp = -1
            pendingHasKeyFrame = false
        }
    }

    private fun addNal(source: ByteArray, offset: Int, size: Int, nalType: Int) {
        val nal = ByteArray(size)
        System.arraycopy(source, offset, nal, 0, size)
        registerNal(nal, nalType)
    }

    private fun registerNal(nal: ByteArray, nalType: Int) {
        val withStartCode = ByteArray(startCode.size + nal.size)
        System.arraycopy(startCode, 0, withStartCode, 0, startCode.size)
        System.arraycopy(nal, 0, withStartCode, startCode.size, nal.size)
        pendingNals.add(withStartCode)
        if (nalType == 5) pendingHasKeyFrame = true // IDR
    }
}
