package com.mohammed.xvrviewer.rtsp

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Base64
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import kotlin.concurrent.thread

/**
 * يسجّل تدفق كاميرا واحدة بشكل مستمر ومقسّم إلى ملفات (segments) بدون أي تدخل يدوي.
 * كل segment ينتج ملف MP4 صالح للتشغيل بمجرد اكتماله.
 */
class StreamRecorder(
    private val rtspUrl: String,
    private val outputDir: File,
    private val segmentDurationMs: Long = 5 * 60 * 1000L, // 5 دقائق لكل ملف (قابل للتعديل)
    private val onSegmentFinished: (File, startTime: Long, endTime: Long) -> Unit,
    private val onError: (String) -> Unit
) {
    @Volatile
    private var running = false
    private var recorderThread: Thread? = null
    private var client: RtspClient? = null

    fun start() {
        running = true
        recorderThread = thread(start = true, name = "StreamRecorder") {
            while (running) {
                try {
                    recordLoop()
                } catch (e: Exception) {
                    onError(e.message ?: "خطأ غير معروف في التسجيل")
                }
                if (running) {
                    // إعادة محاولة الاتصال تلقائياً بعد انقطاع الشبكة أو الجهاز
                    Thread.sleep(3000)
                }
            }
        }
    }

    fun stop() {
        running = false
        client?.stop()
        recorderThread?.interrupt()
    }

    private fun recordLoop() {
        val depacketizer = H264RtpDepacketizer()
        var muxer: MediaMuxer? = null
        var videoTrackIndex = -1
        var muxerStarted = false
        var segmentStartTime = 0L
        var firstPts = -1L
        var currentFile: File? = null
        var csd0: ByteArray? = null

        client = RtspClient(rtspUrl)

        client!!.connectAndPlay { channel, data, length ->
            if (!running) return@connectAndPlay
            if (channel != 0) return@connectAndPlay // القناة 0 = فيديو (حسب SETUP)

            depacketizer.onRtpPacket(data, length) { accessUnit ->
                try {
                    // بناء SPS/PPS (csd-0) من SDP عند أول وحدة مفاتيح
                    if (csd0 == null) {
                        val sps = client!!.sdpInfo?.spsBase64
                        val pps = client!!.sdpInfo?.ppsBase64
                        if (sps != null && pps != null) {
                            csd0 = buildCsd0(sps, pps)
                        }
                    }

                    if (muxer == null && accessUnit.isKeyFrame && csd0 != null) {
                        // ابدأ ملف جديد
                        val fileName = "REC_${System.currentTimeMillis()}.mp4"
                        currentFile = File(outputDir, fileName)
                        muxer = MediaMuxer(currentFile!!.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1920, 1080)
                        format.setByteBuffer("csd-0", ByteBuffer.wrap(csd0))
                        videoTrackIndex = muxer!!.addTrack(format)
                        muxer!!.start()
                        muxerStarted = true
                        segmentStartTime = System.currentTimeMillis()
                        firstPts = accessUnit.timestamp
                    }

                    if (muxerStarted) {
                        val combined = combineNals(accessUnit.nalUnits)
                        val ptsUs = ((accessUnit.timestamp - firstPts).coerceAtLeast(0)) * 1_000_000L / 90000L
                        val bufferInfo = MediaCodec.BufferInfo()
                        bufferInfo.offset = 0
                        bufferInfo.size = combined.size
                        bufferInfo.presentationTimeUs = ptsUs
                        bufferInfo.flags = if (accessUnit.isKeyFrame) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                        muxer!!.writeSampleData(videoTrackIndex, ByteBuffer.wrap(combined), bufferInfo)

                        // إنهاء المقطع الحالي وبدء مقطع جديد بعد المدة المحددة (تسجيل دائري تلقائي)
                        if (System.currentTimeMillis() - segmentStartTime >= segmentDurationMs) {
                            finishSegment(muxer, currentFile, segmentStartTime)
                            muxer = null
                            muxerStarted = false
                        }
                    }
                } catch (e: Exception) {
                    Log.e("StreamRecorder", "خطأ أثناء كتابة العينة: ${e.message}")
                }
            }
        }

        // عند خروج حلقة القراءة (انقطاع الاتصال) أنهِ المقطع الحالي إن وجد
        finishSegment(muxer, currentFile, segmentStartTime)
    }

    private fun finishSegment(muxer: MediaMuxer?, file: File?, startTime: Long) {
        if (muxer == null || file == null || startTime == 0L) return
        try {
            muxer.stop()
            muxer.release()
            onSegmentFinished(file, startTime, System.currentTimeMillis())
        } catch (e: Exception) {
            Log.e("StreamRecorder", "تعذر إغلاق المقطع: ${e.message}")
        }
    }

    private fun combineNals(nals: List<ByteArray>): ByteArray {
        val total = nals.sumOf { it.size }
        val out = ByteArray(total)
        var pos = 0
        for (n in nals) {
            System.arraycopy(n, 0, out, pos, n.size)
            pos += n.size
        }
        return out
    }

    private fun buildCsd0(spsB64: String, ppsB64: String): ByteArray {
        val sps = Base64.decode(spsB64, Base64.DEFAULT)
        val pps = Base64.decode(ppsB64, Base64.DEFAULT)
        val startCode = byteArrayOf(0, 0, 0, 1)
        val out = ByteArray(startCode.size * 2 + sps.size + pps.size)
        var pos = 0
        System.arraycopy(startCode, 0, out, pos, startCode.size); pos += startCode.size
        System.arraycopy(sps, 0, out, pos, sps.size); pos += sps.size
        System.arraycopy(startCode, 0, out, pos, startCode.size); pos += startCode.size
        System.arraycopy(pps, 0, out, pos, pps.size)
        return out
    }
}
