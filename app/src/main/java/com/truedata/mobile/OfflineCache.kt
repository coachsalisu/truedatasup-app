package com.truedata.mobile

import android.content.Context
import android.text.format.DateFormat
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Lets the dashboard (and any other page the user has already visited while
 * online) open even with no internet connection, by keeping a local copy of
 * each page from the last time it loaded successfully.
 *
 * Safety rules - do not relax these:
 *  1. Only GET requests are ever cached or served from cache. Purchases,
 *     login, and every other form submission is POST, and POST is never
 *     touched here - those always go straight to the live server, as they
 *     must for a financial app.
 *  2. The network is always tried FIRST. The saved copy is only used when
 *     the live request genuinely fails.
 *  3. Every HTML page served from the offline cache gets a visible red
 *     banner injected at the top showing when it was saved, so a saved
 *     balance/history can never be mistaken for a live one.
 */
class OfflineCache(context: Context) {

    private val cacheDir = File(context.cacheDir, "offline_pages").apply { mkdirs() }
    private val connectTimeoutMs = 6000
    private val readTimeoutMs = 6000

    fun intercept(request: WebResourceRequest): WebResourceResponse? {
        val url = request.url.toString()

        // Only ever cache/serve our own site, and only ever cache safe GET requests.
        if (!url.contains("truedata.com.ng")) return null
        if (!request.method.equals("GET", ignoreCase = true)) return null

        val cacheFile = fileFor(url)

        // 1) Try the network first - always prefer live data when it's available.
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = connectTimeoutMs
            conn.readTimeout = readTimeoutMs
            conn.instanceFollowRedirects = true
            conn.connect()

            if (conn.responseCode in 200..299) {
                val bytes = conn.inputStream.use { it.readBytes() }
                val mimeType = (conn.contentType ?: guessMimeType(url)).substringBefore(";").trim()

                // Save a fresh copy for next time we're offline. Never cache
                // login/registration pages themselves with prefilled data -
                // this is plain HTML, no personal data is added by us here.
                runCatching { cacheFile.writeBytes(bytes) }

                return WebResourceResponse(mimeType, "UTF-8", bytes.inputStream())
            }
        } catch (e: Exception) {
            // Network failed - most likely no internet. Fall through to cache below.
        }

        // 2) No live connection - serve the last saved copy, if we have one.
        if (cacheFile.exists()) {
            val bytes = cacheFile.readBytes()
            val mimeType = guessMimeType(url)
            val finalBytes = if (mimeType == "text/html") {
                injectOfflineBanner(bytes, cacheFile.lastModified())
            } else {
                bytes
            }
            return WebResourceResponse(mimeType, "UTF-8", finalBytes.inputStream())
        }

        // 3) Nothing saved either - let WebView's normal error handling take
        // over. This is what shows the full "No Internet Connection" screen,
        // and only happens on a page that has genuinely never loaded before.
        return null
    }

    private fun fileFor(url: String): File {
        val hash = MessageDigest.getInstance("MD5").digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(cacheDir, hash)
    }

    private fun guessMimeType(url: String): String = when {
        url.contains(".css") -> "text/css"
        url.contains(".js") -> "application/javascript"
        url.contains(".png") -> "image/png"
        url.contains(".jpg") || url.contains(".jpeg") -> "image/jpeg"
        url.contains(".svg") -> "image/svg+xml"
        url.contains(".woff") -> "font/woff"
        else -> "text/html"
    }

    private fun injectOfflineBanner(htmlBytes: ByteArray, savedAt: Long): ByteArray {
        val html = String(htmlBytes, Charsets.UTF_8)
        val savedTime = DateFormat.format("dd MMM, h:mm a", savedAt)
        val banner = """
            <div style="position:sticky;top:0;z-index:99999;background:#B23B3B;color:#fff;
                        text-align:center;padding:8px 12px;font-family:sans-serif;font-size:13px;">
                You're offline — showing data saved on $savedTime. Balance and history may not be current.
            </div>
        """.trimIndent()
        val marker = "<body"
        val idx = html.indexOf(marker)
        return if (idx != -1) {
            val bodyTagEnd = html.indexOf(">", idx) + 1
            (html.substring(0, bodyTagEnd) + banner + html.substring(bodyTagEnd)).toByteArray(Charsets.UTF_8)
        } else {
            html.toByteArray(Charsets.UTF_8)
        }
    }
}
