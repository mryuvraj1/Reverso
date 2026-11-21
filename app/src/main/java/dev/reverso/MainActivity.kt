package dev.reverso

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import dev.reverso.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReversoTheme { ReversoApp() } }
    }
}

@Composable
private fun ReversoApp() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val tabs = remember { mutableStateListOf<EditorTab>() }
    var activeId by remember { mutableStateOf<String?>(null) }
    var activity by remember { mutableStateOf(IdeActivity.EXPLORER) }
    var bottomTool by remember { mutableStateOf(BottomTool.TERMINAL) }
    var bottomOpen by remember { mutableStateOf(false) }
    var bottomHeight by remember { mutableFloatStateOf(190f) }
    var workspace by remember { mutableStateOf<ProjectWorkspace?>(null) }
    val expandedFolders = remember { mutableStateListOf<String>() }
    var recents by remember { mutableStateOf(RecentProjects.load(context)) }
    var status by remember { mutableStateOf("Ready · Local editing") }
    var output by remember { mutableStateOf(listOf("Reverso is ready. Open a folder or create a document.")) }
    var query by remember { mutableStateOf("") }
    var findDialog by remember { mutableStateOf(false) }
    var replaceText by remember { mutableStateOf("") }
    var createDialog by remember { mutableStateOf(false) }
    var createFolder by remember { mutableStateOf(false) }
    var createName by remember { mutableStateOf("") }
    var newProjectDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    var pendingProjectName by remember { mutableStateOf<String?>(null) }
    var pendingRelease by remember { mutableStateOf<AvailableRelease?>(null) }
    var splitMode by remember { mutableStateOf(false) }
    val active = tabs.firstOrNull { it.id == activeId }
    val treeItems = remember(workspace, workspace?.root, expandedFolders.toList()) {
        workspace?.let { listWorkspaceItems(it.root, expandedFolders.toSet()) }.orEmpty()
    }

    LaunchedEffect(Unit) {
        pendingRelease = withContext(Dispatchers.IO) { runCatching { ReleaseChecker.latest(context) }.getOrNull() }
    }

    fun log(message: String) { output = (output + message).takeLast(100) }
    fun updateTab(tab: EditorTab) {
        val index = tabs.indexOfFirst { it.id == tab.id }
        if (index >= 0) tabs[index] = tab
    }
    fun addTab(tab: EditorTab) {
        val existing = tabs.indexOfFirst { it.uri != null && it.uri == tab.uri }
        if (existing >= 0) activeId = tabs[existing].id else { tabs.add(tab); activeId = tab.id }
        activity = IdeActivity.EXPLORER
    }
    fun openUri(uri: Uri, title: String) {
        runCatching {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: throw IOException("The selected document could not be read")
            addTab(EditorTab(uri.toString(), title, uri, TextFieldValue(text)))
            status = "Opened $title"; log("Opened $title")
        }.onFailure { status = "Open failed: ${it.message ?: "unknown error"}" }
    }
    fun saveTab(tab: EditorTab, destination: Uri? = tab.uri) {
        if (destination == null) { status = "Choose a location to save this file"; return }
        runCatching {
            context.contentResolver.openOutputStream(destination, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(tab.value.text) }
                ?: throw IOException("The selected document could not be written")
            val updated = tab.copy(uri = destination, dirty = false)
            updateTab(updated); status = "Saved ${tab.title}"; log("Saved ${tab.title}")
        }.onFailure { status = "Save failed: ${it.message ?: "unknown error"}" }
    }

    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) openUri(uri, uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')?.takeIf(String::isNotBlank) ?: "untitled.txt")
    }
    val openFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            val selectedRoot = DocumentFile.fromTreeUri(context, uri) ?: error("Folder access was not granted")
            val projectName = pendingProjectName
            val root = if (projectName != null) selectedRoot.createDirectory(projectName) ?: error("Could not create project folder") else selectedRoot
            val recentUri = root.uri
            workspace = ProjectWorkspace(recentUri, root, root.name ?: "Workspace")
            val recent = RecentProject(root.name ?: "Workspace", recentUri.toString())
            RecentProjects.remember(context, recent); recents = RecentProjects.load(context)
            activity = IdeActivity.EXPLORER; status = if (projectName == null) "Workspace opened: ${recent.name}" else "Project created: ${recent.name}"; log(status)
            pendingProjectName = null; newProjectName = ""
        }.onFailure { status = "Folder open failed: ${it.message ?: "unknown error"}"; pendingProjectName = null }
        else pendingProjectName = null
    }
    val saveDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) active?.let { saveTab(it, uri) }
    }

    if (newProjectDialog) AlertDialog(
        onDismissRequest = { newProjectDialog = false }, title = { Text("Create project") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(newProjectName, { newProjectName = it }, label = { Text("Project folder name") }, singleLine = true)
            Text("Choose a parent folder next. Reverso will create the project folder there.", color = ReversoColors.Muted, style = MaterialTheme.typography.bodySmall)
        } },
        confirmButton = { TextButton(onClick = {
            val name = newProjectName.trim().replace('/', '-').replace('\\', '-')
            if (name.isNotEmpty()) { pendingProjectName = name; newProjectDialog = false; openFolder.launch(null) }
        }) { Text("Choose Location") } },
        dismissButton = { TextButton(onClick = { newProjectDialog = false }) { Text("Cancel") } },
    )

    if (findDialog) AlertDialog(
        onDismissRequest = { findDialog = false }, title = { Text("Find and replace") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(query, { query = it }, label = { Text("Find") }, singleLine = true)
            OutlinedTextField(replaceText, { replaceText = it }, label = { Text("Replace with") }, singleLine = true)
            Text("${if (query.isBlank()) 0 else Regex(Regex.escape(query)).findAll(active?.value?.text.orEmpty()).count()} matches", color = ReversoColors.Muted)
        } },
        confirmButton = { TextButton(onClick = {
            active?.let { tab -> if (query.isNotEmpty()) { val count = Regex(Regex.escape(query)).findAll(tab.value.text).count(); updateTab(tab.copy(value = TextFieldValue(tab.value.text.replace(query, replaceText)), dirty = tab.dirty || count > 0)); status = "Replaced $count occurrence(s)" } }
            findDialog = false
        }) { Text("Replace all") } },
        dismissButton = { TextButton(onClick = { findDialog = false }) { Text("Close") } },
    )

    if (createDialog) AlertDialog(
        onDismissRequest = { createDialog = false },
        title = { Text(if (createFolder) "New folder" else "New file") },
        text = { OutlinedTextField(createName, { createName = it }, label = { Text("Name") }, singleLine = true) },
        confirmButton = { TextButton(onClick = {
            val name = createName.trim()
            if (name.isNotEmpty()) {
                if (workspace == null) {
                    if (!createFolder) { val tab = EditorTab("new-${System.nanoTime()}", name); tabs.add(tab); activeId = tab.id; status = "Created $name" }
                    else status = "Open a workspace before creating a folder"
                } else runCatching {
                    val file = if (createFolder) workspace!!.root.createDirectory(name) else workspace!!.root.createFile(mimeFor(name), name)
                    if (file == null) error("Could not create $name")
                    if (!createFolder) addTab(EditorTab(file.uri.toString(), name, file.uri, TextFieldValue("")))
                    status = "Created $name"; log("Created $name")
                }.onFailure { status = "Create failed: ${it.message ?: "unknown error"}" }
            }
            createDialog = false; createName = ""
        }) { Text("Create") } },
        dismissButton = { TextButton(onClick = { createDialog = false }) { Text("Cancel") } },
    )

    BoxWithConstraints(Modifier.fillMaxSize().background(ReversoColors.Background).onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown && event.isCtrlPressed && event.key == Key.S) { active?.let { if (it.uri == null) saveDocument.launch(it.title) else saveTab(it) }; true }
        else if (event.type == KeyEventType.KeyDown && event.isCtrlPressed && event.key == Key.F) { findDialog = true; true }
        else false
    }) {
        val wide = maxWidth >= 720.dp
        Column(Modifier.fillMaxSize()) {
            Header(active?.title, onHome = { activity = IdeActivity.EXPLORER; activeId = null }, onOpen = { openDocument.launch(arrayOf("text/*", "application/json", "application/xml", "application/octet-stream")) }, onSave = { active?.let { if (it.uri == null) saveDocument.launch(it.title) else saveTab(it) } }, onFind = { findDialog = true }, onSplit = { if (wide && tabs.size > 1) splitMode = !splitMode else status = "Split view needs a second open file and a wide screen" }, onTerminal = { bottomTool = BottomTool.TERMINAL; bottomOpen = !bottomOpen })
            if (wide) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    ActivityRail(activity) { activity = it }
                    if (activity == IdeActivity.EXPLORER || activity == IdeActivity.SEARCH) {
                        SidePanel(activity, workspace?.name, treeItems, tabs, activeId, query, { query = it },
                            onOpenFolder = { openFolder.launch(null) }, onOpenFile = { openDocument.launch(arrayOf("text/*", "application/json", "application/xml", "application/octet-stream")) },
                            onCreateFile = { createFolder = false; createDialog = true }, onCreateFolder = { createFolder = true; createDialog = true },
                            onFile = { if (it.isDirectory) { if (it.id in expandedFolders) expandedFolders.remove(it.id) else expandedFolders.add(it.id) } else openUri(it.uri, it.name) }, onTab = { activeId = it.id; activity = IdeActivity.EXPLORER }, modifier = Modifier.width(270.dp))
                    } else ActivityPage(activity, workspace?.name, recents, onOpenFolder = { openFolder.launch(null) }, onOpenDocument = { openDocument.launch(arrayOf("text/*", "application/json", "application/xml")) }, onRecent = { RecentProjects.open(context, it)?.let { ws -> workspace = ws; activity = IdeActivity.EXPLORER } })
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        if (activity == IdeActivity.EXPLORER || activity == IdeActivity.SEARCH) EditorArea(active, tabs, activeId, splitMode, workspace?.name, recents,
                            onSelect = { activeId = it.id }, onClose = { tabs.remove(it); if (activeId == it.id) activeId = tabs.lastOrNull()?.id }, onValue = ::updateTab,
                            onWorkspace = { activity = IdeActivity.EXPLORER }, onInsert = { active?.let { tab -> updateTab(insertAtSelection(tab, it)) } }, onWelcome = { activeId = null },
                            onNew = { newProjectDialog = true },
                            onOpen = { openDocument.launch(arrayOf("text/*", "application/json", "application/xml", "application/octet-stream")) }, onFolder = { openFolder.launch(null) }, onRecent = { RecentProjects.open(context, it)?.let { ws -> workspace = ws; activity = IdeActivity.EXPLORER } }, onDocs = { runCatching { uriHandler.openUri("https://github.com/mryuvraj1/Reverso/tree/main/project-knowledge") } })
                        else ActivityPage(activity, workspace?.name, recents, onOpenFolder = { openFolder.launch(null) }, onOpenDocument = { openDocument.launch(arrayOf("text/*", "application/json", "application/xml")) }, onRecent = { RecentProjects.open(context, it)?.let { ws -> workspace = ws; activity = IdeActivity.EXPLORER } })
                        if (bottomOpen) BottomPanel(bottomTool, output, Modifier.height(bottomHeight.dp), onTool = { bottomTool = it }, onClose = { bottomOpen = false }, onDrag = { delta -> bottomHeight = (bottomHeight - delta).coerceIn(110f, 420f) })
                    }
                }
            } else {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (activity) {
                        IdeActivity.EXPLORER -> if (active == null) WelcomeScreen(recents, onNew = { newProjectDialog = true }, onOpen = { openDocument.launch(arrayOf("text/*", "application/json", "application/xml", "application/octet-stream")) }, onFolder = { openFolder.launch(null) }, onRecent = { RecentProjects.open(context, it)?.let { ws -> workspace = ws; status = "Workspace opened: ${ws.name}" } }, onDocs = { runCatching { uriHandler.openUri("https://github.com/mryuvraj1/Reverso/tree/main/project-knowledge") } })
                            else EditorArea(active, tabs, activeId, false, workspace?.name, recents, onSelect = { activeId = it.id }, onClose = { tabs.remove(it); if (activeId == it.id) activeId = tabs.lastOrNull()?.id }, onValue = ::updateTab, onWorkspace = { activity = IdeActivity.EXPLORER }, onInsert = { active?.let { tab -> updateTab(insertAtSelection(tab, it)) } }, onWelcome = { activeId = null }, onNew = { newProjectDialog = true }, onOpen = { openDocument.launch(arrayOf("text/*", "application/json", "application/xml", "application/octet-stream")) }, onFolder = { openFolder.launch(null) }, onRecent = { RecentProjects.open(context, it)?.let { ws -> workspace = ws } }, onDocs = { runCatching { uriHandler.openUri("https://github.com/mryuvraj1/Reverso/tree/main/project-knowledge") } })
                        IdeActivity.SEARCH -> SearchPage(query, { query = it }, tabs, onSelect = { activeId = it.id; activity = IdeActivity.EXPLORER })
                        else -> ActivityPage(activity, workspace?.name, recents, onOpenFolder = { openFolder.launch(null) }, onOpenDocument = { openDocument.launch(arrayOf("text/*", "application/json", "application/xml")) }, onRecent = { RecentProjects.open(context, it)?.let { ws -> workspace = ws; activity = IdeActivity.EXPLORER } })
                    }
                    if (bottomOpen && active != null && activity == IdeActivity.EXPLORER) Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) { BottomPanel(bottomTool, output, Modifier.height(bottomHeight.dp.coerceAtMost(maxHeight * 0.58f)), onTool = { bottomTool = it }, onClose = { bottomOpen = false }, onDrag = { delta -> bottomHeight = (bottomHeight - delta).coerceIn(110f, maxHeight.value * .7f) }) }
                }
            }
            if (active != null) StatusBar(active, status, onPanel = { bottomTool = it; bottomOpen = true })
            if (!wide) MobileActivityBar(activity, { activity = it })
        }
    }

    pendingRelease?.let { release -> AlertDialog(onDismissRequest = { pendingRelease = null }, title = { Text("Reverso ${release.tag} is available") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("A newer version has been published."); if (release.notes.isNotBlank()) Text(release.notes, maxLines = 8, color = ReversoColors.Muted); Text("The release page has download and installation instructions.", color = ReversoColors.Muted, style = MaterialTheme.typography.bodySmall) } },
        confirmButton = { TextButton(onClick = { runCatching { uriHandler.openUri(release.url) }; pendingRelease = null }) { Text("View release") } },
        dismissButton = { TextButton(onClick = { pendingRelease = null }) { Text("Later") } }) }
}

