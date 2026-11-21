package dev.reverso

import android.annotation.SuppressLint
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

internal object UpdateScheduler {
    private const val PERIODIC_NAME = "reverso-github-release-check"

    fun configure(context: Context) {
        val work = WorkManager.getInstance(context)
        if (!UpdatePreferences.automatic(context)) {
            work.cancelUniqueWork(PERIODIC_NAME)
            return
        }
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request = PeriodicWorkRequestBuilder<ReleaseCheckWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        work.enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

}

internal class ReleaseCheckWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        if (!UpdatePreferences.automatic(applicationContext)) return Result.success()
        return try {
            val result = ReleaseChecker.check(applicationContext, UpdatePreferences.beta(applicationContext), force = true)
            if (result.error != null && result.releases.isEmpty()) Result.retry() else Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

internal object UpdateNotification {
    private const val CHANNEL_ID = "reverso_updates"
    private const val NOTIFICATION_ID = 4102

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Reverso Updates", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Notifications about new Reverso releases"
        })
    }

    fun notificationsAllowed(context: Context): Boolean {
        if (!UpdatePreferences.notifications(context) || !NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission") // notificationsAllowed checks app preference, system state, and POST_NOTIFICATIONS before this call.
    fun notifyForNewRelease(context: Context, release: AvailableRelease?) {
        if (release == null || !notificationsAllowed(context) || !releaseNeedsNotification(release.tag, UpdatePreferences.lastNotified(context))) return
        ensureChannel(context)
        val pending = PendingIntent.getActivity(
            context, NOTIFICATION_ID, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_update_notification)
            .setContentTitle("Reverso ${release.tag} is available")
            .setContentText("Tap to view release notes and update options")
            .setStyle(NotificationCompat.BigTextStyle().bigText(release.notes.take(240)))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
            .onSuccess { UpdatePreferences.markNotified(context, release.tag) }
    }
}
