package dev.reverso

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Checks the public GitHub Releases feed and returns a release newer than this install. */
data class AvailableRelease(val tag: String, val notes: String, val url: String)

object ReleaseChecker {
    private const val API_URL = "https://api.github.com/repos/mryuvraj1/Reverso/releases/latest"
    private const val MAX_RESPONSE_BYTES = 1_000_000

    fun latest(context: Context): AvailableRelease? {
        val connection = (URL(API_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Reverso-Android")
            setRequestProperty("X-GitHub-Api-Version", "2026-03-10")
        }
        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val bytes = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8_192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_RESPONSE_BYTES) { "Release metadata exceeded the size limit" }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            val json = JSONObject(bytes.toString(Charsets.UTF_8))
            val releaseTag = json.optString("tag_name").trim()
            val releaseName = json.optString("name").trim()
            val version = releaseTag.takeIf { parseSemanticVersion(it) != null }
                ?: releaseName.takeIf { parseSemanticVersion(it) != null }
                ?: return null
            val releaseUrl = json.optString("html_url").trim()
            val releaseUri = runCatching { URL(releaseUrl) }.getOrNull()
            if (releaseUri?.protocol != "https" || releaseUri.host != "github.com") return null
            val current = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
            if (compareSemanticVersions(version, current) <= 0) return null
            AvailableRelease(version, json.optString("body").trim().take(1_200), releaseUrl)
        } finally {
            connection.disconnect()
        }
    }


}