private fun mimeFor(name: String) = when (name.substringAfterLast('.', "").lowercase()) { "kt", "rs", "go", "ts", "js", "py", "sh", "txt", "md" -> "text/plain"; "json" -> "application/json"; "xml" -> "application/xml"; else -> "application/octet-stream" }

@Composable private fun Header(title: String?, onHome: () -> Unit, onOpen: () -> Unit, onSave: () -> Unit, onFind: () -> Unit, onSplit: () -> Unit, onTerminal: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(58.dp).background(ReversoColors.Surface).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        ReversoMark(36.dp, Modifier.clickable(onClick = onHome)); Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f).clickable(onClick = onHome)) { Text("REVERSO", style = MaterialTheme.typography.titleMedium, letterSpacing = 1.3.sp); Text(title ?: "Code anywhere · Build everything", color = ReversoColors.Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
        IconButton(onClick = onOpen) { Icon(Icons.Filled.FolderOpen, "Open file", tint = ReversoColors.Muted) }
        IconButton(onClick = onSave) { Icon(Icons.Filled.Save, "Save", tint = ReversoColors.Muted) }
        IconButton(onClick = onFind) { Icon(Icons.Filled.Search, "Find", tint = ReversoColors.Muted) }
        IconButton(onClick = onSplit) { Icon(Icons.Filled.CallSplit, "Split editor", tint = ReversoColors.Muted) }
        IconButton(onClick = onTerminal) { Icon(Icons.Filled.Terminal, "Terminal panel", tint = ReversoColors.Muted) }
    }
}

