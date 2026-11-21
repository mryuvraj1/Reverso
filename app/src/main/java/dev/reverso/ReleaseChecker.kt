package dev.reverso

import android.content.Context
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

internal data class ReleaseAsset(val name: String, val url: String, val digest: String?, val size: Long)
internal data class AvailableRelease(
    val tag: String,
    val name: String,
    val notes: String,
    val url: String,
    val publishedAt: String,
    val prerelease: Boolean,
    val apk: ReleaseAsset?,
)
internal data class ReleaseCheckResult(val releases: List<AvailableRelease>, val checkedAt: Long, val error: String? = null)

internal fun selectNewerRelease(releases: List<AvailableRelease>, currentVersion: String, includeBeta: Boolean): AvailableRelease? =
    releases.firstOrNull { (!it.prerelease || includeBeta) && compareSemanticVersions(it.tag, currentVersion) > 0 }

internal fun releaseNeedsNotification(candidateTag: String?, lastNotifiedTag: String?): Boolean =
    !candidateTag.isNullOrBlank() && candidateTag != lastNotifiedTag

internal fun sha256DigestMatches(expected: String, actual: String): Boolean =
    expected.startsWith("sha256:", ignoreCase = true) && expected.substringAfter(':').equals(actual, ignoreCase = true)

internal fun releaseCheckFailure(cached: List<AvailableRelease>, checkedAt: Long, message: String) =
    ReleaseCheckResult(cached, checkedAt, message)

internal object UpdatePreferences {
    private const val PREFS = "reverso-updates"
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun automatic(context: Context) = prefs(context).getBoolean("automatic", true)
    fun notifications(context: Context) = prefs(context).getBoolean("notifications", true)
    fun beta(context: Context) = prefs(context).getBoolean("beta", false)
    fun setAutomatic(context: Context, enabled: Boolean) { prefs(context).edit().putBoolean("automatic", enabled).apply(); UpdateScheduler.configure(context) }
    fun setNotifications(context: Context, enabled: Boolean) { prefs(context).edit().putBoolean("notifications", enabled).apply() }
    fun setBeta(context: Context, enabled: Boolean) { prefs(context).edit().putBoolean("beta", enabled).apply(); UpdateScheduler.configure(context) }
    fun lastNotified(context: Context) = prefs(context).getString("last_notified", null)
    fun markNotified(context: Context, tag: String) { prefs(context).edit().putString("last_notified", tag).apply() }
    fun isRemindedLater(context: Context, tag: String) = prefs(context).getString("remind_later", null) == tag
    fun remindLater(context: Context, tag: String) { prefs(context).edit().putString("remind_later", tag).apply() }
}

/** Reads only the official public repository's GitHub Releases REST API; no token is used. */
internal object ReleaseChecker {
    private const val API_URL = "https://api.github.com/repos/mryuvraj1/Reverso/releases?per_page=100"
    private const val PREFS = "reverso-release-cache"
    private const val CACHE_KEY = "releases"
    private const val CHECKED_KEY = "checked_at"
    private const val MAX_RESPONSE_BYTES = 1_500_000
    private const val CACHE_TTL_MS = 6 * 60 * 60 * 1000L
    private const val MIN_REQUEST_INTERVAL_MS = 60 * 1000L

    fun currentVersion(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }.getOrDefault("0.0.0")

