package com.example.opensonggoogletvviewer.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class NoPresentationRunningException(message: String) : RuntimeException(message)

class OpenSongHttpClient(
    private val client: OkHttpClient,
    private val host: String,
    private val port: Int = 8082
) {

    private fun buildGetRequest(url: String): Request {
        return Request.Builder()
            .url(url)
            .addCommonHeaders()
            .get()
            .build()
    }

    private fun buildPostRequest(url: String): Request {
        return Request.Builder()
            .url(url)
            .addCommonHeaders()
            .post(ByteArray(0).toRequestBody())
            .build()
    }

    private fun Request.Builder.addCommonHeaders(): Request.Builder {
        return this
            .addHeader(
                "User-Agent",
                "Mozilla/5.0 (Android TV; Linux; Android 12) AppleWebKit/537.36 Chrome/120 Safari/537.36"
            )
            .addHeader("Accept", "text/xml,application/xml,text/html;q=0.9,*/*;q=0.8")
            .addHeader("Connection", "keep-alive")
            .addHeader("Cache-Control", "no-cache")
    }

    suspend fun getCurrentSlideXml(): String = withContext(Dispatchers.IO) {
        val url = "http://$host:$port/presentation/slide/current"
        val request = buildGetRequest(url)

        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string() ?: ""

            if (!resp.isSuccessful) {
                if (
                    resp.code == 403 &&
                    body.contains("There is no running presentation", ignoreCase = true)
                ) {
                    throw NoPresentationRunningException(body)
                }

                throw RuntimeException("HTTP ${resp.code}\n$body")
            }

            body
        }
    }

    private suspend fun postAction(path: String): Unit = withContext(Dispatchers.IO) {
        val url = "http://$host:$port$path"
        val request = buildPostRequest(url)

        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                val body = resp.body?.string().orEmpty()
                throw RuntimeException("HTTP ${resp.code}\n$body")
            }
        }
    }

    suspend fun nextSlide() {
        postAction("/presentation/slide/next")
    }

    suspend fun previousSlide() {
        postAction("/presentation/slide/previous")
    }
}