@Composable private fun ActivityRail(selected: IdeActivity, onSelect: (IdeActivity) -> Unit) {
    Column(Modifier.width(54.dp).fillMaxHeight().background(ReversoColors.Surface), horizontalAlignment = Alignment.CenterHorizontally) {
        IdeActivity.entries.forEach { item -> val icon = activityIcon(item); IconButton(onClick = { onSelect(item) }, modifier = Modifier.padding(vertical = 2.dp)) { Icon(icon, item.label, tint = if (selected == item) ReversoColors.Blue else ReversoColors.Muted) } }
    }
}

@Composable private fun MobileActivityBar(selected: IdeActivity, onSelect: (IdeActivity) -> Unit) {
    Row(Modifier.fillMaxWidth().height(58.dp).background(ReversoColors.Surface), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        listOf(IdeActivity.EXPLORER, IdeActivity.SEARCH, IdeActivity.SOURCE_CONTROL, IdeActivity.RUN_DEBUG, IdeActivity.EXTENSIONS, IdeActivity.SETTINGS).forEach { item ->
            IconButton(onClick = { onSelect(item) }, modifier = Modifier.size(46.dp)) { Icon(activityIcon(item), item.label, tint = if (selected == item) ReversoColors.Blue else ReversoColors.Muted) }
        }
    }
}

