package dev.reverso.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import dev.reverso.AvailableRelease
import dev.reverso.compareSemanticVersions
import java.text.DateFormat
import java.util.Date

@Composable
internal fun UpdateSettingsScreen(
    currentVersion: String,
    latestVersion: String?,
    history: List<AvailableRelease>,
    checkedAt: Long,
    error: String?,
    checking: Boolean,
    automatic: Boolean,
    beta: Boolean,
    notifications: Boolean,
    notificationPermission: Boolean,
    onAutomatic: (Boolean) -> Unit,
    onBeta: (Boolean) -> Unit,
    onNotifications: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onCheck: () -> Unit,
    onDownload: (AvailableRelease) -> Unit,
    onViewRelease: (AvailableRelease) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Release status", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onCheck, enabled = !checking) { Text(if (checking) "Checking…" else "Check for Updates") }
        }
        Text("Current Installed Version: $currentVersion", color = ReversoColors.Ink, style = MaterialTheme.typography.bodySmall)
        Text("Latest Available Version: ${latestVersion ?: if (checkedAt <= 0) "Not checked" else "No release found"}", color = ReversoColors.Ink, style = MaterialTheme.typography.bodySmall)
        val latest = history.firstOrNull { it.tag == latestVersion }
        latest?.let { release ->
            if (compareSemanticVersions(release.tag, currentVersion) <= 0) Text("Reverso is up to date on this channel.", color = ReversoColors.Muted, style = MaterialTheme.typography.bodySmall)
            else if (release.apk != null) Button(onClick = { onDownload(release) }, modifier = Modifier.fillMaxWidth()) { Text("Download ${release.apk.name}") }
            else Text("No compatible APK is attached to this release; it is source-only.", color = ReversoColors.Muted, style = MaterialTheme.typography.bodySmall)
        }
        Text("Last checked: ${if (checkedAt <= 0) "Never" else DateFormat.getDateTimeInstance().format(Date(checkedAt))}", color = ReversoColors.Muted, style = MaterialTheme.typography.bodySmall)
        error?.let { Text(it, color = ReversoColors.Warning, style = MaterialTheme.typography.bodySmall) }
        PreferenceSwitch("Automatically check for updates", automatic, onAutomatic)
        Text("Release channel", color = ReversoColors.Muted, style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !beta, onClick = { onBeta(false) }, label = { Text("Stable") })
            FilterChip(selected = beta, onClick = { onBeta(true) }, label = { Text("Stable + Beta") })
        }
        PreferenceSwitch("Update notifications", notifications, onNotifications)
        if (notifications && !notificationPermission) {
            OutlinedButton(onClick = onRequestNotificationPermission, modifier = Modifier.fillMaxWidth()) { Text("Allow Android notifications") }
        }
        Text("Release history", style = MaterialTheme.typography.labelLarge, color = ReversoColors.Muted)
        if (history.isEmpty()) Text("Release history appears after a successful GitHub check.", color = ReversoColors.Muted, style = MaterialTheme.typography.bodySmall)
        history.forEach { release ->
            Surface(color = ReversoColors.Background, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.SystemUpdate, null, tint = ReversoColors.Blue)
                    Column(Modifier.weight(1f)) {
                        Text(release.name, color = ReversoColors.Ink, maxLines = 1)
                        Text("${release.publishedAt.take(10)} · ${if (release.prerelease) "Beta" else "Stable"} · ${if (release.apk == null) "Source only" else release.apk.name}", color = ReversoColors.Muted, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                    }
                    TextButton(onClick = { onViewRelease(release) }) { Text("View") }
                }
            }
        }
        OutlinedButton(onClick = { runCatching { uriHandler.openUri("https://github.com/mryuvraj1/Reverso/releases") } }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.OpenInNew, null, tint = ReversoColors.Blue)
            Text("  View on GitHub")
        }
    }
}

@Composable
private fun PreferenceSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f), color = ReversoColors.Ink, style = MaterialTheme.typography.bodySmall)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
