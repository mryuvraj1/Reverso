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
            val version = releaseTag.takeIf { parseVersion(it) != null }
                ?: releaseName.takeIf { parseVersion(it) != null }
                ?: return null
            val releaseUrl = json.optString("html_url").trim()
            val releaseUri = runCatching { URL(releaseUrl) }.getOrNull()
            if (releaseUri?.protocol != "https" || releaseUri.host != "github.com") return null
            val current = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
            if (compareVersions(version, current) <= 0) return null
            AvailableRelease(version, json.optString("body").trim().take(1_200), releaseUrl)
        } finally {
            connection.disconnect()
        }
    }

    private data class Version(val major: Int, val minor: Int, val patch: Int, val prerelease: String?)

    private fun parseVersion(value: String): Version? {
        val match = Regex("^[\\s\\-vV]*(\\d+)\\.(\\d+)\\.(\\d+)(?:-([0-9A-Za-z.-]+))?(?:\\+[0-9A-Za-z.-]+)?$")
            .matchEntire(value.trim()) ?: return null
        return Version(
            match.groupValues[1].toIntOrNull() ?: return null,
            match.groupValues[2].toIntOrNull() ?: return null,
            match.groupValues[3].toIntOrNull() ?: return null,
            match.groupValues[4].ifEmpty { null },
        )
    }

    private fun compareVersions(candidate: String, installed: String): Int {
        val left = parseVersion(candidate) ?: return 0
        val right = parseVersion(installed) ?: return 0
        compareValuesBy(left, right, Version::major, Version::minor, Version::patch).takeIf { it != 0 }?.let { return it }
        if (left.prerelease == null) return if (right.prerelease == null) 0 else 1
        if (right.prerelease == null) return -1
        val leftParts = left.prerelease.split('.')
        val rightParts = right.prerelease.split('.')
        for (index in 0 until maxOf(leftParts.size, rightParts.size)) {
            if (index >= leftParts.size) return -1
            if (index >= rightParts.size) return 1
            val a = leftParts[index]
            val b = rightParts[index]
            val aNumber = a.toIntOrNull()
            val bNumber = b.toIntOrNull()
            val compared = when {
                aNumber != null && bNumber != null -> aNumber.compareTo(bNumber)
                aNumber != null -> -1
                bNumber != null -> 1
                else -> a.compareTo(b)
            }
            if (compared != 0) return compared
        }
        return 0
    }
}