private fun activityIcon(item: IdeActivity) = when (item) {
    IdeActivity.EXPLORER -> Icons.Filled.AccountTree
    IdeActivity.SEARCH -> Icons.Filled.Search
    IdeActivity.SOURCE_CONTROL -> Icons.Filled.Source
    IdeActivity.RUN_DEBUG -> Icons.Filled.BugReport
    IdeActivity.EXTENSIONS -> Icons.Filled.Extension
    IdeActivity.SETTINGS -> Icons.Filled.Settings
}

@Composable private fun SidePanel(activity: IdeActivity, workspace: String?, items: List<WorkspaceItem<DocumentFile>>, tabs: List<EditorTab>, activeId: String?, query: String, onQuery: (String) -> Unit, onOpenFolder: () -> Unit, onOpenFile: () -> Unit, onCreateFile: () -> Unit, onCreateFolder: () -> Unit, onFile: (WorkspaceItem<DocumentFile>) -> Unit, onTab: (EditorTab) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxHeight().background(ReversoColors.Surface).padding(horizontal = 10.dp)) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) { Text(if (activity == IdeActivity.SEARCH) "SEARCH" else "EXPLORER", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = ReversoColors.Muted, letterSpacing = 1.sp); if (activity == IdeActivity.EXPLORER) { IconButton(onClick = onCreateFile) { Icon(Icons.Filled.NoteAdd, "New file") }; IconButton(onClick = onCreateFolder) { Icon(Icons.Filled.CreateNewFolder, "New folder") } } }
        if (activity == IdeActivity.SEARCH) {
            OutlinedTextField(query, onQuery, Modifier.fillMaxWidth(), placeholder = { Text("Search open files") }, singleLine = true)
            LazyColumn { if (query.isNotBlank()) tabs.forEach { tab ->
                tab.value.text.lines().forEachIndexed { index, line ->
                    if (line.contains(query, ignoreCase = true)) item(key = "${tab.id}:$index") {
                        TextButton(onClick = { onTab(tab) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth()) { Text("${tab.title}:${index + 1}", color = ReversoColors.Blue); Text(line.trim(), color = ReversoColors.Ink, maxLines = 1) }
                        }
                    }
                }
            } }
        }
        else {
            TextButton(onClick = onOpenFolder, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text(workspace ?: "Open Workspace", maxLines = 1) }
            Text("OPEN EDITORS", color = ReversoColors.Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp))
            tabs.forEach { tab -> TextButton(onClick = { onTab(tab) }, modifier = Modifier.fillMaxWidth()) { Text((if (tab.dirty) "● " else "") + tab.title, color = if (tab.id == activeId) ReversoColors.Ink else ReversoColors.Muted, maxLines = 1) } }
            Text("WORKSPACE", color = ReversoColors.Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp))
            if (items.isEmpty()) Text("Choose a folder to see its files", color = ReversoColors.Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
            LazyColumn { items(items, key = { it.id }) { item -> TextButton(onClick = { onFile(item) }, modifier = Modifier.fillMaxWidth()) { Icon(if (item.isDirectory) Icons.Filled.Folder else Icons.Filled.Code, null, tint = if (item.isDirectory) ReversoColors.Blue else ReversoColors.Muted, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(item.name, Modifier.padding(start = (item.depth * 12).dp), maxLines = 1, color = ReversoColors.Ink) } } }
            Spacer(Modifier.weight(1f)); TextButton(onClick = onOpenFile, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.FileOpen, null); Text("Open File") }
        }
    }
}

