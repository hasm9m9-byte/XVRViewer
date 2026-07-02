package com.mohammed.xvrviewer.rtsp

import android.util.Base64
import android.util.Log
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.OutputStream
import java.net.Socket
import java.net.URI
import java.security.MessageDigest

/**
 * عميل RTSP خفيف يدعم النقل عبر TCP (interleaved) وهو النمط الأكثر توافقاً
 * مع أجهزة الـ XVR/DVR خلف الراوتر لأنه لا يحتاج فتح منافذ UDP.
 * يدعم المصادقة Basic و Digest (شائعة جداً في أجهزة Dahua / Hikvision وما شابه).
 */
class RtspClient(private val rtspUrl: String) {

    data class SdpInfo(
        val controlUrl: String,
        val payloadType: Int,
        val spsBase64: String?,
        val ppsBase64: String?,
        val clockRate: Int = 90000
    )

    private lateinit var socket: Socket
    private lateinit var output: OutputStream
    private lateinit var input: DataInputStream
    private var cSeq = 1
    private var sessionId: String? = null
    private var authHeader: String? = null

    private lateinit var uri: URI
    private var username: String = ""
    private var password: String = ""

    var sdpInfo: SdpInfo? = null
        private set

    @Volatile
    var isRunning = false
        private set

    fun connectAndPlay(onPacket: (channel: Int, data: ByteArray, length: Int) -> Unit) {
        uri = URI(rtspUrl)
        val userInfo = uri.userInfo
        if (userInfo != null) {
            val parts = userInfo.split(":")
            username = parts.getOrElse(0) { "" }
            password = parts.getOrElse(1) { "" }
        }
        val host = uri.host
        val port = if (uri.port == -1) 554 else uri.port

        socket = Socket()
        socket.connect(java.net.InetSocketAddress(host, port), 8000)
        socket.soTimeout = 15000
        output = socket.getOutputStream()
        input = DataInputStream(BufferedInputStream(socket.getInputStream()))

        val baseUrl = "rtsp://$host:$port${uri.path ?: ""}${if (uri.query != null) "?${uri.query}" else ""}"

        sendRequestWithAuth("OPTIONS", baseUrl)
        val describeResp = sendRequestWithAuth("DESCRIBE", baseUrl, extraHeaders = mapOf("Accept" to "application/sdp"))
        val sdp = parseSdp(describeResp.body, baseUrl)
        sdpInfo = sdp

        val setupResp = sendRequestWithAuth(
            "SETUP", sdp.controlUrl,
            extraHeaders = mapOf("Transport" to "RTP/AVP/TCP;unicast;interleaved=0-1")
        )
        sessionId = setupResp.headers["Session"]?.split(";")?.get(0)?.trim()

        sendRequestWithAuth("PLAY", baseUrl, extraHeaders = mapOf("Range" to "npt=0.000-"))

        isRunning = true
        readInterleavedLoop(onPacket)
    }

    fun stop() {
        isRunning = false
        try {
            socket.close()
        } catch (_: Exception) {
        }
    }

    private fun readInterleavedLoop(onPacket: (Int, ByteArray, Int) -> Unit) {
        val header = ByteArray(4)
        while (isRunning) {
            try {
                input.readFully(header, 0, 1)
                if (header[0] != '$'.code.toByte()) continue // مزامنة حتى إيجاد بداية حزمة
                input.readFully(header, 1, 3)
                val channel = header[1].toInt() and 0xFF
                val len = ((header[2].toInt() and 0xFF) shl 8) or (header[3].toInt() and 0xFF)
                val payload = ByteArray(len)
                input.readFully(payload, 0, len)
                onPacket(channel, payload, len)
            } catch (e: Exception) {
                if (isRunning) Log.w("RtspClient", "توقفت قراءة البث: ${e.message}")
                isRunning = false
            }
        }
    }

    private data class RtspResponse(val statusCode: Int, val headers: Map<String, String>, val body: String)

    private fun sendRequestWithAuth(
        method: String,
        url: String,
        extraHeaders: Map<String, String> = emptyMap()
    ): RtspResponse {
        var resp = sendRequest(method, url, extraHeaders)
        if (resp.statusCode == 401 && authHeader == null) {
            val wwwAuth = resp.headers["WWW-Authenticate"] ?: ""
            authHeader = buildAuthHeader(method, url, wwwAuth)
            resp = sendRequest(method, url, extraHeaders)
        }
        if (resp.statusCode !in 200..299) {
            throw RuntimeException("فشل طلب RTSP ($method): ${resp.statusCode}")
        }
        return resp
    }

