package com.example.opensonggoogletvviewer.network

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.time.Duration

object OpenSongDiscovery {

    private const val TAG = "OpenSongDiscovery"

    data class Found(val ip: String)

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun discoverOnLocal24(
        okHttp: OkHttpClient,
        port: Int = 8082,
        concurrency: Int = 200,
        connectTimeoutMs: Int = 250,
        verifyTimeoutMs: Int = 1200,
    ): List<Found> = withContext(Dispatchers.IO) {
        Log.d(TAG, "discoverOnLocal24() started")

        val localIp = getLocalIpv4() ?: run {
            Log.d(TAG, "Local IP not found")
            return@withContext emptyList()
        }

        Log.d(TAG, "Local IP = $localIp")

        val candidates = buildCandidateIps(localIp)
        Log.d(TAG, "Candidate count = ${candidates.size}")
        Log.d(TAG, "Candidates include emulator aliases = ${candidates.any { it == "10.0.2.2" || it == "10.0.3.2" }}")

        val semaphore = Semaphore(concurrency)

        coroutineScope {
            val jobs = candidates.map { ip ->
                async {
                    semaphore.withPermit {
                        if (!isPortOpen(ip, port, connectTimeoutMs)) return@withPermit null
                        Log.d(TAG, "Port $port open on $ip")

                        if (!verifyOpenSong(okHttp, ip, port, verifyTimeoutMs)) {
                            Log.d(TAG, "Verification failed on $ip")
                            return@withPermit null
                        }

                        Log.d(TAG, "OpenSong confirmed on $ip")
                        Found(ip)
                    }
                }
            }

            val results = jobs.awaitAll()
                .filterNotNull()
                .distinctBy { it.ip }
                .sortedBy { it.ip }

            Log.d(TAG, "Discovery finished, found ${results.size} host(s)")
            results
        }
    }

    /**
     * Normal behavior:
     * - scan the device's own /24 subnet
     *
     * Emulator-aware behavior:
     * - also try 10.0.2.2 (host machine from Android emulator)
     * - also try 10.0.3.2 (some emulator setups / variants)
     *
     * This does NOT magically enumerate the host PC's LAN subnet from inside the emulator.
     * It simply adds the known emulator host aliases, which is the useful part for local testing.
     */
    private fun buildCandidateIps(localIp: String): List<String> {
        val result = linkedSetOf<String>()

        val prefix = localIp.substringBeforeLast('.', missingDelimiterValue = "")
        if (prefix.isNotEmpty()) {
            for (last in 1..254) {
                val ip = "$prefix.$last"
                if (ip != localIp) result += ip
            }
            Log.d(TAG, "Scanning local prefix = $prefix.x")
        }

        if (isProbablyAndroidEmulatorSubnet(localIp)) {
            result += "10.0.2.2"
            result += "10.0.3.2"
            Log.d(TAG, "Emulator subnet detected, added host aliases 10.0.2.2 and 10.0.3.2")
        }

        return result.toList()
    }

    private fun isProbablyAndroidEmulatorSubnet(localIp: String): Boolean {
        return localIp.startsWith("10.0.2.") || localIp.startsWith("10.0.3.")
    }

    private fun isPortOpen(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { s ->
                s.soTimeout = timeoutMs
                s.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (_: Throwable) {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun verifyOpenSong(
        okHttp: OkHttpClient,
        ip: String,
        port: Int,
        timeoutMs: Int
    ): Boolean {
        return try {
            val url = "http://$ip:$port/presentation/slide/current"

            val request = Request.Builder()
                .url(url)
                .addHeader(
                    "User-Agent",
                    "Mozilla/5.0 (Android TV; Linux; Android 12) AppleWebKit/537.36 Chrome/120 Safari/537.36"
                )
                .addHeader("Accept", "text/xml,application/xml,text/html;q=0.9,*/*;q=0.8")
                .addHeader("Connection", "keep-alive")
                .addHeader("Cache-Control", "no-cache")
                .get()
                .build()

            val verifyClient = okHttp.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs.toLong()))
                .readTimeout(Duration.ofMillis(timeoutMs.toLong()))
                .callTimeout(Duration.ofMillis(timeoutMs.toLong()))
                .build()

            verifyClient.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()

                if (!resp.isSuccessful) {
                    val isNoPresentation = resp.code == 403 && body.contains("no running presentation", ignoreCase = true)
                    if (isNoPresentation) {
                        Log.d(TAG, "OpenSong confirmed on $ip (no presentation running)")
                        return true
                    }
                    Log.d(TAG, "HTTP ${resp.code} on $ip, body preview=${body.take(120)}")
                    return false
                }

                val looksLikeOpenSong =
                    body.contains("<slides", ignoreCase = true) ||
                            body.contains("<slide", ignoreCase = true) ||
                            body.contains("<title", ignoreCase = true) ||
                            body.contains("<body", ignoreCase = true)

                if (!looksLikeOpenSong) {
                    Log.d(TAG, "Response on $ip did not look like OpenSong, body preview=${body.take(120)}")
                }

                looksLikeOpenSong
            }
        } catch (t: Throwable) {
            Log.d(TAG, "Verification error on $ip: ${t.message}")
            false
        }
    }

    /**
     * Best-effort local IPv4 detection without Wi-Fi-specific APIs.
     */
    private fun getLocalIpv4(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces().toList()
                .asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList().asSequence() }
                .mapNotNull { addr ->
                    val host = addr.hostAddress ?: return@mapNotNull null
                    if (host.contains(":")) return@mapNotNull null
                    if (host.startsWith("169.254.")) return@mapNotNull null
                    host
                }
                .firstOrNull()
        } catch (t: Throwable) {
            Log.d(TAG, "getLocalIpv4() failed: ${t.message}")
            null
        }
    }
}