@Composable private fun EditorArea(active: EditorTab?, tabs: List<EditorTab>, activeId: String?, split: Boolean, workspace: String?, recents: List<RecentProject>, onSelect: (EditorTab) -> Unit, onClose: (EditorTab) -> Unit, onValue: (EditorTab) -> Unit, onWorkspace: () -> Unit, onInsert: (String) -> Unit, onWelcome: () -> Unit, onNew: () -> Unit, onOpen: () -> Unit, onFolder: () -> Unit, onRecent: (RecentProject) -> Unit, onDocs: () -> Unit) {
    Column(Modifier.fillMaxSize().background(ReversoColors.Background)) {
        if (tabs.isNotEmpty()) EditorTabs(tabs, activeId, onSelect, onClose)
        if (active == null) WelcomeScreen(recents, onNew, onOpen, onFolder, onRecent, onDocs)
        else {
            Breadcrumbs(workspace, listOf(active.title), onWorkspace)
            if (split && tabs.size > 1) Row(Modifier.weight(1f)) {
                EditorSurface(active, onValue, 14, Modifier.weight(1f), showMinimap = true)
                VerticalDivider(color = ReversoColors.Border)
                val second = tabs.firstOrNull { it.id != active.id }
                if (second != null) EditorSurface(second, onValue, 14, Modifier.weight(1f), showMinimap = true)
            } else EditorSurface(active, onValue, 14, Modifier.weight(1f), showMinimap = false)
            KeyboardToolbar(onInsert)
        }
    }
}

