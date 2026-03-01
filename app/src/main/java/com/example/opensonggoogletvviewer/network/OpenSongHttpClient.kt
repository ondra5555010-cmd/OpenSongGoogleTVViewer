package com.example.opensonggoogletvviewer.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class OpenSongHttpClient(
    private val client: OkHttpClient,
    private val host: String,
    private val port: Int = 8082
) {

    private fun buildRequest(url: String): Request {
        return Request.Builder()
            .url(url)

            // --- Important for OpenSong (prevents HTTP 403) ---
            .addHeader(
                "User-Agent",
                "Mozilla/5.0 (Android TV; Linux; Android 12) AppleWebKit/537.36 Chrome/120 Safari/537.36"
            )
            .addHeader("Accept", "text/xml,application/xml,text/html;q=0.9,*/*;q=0.8")
            .addHeader("Connection", "keep-alive")
            .addHeader("Cache-Control", "no-cache")

            .get()
            .build()
    }

    suspend fun getCurrentSlideXml(): String = withContext(Dispatchers.IO) {
        val url = "http://$host:$port/presentation/slide/current"
        val request = buildRequest(url)

        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string() ?: ""

            if (!resp.isSuccessful) {
                // this helps debugging a LOT with OpenSong
                throw RuntimeException("HTTP ${resp.code}\n$body")
            }

            body
        }
    }
}