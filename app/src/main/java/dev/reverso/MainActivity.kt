package dev.reverso

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalUriHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import java.io.IOException

private val Ink = Color(0xFFF4F6FC)
private val Base = Color(0xFF0B0F19)
private val Panel = Color(0xFF151B2B)
private val Raised = Color(0xFF202B3D)
private val Violet = Color(0xFF7C8CFF)
private val Muted = Color(0xFFA0ABC0)
private val Accent = Color(0xFF00D6A3)

private data class EditorTab(
    val id: String,
    val title: String,
    val uri: Uri? = null,
    val text: String = "",
    val dirty: Boolean = false,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReversoApp() }
    }
}

@Composable
private fun ReversoApp() {
    val context = LocalContext.current
    val tabs = remember { mutableStateListOf(EditorTab("welcome", "Welcome", text = welcomeText)) }
    var activeId by remember { mutableStateOf("welcome") }
    var status by remember { mutableStateOf("Ready · Local editing") }
    var showFind by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf("") }
    var availableRelease by remember { mutableStateOf<AvailableRelease?>(null) }
    val uriHandler = LocalUriHandler.current
    val active = tabs.firstOrNull { it.id == activeId }
    LaunchedEffect(Unit) {
        availableRelease = withContext(Dispatchers.IO) { runCatching { ReleaseChecker.latest(context) }.getOrNull() }
    }
    val editorStyle = TextStyle(color = Ink, fontFamily = FontFamily.Monospace, fontSize = 14.sp, lineHeight = 21.sp)

    fun update(tab: EditorTab) {
        val index = tabs.indexOfFirst { it.id == tab.id }
        if (index >= 0) tabs[index] = tab
    }

    fun readDocument(uri: Uri, displayName: String) {
        runCatching {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: throw IOException("The selected document could not be opened")
            val tab = EditorTab(uri.toString(), displayName, uri, text)
            val existing = tabs.indexOfFirst { it.id == tab.id }
            if (existing >= 0) tabs[existing] = tab else tabs.add(tab)
            activeId = tab.id
            status = "Opened $displayName"
        }.onFailure { status = "Open failed: ${it.message ?: "unknown error"}" }
    }

    fun saveDocument(tab: EditorTab, uri: Uri? = tab.uri) {
        val destination = uri ?: run { status = "Choose a location to save this document"; return }
        runCatching {
            context.contentResolver.openOutputStream(destination, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(tab.text) }
                ?: throw IOException("The selected document could not be written")
            update(tab.copy(uri = destination, dirty = false))
            status = "Saved ${tab.title}"
        }.onFailure { status = "Save failed: ${it.message ?: "unknown error"}" }
    }

    val openPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val name = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')?.takeIf { it.isNotBlank() } ?: "document.txt"
            readDocument(uri, name)
        }
    }
    val savePicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val tab = tabs.firstOrNull { it.id == activeId }
        if (uri != null && tab != null) saveDocument(tab, uri)
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Violet, secondary = Accent, background = Base, surface = Panel, onSurface = Ink)) {
        Column(
            Modifier.fillMaxSize().background(Base).onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.isCtrlPressed && event.key == Key.S) {
                    active?.let { if (it.uri == null) savePicker.launch(it.title) else saveDocument(it) }
                    true
                } else if (event.type == KeyEventType.KeyDown && event.isCtrlPressed && event.key == Key.F) {
                    showFind = true
                    true
                } else false
            }.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("REVERSO", style = MaterialTheme.typography.titleLarge, color = Ink)
                    Text("CODE ANYWHERE · BUILD EVERYTHING", style = MaterialTheme.typography.labelSmall, color = Muted, letterSpacing = 1.sp)
                }
                Text("●", color = Accent, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val tab = EditorTab("new-${System.nanoTime()}", "untitled.txt")
                    tabs.add(tab); activeId = tab.id; status = "New document"
                }) { Text("New") }
                OutlinedButton(onClick = { openPicker.launch(arrayOf("text/*", "application/json", "application/xml")) }) { Text("Open") }
                Button(enabled = active != null, onClick = { active?.let { if (it.uri == null) savePicker.launch(it.title) else saveDocument(it) } }) { Text("Save") }
                OutlinedButton(onClick = { showFind = true }) { Text("Find") }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tabs.forEach { tab ->
                    val selected = tab.id == activeId
                    TextButton(onClick = { activeId = tab.id }, colors = ButtonDefaults.textButtonColors(containerColor = if (selected) Raised else Color.Transparent)) {
                        Text((if (tab.dirty) "● " else "") + tab.title, color = if (selected) Ink else Muted, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().background(Panel).padding(horizontal = 10.dp, vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(active?.title ?: "No document", color = Violet, style = MaterialTheme.typography.labelMedium)
                Text(status, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            Spacer(Modifier.height(1.dp))
            if (active != null) {
                BasicTextField(
                    value = active.text,
                    onValueChange = { update(active.copy(text = it, dirty = true)) },
                    modifier = Modifier.fillMaxWidth().weight(1f).background(Panel).padding(12.dp),
                    textStyle = editorStyle,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    keyboardActions = KeyboardActions(default = {}),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxSize()) {
                            if (active.text.isEmpty()) Text("Start writing…", color = Muted, style = editorStyle)
                            inner()
                        }
                    },
                )
            }
        }
    }

    availableRelease?.let { release ->
        AlertDialog(
            onDismissRequest = { availableRelease = null },
            title = { Text("Reverso ${release.tag} is available") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("A newer version has been published.")
                    if (release.notes.isNotBlank()) Text(release.notes, maxLines = 8, color = Muted)
                    Text("The release page will show download and installation instructions.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    runCatching { uriHandler.openUri(release.url) }
                    availableRelease = null
                }) { Text("View release") }
            },
            dismissButton = { TextButton(onClick = { availableRelease = null }) { Text("Later") } },
        )
    }

    if (showFind) {
        AlertDialog(
            onDismissRequest = { showFind = false },
            title = { Text("Find and replace") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(query, { query = it }, label = { Text("Find") }, singleLine = true)
                    OutlinedTextField(replacement, { replacement = it }, label = { Text("Replace with") }, singleLine = true)
                    val matches = if (query.isEmpty()) 0 else Regex(Regex.escape(query)).findAll(active?.text.orEmpty()).count()
                    Text("$matches match${if (matches == 1) "" else "es"}", color = Muted, style = MaterialTheme.typography.labelMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    active?.let { tab ->
                        if (query.isNotEmpty()) {
                            val count = Regex(Regex.escape(query)).findAll(tab.text).count()
                            update(tab.copy(text = tab.text.replace(query, replacement), dirty = tab.dirty || count > 0))
                            status = "Replaced $count occurrence${if (count == 1) "" else "s"}"
                        }
                    }
                    showFind = false
                }) { Text("Replace all") }
            },
            dismissButton = { TextButton(onClick = { showFind = false }) { Text("Close") } },
        )
    }
}

private val welcomeText = """// Welcome to Reverso
// Create a document or open a text file to begin.
// Ctrl+S saves · Ctrl+F finds and replaces

"""