@Composable private fun WelcomeScreen(recents: List<RecentProject>, onNew: () -> Unit, onOpen: () -> Unit, onFolder: () -> Unit, onRecent: (RecentProject) -> Unit, onDocs: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(ReversoColors.Background).padding(horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally, contentPadding = PaddingValues(top = 28.dp, bottom = 28.dp)) {
        item { ReversoMark(76.dp); Spacer(Modifier.height(14.dp)); Text("REVERSO", style = MaterialTheme.typography.headlineSmall, letterSpacing = 3.sp); Text("Code anywhere. Build everything.", color = ReversoColors.Muted); Spacer(Modifier.height(26.dp)) }
        item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WelcomeAction("Create New Project", "Start with a new editable file", Icons.Filled.Add, onNew)
            WelcomeAction("Open Folder", "Work with files in a workspace", Icons.Filled.FolderOpen, onFolder)
            WelcomeAction("Open File", "Edit an existing document", Icons.Filled.FileOpen, onOpen)
            WelcomeAction("Clone Git Repository", "Git clone integration is not implemented yet", Icons.Filled.CloudDownload, null)
        }; Spacer(Modifier.height(24.dp)); Text("RECENT PROJECTS", Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelLarge, color = ReversoColors.Muted); Spacer(Modifier.height(8.dp)) }
        if (recents.isEmpty()) item { Text("Folders you open will appear here.", Modifier.fillMaxWidth().padding(vertical = 16.dp), color = ReversoColors.Muted) }
        items(recents) { project -> TextButton(onClick = { onRecent(project) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Folder, null, tint = ReversoColors.Blue); Column(Modifier.padding(start = 10.dp)) { Text(project.name, color = ReversoColors.Ink); Text(project.uri, color = ReversoColors.Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1) } } }
        item { Spacer(Modifier.height(20.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedButton(onClick = onDocs, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.MenuBook, null); Text(" Documentation") }; OutlinedButton(onClick = onDocs, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.RocketLaunch, null); Text(" Getting Started") } } }
    }
}