    private fun sendRequest(method: String, url: String, extraHeaders: Map<String, String>): RtspResponse {
        val sb = StringBuilder()
        sb.append("$method $url RTSP/1.0\r\n")
        sb.append("CSeq: ${cSeq++}\r\n")
        sb.append("User-Agent: XVRViewer/1.0\r\n")
        sessionId?.let { sb.append("Session: $it\r\n") }
        authHeader?.let { sb.append("Authorization: $it\r\n") }
        for ((k, v) in extraHeaders) sb.append("$k: $v\r\n")
        sb.append("\r\n")

        output.write(sb.toString().toByteArray(Charsets.US_ASCII))
        output.flush()

        return readResponse()
    }

    private fun readResponse(): RtspResponse {
        val statusLine = readLine()
        val statusCode = Regex("RTSP/1\\.0 (\\d+)").find(statusLine)?.groupValues?.get(1)?.toIntOrNull() ?: -1
        val headers = mutableMapOf<String, String>()
        var line = readLine()
        while (line.isNotEmpty()) {
            val idx = line.indexOf(':')
            if (idx > 0) {
                headers[line.substring(0, idx).trim()] = line.substring(idx + 1).trim()
            }
            line = readLine()
        }
        var body = ""
        val contentLength = headers["Content-Length"]?.toIntOrNull() ?: 0
        if (contentLength > 0) {
            val buf = ByteArray(contentLength)
            input.readFully(buf)
            body = String(buf, Charsets.UTF_8)
        }
        return RtspResponse(statusCode, headers, body)
    }

    private fun readLine(): String {
        val sb = StringBuilder()
        var prev = -1
        while (true) {
            val b = input.read()
            if (b == -1) break
            if (prev == '\r'.code && b == '\n'.code) {
                sb.setLength(sb.length - 1)
                break
            }
            sb.append(b.toChar())
            prev = b
        }
        return sb.toString()
    }

    private fun buildAuthHeader(method: String, url: String, wwwAuth: String): String {
        return if (wwwAuth.startsWith("Digest", ignoreCase = true)) {
            val realm = extractQuoted(wwwAuth, "realm")
            val nonce = extractQuoted(wwwAuth, "nonce")
            val ha1 = md5("$username:$realm:$password")
            val ha2 = md5("$method:$url")
            val response = md5("$ha1:$nonce:$ha2")
            "Digest username=\"$username\", realm=\"$realm\", nonce=\"$nonce\", uri=\"$url\", response=\"$response\""
        } else {
            val token = Base64.encodeToString("$username:$password".toByteArray(), Base64.NO_WRAP)
            "Basic $token"
        }
    }

    private fun extractQuoted(source: String, key: String): String {
        val match = Regex("$key=\"([^\"]*)\"").find(source)
        return match?.groupValues?.get(1) ?: ""
    }

    private fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /** يحلل استجابة SDP لاستخراج معلومات مسار الفيديو H264 */
    private fun parseSdp(sdp: String, baseUrl: String): SdpInfo {
        var payloadType = -1
        var control = ""
        var spsB64: String? = null
        var ppsB64: String? = null
        var inVideoBlock = false
        var sessionControl = ""

        for (rawLine in sdp.lines()) {
            val l = rawLine.trim()
            when {
                l.startsWith("a=control:") && !inVideoBlock -> {
                    sessionControl = l.removePrefix("a=control:")
                }
                l.startsWith("m=video") -> {
                    inVideoBlock = true
                    val parts = l.split(" ")
                    payloadType = parts.lastOrNull()?.toIntOrNull() ?: 96
                }
                l.startsWith("m=") && !l.startsWith("m=video") -> {
                    inVideoBlock = false
                }
                inVideoBlock && l.startsWith("a=control:") -> {
                    control = l.removePrefix("a=control:")
                }
                inVideoBlock && l.startsWith("a=fmtp:") -> {
                    val spropMatch = Regex("sprop-parameter-sets=([^;]+)").find(l)
                    spropMatch?.groupValues?.get(1)?.split(",")?.let { sets ->
                        if (sets.isNotEmpty()) spsB64 = sets[0]
                        if (sets.size > 1) ppsB64 = sets[1]
                    }
                }
            }
        }

        val controlUrl = when {
            control.startsWith("rtsp://") -> control
            control.isNotEmpty() -> "$baseUrl/$control"
            sessionControl.startsWith("rtsp://") -> sessionControl
            else -> baseUrl
        }

        return SdpInfo(controlUrl, if (payloadType == -1) 96 else payloadType, spsB64, ppsB64)
    }
}