    fun history(context: Context): List<AvailableRelease> = runCatching {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CACHE_KEY, "[]")
        decodeCachedReleases(JSONArray(json))
    }.getOrDefault(emptyList())

    fun lastChecked(context: Context): Long = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(CHECKED_KEY, 0L)

    fun check(context: Context, includeBeta: Boolean = false, force: Boolean = false): ReleaseCheckResult {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val cached = history(context)
        val checkedAt = lastChecked(context)
        val elapsed = System.currentTimeMillis() - checkedAt
        if (checkedAt > 0 && (elapsed < MIN_REQUEST_INTERVAL_MS || (!force && elapsed < CACHE_TTL_MS))) {
            return ReleaseCheckResult(cached, checkedAt)
        }
        return try {
            val connection = (URL(API_URL).openConnection() as HttpsURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 12_000
                instanceFollowRedirects = false
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Reverso-Android")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            }
            val releases = try {
                val code = connection.responseCode
                if (code != HttpsURLConnection.HTTP_OK) {
                    val reset = connection.getHeaderField("X-RateLimit-Reset")?.toLongOrNull()
                    val retryAt = reset?.let { java.time.Instant.ofEpochSecond(it).toString() }
                    throw IOException(if (code == 403 && reset != null) "GitHub API rate limit reached; retry after $retryAt" else "GitHub Releases API returned HTTP $code")
                }
                val bytes = connection.inputStream.use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8_192)
                    var total = 0
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > MAX_RESPONSE_BYTES) throw IOException("Release metadata exceeded the size limit")
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
                decodeApiReleases(JSONArray(bytes.toString(Charsets.UTF_8)))
            } finally { connection.disconnect() }
            val now = System.currentTimeMillis()
            prefs.edit().putString(CACHE_KEY, encodeReleases(releases)).putLong(CHECKED_KEY, now).apply()
            UpdateNotification.notifyForNewRelease(context, selectNewer(context, releases, includeBeta))
            ReleaseCheckResult(releases, now)
        } catch (error: Exception) {
            releaseCheckFailure(cached, checkedAt, error.message ?: "Could not check GitHub Releases")
        }
    }

    fun latestNewer(context: Context, releases: List<AvailableRelease>, includeBeta: Boolean): AvailableRelease? =
        selectNewer(context, releases, includeBeta)

    fun latestPublished(releases: List<AvailableRelease>, includeBeta: Boolean): AvailableRelease? =
        releases.firstOrNull { !it.prerelease || includeBeta }

    private fun selectNewer(context: Context, releases: List<AvailableRelease>, includeBeta: Boolean): AvailableRelease? {
        val current = currentVersion(context)
        return selectNewerRelease(releases, current, includeBeta)
    }

    private fun decodeApiReleases(array: JSONArray): List<AvailableRelease> = (0 until array.length()).mapNotNull { index ->
        val item = array.optJSONObject(index) ?: return@mapNotNull null
        if (item.optBoolean("draft", false)) return@mapNotNull null
        val tag = item.optString("tag_name").trim()
        if (parseSemanticVersion(tag) == null) return@mapNotNull null
        val releaseUrl = officialReleaseUrl(item.optString("html_url")) ?: return@mapNotNull null
        val assets = item.optJSONArray("assets") ?: JSONArray()
        val apk = chooseApkAsset(assets)
        AvailableRelease(
            tag = tag,
            name = item.optString("name").takeIf(String::isNotBlank) ?: tag,
            notes = item.optString("body").trim().take(12_000),
            url = releaseUrl,
            publishedAt = item.optString("published_at"),
            prerelease = item.optBoolean("prerelease", false),
            apk = apk,
        )
    }

    private fun officialReleaseUrl(value: String): String? = runCatching {
        URL(value).takeIf { it.protocol == "https" && it.host.equals("github.com", ignoreCase = true) && it.path.startsWith("/mryuvraj1/Reverso/releases/") }?.toString()
    }.getOrNull()

    private fun chooseApkAsset(assets: JSONArray): ReleaseAsset? {
        val candidates = (0 until assets.length()).mapNotNull { assets.optJSONObject(it) }
            .filter { it.optString("state") == "uploaded" && it.optString("name").endsWith(".apk", ignoreCase = true) }
            .mapNotNull { item ->
                val name = item.optString("name")
                val url = officialAssetUrl(item.optString("browser_download_url")) ?: return@mapNotNull null
                ReleaseAsset(name, url, item.optString("digest").takeIf(String::isNotBlank), item.optLong("size"))
            }
        if (candidates.isEmpty()) return null
        return candidates.firstOrNull { it.name.contains("universal", true) }
            ?: candidates.firstOrNull { asset -> Build.SUPPORTED_ABIS.any { asset.name.contains(it, true) } }
            ?: candidates.singleOrNull()
    }

    private fun officialAssetUrl(value: String): String? = runCatching {
        URL(value).takeIf { it.protocol == "https" && it.host.equals("github.com", ignoreCase = true) && it.path.startsWith("/mryuvraj1/Reverso/releases/download/") }?.toString()
    }.getOrNull()

    private fun encodeReleases(releases: List<AvailableRelease>) = JSONArray().apply {
        releases.forEach { release -> put(JSONObject().put("tag", release.tag).put("name", release.name).put("notes", release.notes).put("url", release.url).put("published", release.publishedAt).put("prerelease", release.prerelease).put("apk", release.apk?.let { JSONObject().put("name", it.name).put("url", it.url).put("digest", it.digest).put("size", it.size) })) }
    }.toString()

    private fun decodeCachedReleases(json: JSONArray): List<AvailableRelease> = (0 until json.length()).mapNotNull { index ->
        val item = json.optJSONObject(index) ?: return@mapNotNull null
        val tag = item.optString("tag")
        val url = officialReleaseUrl(item.optString("url")) ?: return@mapNotNull null
        val apkJson = item.optJSONObject("apk")
        val apk = apkJson?.let { ReleaseAsset(it.optString("name"), officialAssetUrl(it.optString("url")) ?: return@let null, it.optString("digest").takeIf(String::isNotBlank), it.optLong("size")) }
        AvailableRelease(tag, item.optString("name", tag), item.optString("notes"), url, item.optString("published"), item.optBoolean("prerelease"), apk)
    }
}