@Composable private fun WelcomeAction(title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: (() -> Unit)?) {
    Surface(onClick = { onClick?.invoke() }, enabled = onClick != null, color = ReversoColors.Panel, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = ReversoColors.Blue); Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(title, color = ReversoColors.Ink); Text(detail, color = ReversoColors.Muted, style = MaterialTheme.typography.bodySmall) }; Icon(Icons.Filled.ChevronRight, null, tint = ReversoColors.Muted) }
    }
}

@Composable private fun SearchPage(query: String, onQuery: (String) -> Unit, tabs: List<EditorTab>, onSelect: (EditorTab) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) { Text("Search in open editors", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(12.dp)); OutlinedTextField(query, onQuery, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Search text") }); LazyColumn { items(tabs) { tab -> if (query.isNotBlank()) { val lines = tab.value.text.lines().mapIndexedNotNull { i, line -> if (line.contains(query, ignoreCase = true)) (i + 1) to line.trim() else null }; lines.forEach { (lineNo, line) -> TextButton(onClick = { onSelect(tab) }, modifier = Modifier.fillMaxWidth()) { Column(Modifier.fillMaxWidth()) { Text("${tab.title}:$lineNo", color = ReversoColors.Blue); Text(line, color = ReversoColors.Ink, maxLines = 1) } } } } } } }
}

@Composable private fun ActivityPage(activity: IdeActivity, workspace: String?, recents: List<RecentProject>, onOpenFolder: () -> Unit, onOpenDocument: () -> Unit, onRecent: (RecentProject) -> Unit) {
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(activity.label, style = MaterialTheme.typography.headlineSmall)
        when (activity) {
            IdeActivity.SOURCE_CONTROL -> UnavailableCard("Source control", "Git operations are not connected yet. No repository changes are being simulated.")
            IdeActivity.RUN_DEBUG -> UnavailableCard("Run and Debug", "No run configuration or debugger is connected yet.")
            IdeActivity.EXTENSIONS -> { Text("Reverso Hub", style = MaterialTheme.typography.titleMedium); OutlinedTextField("", {}, Modifier.fillMaxWidth(), enabled = false, placeholder = { Text("Extension search · Hub not connected") }); UnavailableCard("Marketplace unavailable", "Extension discovery and installation will be enabled when Reverso Hub is implemented. The app does not install placeholder extensions.") }
            IdeActivity.SETTINGS -> { Text("Editor settings", style = MaterialTheme.typography.titleMedium); Text("Theme: Reverso Dark", color = ReversoColors.Muted); Text("Editor engine: Android text input (Rust engine integration pending)", color = ReversoColors.Muted); Text("Workspace: ${workspace ?: "None open"}", color = ReversoColors.Muted) }
            else -> { Text("Workspace: ${workspace ?: "No folder open"}", color = ReversoColors.Muted); OutlinedButton(onClick = onOpenFolder) { Text("Open Folder") }; OutlinedButton(onClick = onOpenDocument) { Text("Open File") }; recents.forEach { TextButton(onClick = { onRecent(it) }) { Text(it.name) } }
            }
        }
    }
}

