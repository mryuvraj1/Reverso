package dev.reverso

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.net.URL
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection

internal data class DownloadedUpdate(val file: File, val checksumVerified: Boolean, val checksum: String?)

internal object UpdateInstaller {
    private const val MAX_APK_BYTES = 200L * 1024 * 1024

    fun download(context: Context, release: AvailableRelease): DownloadedUpdate {
        val asset = release.apk ?: throw IOException("This release has no compatible Android APK; it is available as source code only")
        if (!asset.name.endsWith(".apk", ignoreCase = true) || !asset.url.startsWith("https://github.com/mryuvraj1/Reverso/releases/download/")) {
            throw IOException("The release APK asset is not from the official Reverso repository")
        }
        if (asset.size <= 0L || asset.size > MAX_APK_BYTES) throw IOException("APK size is invalid or exceeds the 200 MB limit")
        val directory = File(context.cacheDir, "updates").apply { if (!exists() && !mkdirs()) throw IOException("Could not create update cache") }
        val safeTag = release.tag.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val destination = File(directory, "reverso-$safeTag.apk")
        val connection = (URL(asset.url).openConnection() as HttpsURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/octet-stream")
            setRequestProperty("User-Agent", "Reverso-Android")
        }
        try {
            val code = connection.responseCode
            val finalUrl = URL(connection.url.toString())
            if (code !in 200..299 || finalUrl.protocol != "https" || finalUrl.host !in setOf("github.com", "release-assets.githubusercontent.com")) {
                throw IOException("Could not download the official APK (HTTP $code)")
            }
            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            connection.inputStream.use { input -> destination.outputStream().use { output ->
                val buffer = ByteArray(32 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_APK_BYTES) throw IOException("APK exceeded the 200 MB download limit")
                    digest.update(buffer, 0, count)
                    output.write(buffer, 0, count)
                }
            } }
            if (total != asset.size) throw IOException("Downloaded APK size does not match GitHub release metadata")
            val actualDigest = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
            val expectedDigest = asset.digest?.takeIf { it.startsWith("sha256:", ignoreCase = true) }?.substringAfter(':')
            if (asset.digest != null && expectedDigest == null) throw IOException("GitHub provided an unsupported APK checksum format")
            if (expectedDigest != null && !sha256DigestMatches(asset.digest.orEmpty(), actualDigest)) throw IOException("APK SHA-256 verification failed; the file was rejected")
            return DownloadedUpdate(destination, expectedDigest != null, expectedDigest)
        } catch (error: Exception) {
            destination.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    fun installIntent(context: Context, apk: File): Intent {
        require(apk.isFile && apk.extension.equals("apk", ignoreCase = true)) { "No downloaded APK is available" }
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun installPermissionIntent(context: Context): Intent? = if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
        Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
    } else null
}