@Composable private fun UnavailableCard(title: String, detail: String) { Surface(color = ReversoColors.Panel, shape = RoundedCornerShape(12.dp)) { Column(Modifier.padding(16.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(6.dp)); Text(detail, color = ReversoColors.Muted) } } }

@Composable private fun BottomPanel(tool: BottomTool, output: List<String>, modifier: Modifier, onTool: (BottomTool) -> Unit, onClose: () -> Unit, onDrag: (Float) -> Unit) {
    Column(modifier.fillMaxWidth().background(ReversoColors.Surface)) {
        Box(Modifier.fillMaxWidth().height(6.dp).pointerInput(Unit) { detectVerticalDragGestures { _, drag -> onDrag(drag) } }.background(ReversoColors.Border))
        Row(Modifier.fillMaxWidth().height(44.dp).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) { BottomTool.entries.forEach { TextButton(onClick = { onTool(it) }) { Text(it.label, color = if (tool == it) ReversoColors.Blue else ReversoColors.Muted) } }; Spacer(Modifier.weight(1f)); IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Close panel") } }
        when (tool) {
            BottomTool.OUTPUT -> LazyColumn(Modifier.weight(1f, fill = false).padding(horizontal = 12.dp)) { items(output) { Text(it, color = ReversoColors.Muted, style = MaterialTheme.typography.bodySmall) } }
            BottomTool.PROBLEMS -> Text("No language server is connected. Diagnostics are unavailable.", Modifier.padding(12.dp), color = ReversoColors.Muted)
            BottomTool.TERMINAL -> UnavailableCard("Termux terminal bridge unavailable", "REVERSO does not execute commands itself. Install/configure a supported authenticated Termux companion bridge when available. No terminal session is being faked.")
            BottomTool.DEBUG_CONSOLE -> Text("No debugger is connected.", Modifier.padding(12.dp), color = ReversoColors.Muted)
            BottomTool.GIT -> Text("Git integration is not implemented yet.", Modifier.padding(12.dp), color = ReversoColors.Muted)
        }
    }
}

@Composable private fun StatusBar(tab: EditorTab, message: String, onPanel: (BottomTool) -> Unit) {
    val cursor = tab.value.selection.end.coerceIn(0, tab.value.text.length)
    val before = tab.value.text.take(cursor)
    val line = before.count { it == '\n' } + 1
    val column = before.length - (before.lastIndexOf('\n') + 1) + 1
    Row(Modifier.fillMaxWidth().height(28.dp).background(ReversoColors.Blue).padding(horizontal = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("⎇ No repository", Modifier.clickable { onPanel(BottomTool.GIT) }, color = ReversoColors.Background, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.weight(1f)); Text("${languageForFile(tab.title)} · UTF-8 · Spaces: 4 · Ln $line, Col $column", color = ReversoColors.Background, style = MaterialTheme.typography.labelSmall, maxLines = 1); Spacer(Modifier.width(8.dp)); Text(message, color = ReversoColors.Background, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}
