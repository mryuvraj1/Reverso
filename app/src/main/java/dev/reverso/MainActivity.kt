package dev.reverso

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.DocumentsContract
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import dev.reverso.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    var sidebarOverride by remember { mutableStateOf<Boolean?>(null) }
    var workspace by remember { mutableStateOf<ProjectWorkspace?>(null) }
    var selectedDirectory by remember { mutableStateOf<DocumentFile?>(null) }
    var workspaceRevision by remember { mutableIntStateOf(0) }
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
    var pendingSaveTabId by remember { mutableStateOf<String?>(null) }
    var operationFile by remember { mutableStateOf<DocumentFile?>(null) }
    var operationName by remember { mutableStateOf("") }
    var operationKind by remember { mutableStateOf<WorkspaceFileAction?>(null) }
    var operationDestination by remember { mutableStateOf<DocumentFile?>(null) }
    var pendingRelease by remember { mutableStateOf<AvailableRelease?>(null) }
    var updateResult by remember { mutableStateOf(ReleaseCheckResult(ReleaseChecker.history(context), ReleaseChecker.lastChecked(context))) }
    var checkingUpdates by remember { mutableStateOf(false) }
    var automaticUpdates by remember { mutableStateOf(UpdatePreferences.automatic(context)) }
    var betaUpdates by remember { mutableStateOf(UpdatePreferences.beta(context)) }
    var updateNotifications by remember { mutableStateOf(UpdatePreferences.notifications(context)) }
    var downloadingTag by remember { mutableStateOf<String?>(null) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var downloadedUpdate by remember { mutableStateOf<DownloadedUpdate?>(null) }
    var installError by remember { mutableStateOf<String?>(null) }
    var pendingInstallerPermission by remember { mutableStateOf<DownloadedUpdate?>(null) }
    var splitMode by remember { mutableStateOf(false) }
    var aiConfig by remember { mutableStateOf(AiConfigRepository.load(context)) }
    var credentialSaved by remember { mutableStateOf(aiConfig?.let { SecureCredentialStore.has(context, it.provider) } ?: false) }
    val aiMessages = remember { mutableStateListOf<AiMessage>() }
    var aiLoading by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }
    var showAiSettings by remember { mutableStateOf(false) }
    var showApplyConfirmation by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val active = tabs.firstOrNull { it.id == activeId }
    val currentAppVersion = ReleaseChecker.currentVersion(context)
    val latestAvailableRelease = ReleaseChecker.latestPublished(updateResult.releases, betaUpdates)
    val notificationsPermissionGranted = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val treeItems = remember(workspace, workspace?.root, expandedFolders.toList(), workspaceRevision) {
        workspace?.let { listWorkspaceItems(it.root, expandedFolders.toSet()) }.orEmpty()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        updateNotifications = granted
        UpdatePreferences.setNotifications(context, granted)
    }
    val packageInstallPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val queued = pendingInstallerPermission
        if (queued != null && UpdateInstaller.installPermissionIntent(context) == null) {
            runCatching { context.startActivity(UpdateInstaller.installIntent(context, queued.file)) }
                .onFailure { installError = it.message ?: "Android could not open the package installer" }
        }
        pendingInstallerPermission = null
    }

    LaunchedEffect(Unit) {
        RecentProjects.load(context).firstOrNull()?.let { recent ->
            RecentProjects.open(context, recent)?.let { restored ->
                workspace = restored
                selectedDirectory = restored.root
                activity = IdeActivity.EXPLORER
                status = "Workspace restored: ${restored.name}"
            }
        }
        UpdateNotification.ensureChannel(context)
        UpdateScheduler.configure(context)
        if (automaticUpdates) {
            checkingUpdates = true
            val result = withContext(Dispatchers.IO) { ReleaseChecker.check(context, betaUpdates) }
            updateResult = result
            pendingRelease = ReleaseChecker.latestNewer(context, result.releases, betaUpdates)?.takeUnless { UpdatePreferences.isRemindedLater(context, it.tag) }
            checkingUpdates = false
        }
    }

    fun log(message: String) { output = (output + message).takeLast(100) }
    fun checkUpdates(force: Boolean = true) {
        if (checkingUpdates) return
        checkingUpdates = true
        coroutineScope.launch {
            val result = withContext(Dispatchers.IO) { ReleaseChecker.check(context, betaUpdates, force) }
            updateResult = result
            pendingRelease = ReleaseChecker.latestNewer(context, result.releases, betaUpdates)?.takeUnless { UpdatePreferences.isRemindedLater(context, it.tag) }
            checkingUpdates = false
            if (result.error != null) status = result.error else status = "Release information updated"
        }
    }
    fun downloadUpdate(release: AvailableRelease) {
        if (release.apk == null) { downloadError = "This release has no compatible Android APK; it is available as source code only."; return }
        downloadedUpdate = null
        downloadingTag = release.tag
        downloadError = null
        coroutineScope.launch {
            runCatching { withContext(Dispatchers.IO) { UpdateInstaller.download(context, release) } }
                .onSuccess { downloadedUpdate = it; status = "Downloaded Reverso ${release.tag}" }
                .onFailure { downloadError = it.message ?: "APK download failed" }
            downloadingTag = null
        }
    }
    fun openAndroidInstaller(update: DownloadedUpdate) {
        val settingsIntent = UpdateInstaller.installPermissionIntent(context)
        if (settingsIntent != null) {
            pendingInstallerPermission = update
            packageInstallPermissionLauncher.launch(settingsIntent)
        } else runCatching { context.startActivity(UpdateInstaller.installIntent(context, update.file)) }
            .onFailure { installError = it.message ?: "Android could not open the package installer" }
    }
    fun updateTab(tab: EditorTab) {
        val index = tabs.indexOfFirst { it.id == tab.id }
        if (index >= 0) tabs[index] = tab
    }
    fun addTab(tab: EditorTab) {
        val existing = tabs.indexOfFirst { it.uri != null && it.uri == tab.uri }
        if (existing >= 0) activeId = tabs[existing].id else { tabs.add(tab); activeId = tab.id }
        activity = IdeActivity.EXPLORER
        if (sidebarOverride == true) sidebarOverride = false
    }
    fun openUri(uri: Uri, title: String) {
        runCatching {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: throw IOException("The selected document could not be read")
            val actualName = runCatching { DocumentFile.fromSingleUri(context, uri)?.name }.getOrNull() ?: title
            addTab(EditorTab(uri.toString(), actualName, uri, TextFieldValue(text)))
            status = "Opened $actualName"; log("Opened $actualName")
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
    fun sendAi(prompt: String, includeActiveFile: Boolean) {
        val config = aiConfig
        if (config == null || !credentialSaved) {
            aiError = "Configure an AI provider and save its API key first."
            activity = IdeActivity.AI_CHAT
            sidebarOverride = true
            return
        }
        val apiKey = try { SecureCredentialStore.get(context, config.provider) } catch (_: Exception) {
            aiError = "The saved key could not be decrypted. Save the provider key again in AI settings."
            return
        }
        if (apiKey.isNullOrBlank()) {
            aiError = "No API key is saved. Configure an AI provider first."
            return
        }
        val fileContext = if (includeActiveFile) active?.let { tab ->
            val clipped = tab.value.text.take(12_000)
            "Active file: ${tab.title}\n```\n$clipped${if (tab.value.text.length > clipped.length) "\n[File context truncated at 12,000 characters]" else ""}\n```"
        } else null
        aiMessages.add(AiMessage("user", prompt, fileContext))
        val requestHistory = aiMessages.toList()
        aiError = null
        aiLoading = true
        coroutineScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { AiChatClient.complete(config, apiKey, requestHistory) }
                aiMessages.add(AiMessage("assistant", response))
                log("AI response received from ${config.provider.label}")
            } catch (error: Exception) {
                aiError = error.message?.take(240) ?: "AI request failed. Check your connection and provider configuration."
                log("AI request failed (${config.provider.label})")
            } finally {
                aiLoading = false
            }
        }
    }

    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) openUri(uri, uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')?.takeIf(String::isNotBlank) ?: "untitled.txt")
    }
    val openFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            val selectedRoot = DocumentFile.fromTreeUri(context, uri) ?: error("Folder access was not granted")
            val projectName = pendingProjectName
            val root = if (projectName != null) {
                if (validateDocumentName(projectName) != null) error("Invalid project name")
                if (selectedRoot.findFile(projectName) != null) error("A file or folder named $projectName already exists")
                selectedRoot.createDirectory(projectName) ?: error("Could not create project folder")
            } else selectedRoot
            workspace = ProjectWorkspace(root.uri, root, root.name ?: "Workspace", uri)
            selectedDirectory = root
            expandedFolders.clear()
            workspaceRevision++
            val recent = RecentProject(root.name ?: "Workspace", uri.toString(), projectName)
            RecentProjects.remember(context, recent); recents = RecentProjects.load(context)
            activity = IdeActivity.EXPLORER; status = if (projectName == null) "Workspace opened: ${recent.name}" else "Project created: ${recent.name}"; log(status)
            sidebarOverride = true
            pendingProjectName = null; newProjectName = ""
        }.onFailure { status = "Folder open failed: ${it.message ?: "unknown error"}"; pendingProjectName = null }
        else pendingProjectName = null
    }
    val saveDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val tab = tabs.firstOrNull { it.id == pendingSaveTabId }
        if (uri != null && tab != null) {
            val actualName = runCatching { DocumentFile.fromSingleUri(context, uri)?.name }.getOrNull() ?: tab.title
            saveTab(tab.copy(title = actualName), uri)
        }
        pendingSaveTabId = null
    }

    if (newProjectDialog) AlertDialog(
        onDismissRequest = { newProjectDialog = false }, title = { Text("Create project") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(newProjectName, { newProjectName = it }, label = { Text("Project folder name") }, singleLine = true)
            Text("Choose a parent folder next. Reverso will create the project folder there.", color = ReversoColors.Muted, style = MaterialTheme.typography.bodySmall)
        } },
        confirmButton = { TextButton(onClick = {
            val name = newProjectName
            val error = validateDocumentName(name)
            if (error != null) status = error else { pendingProjectName = name; newProjectDialog = false; openFolder.launch(null) }
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
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(createName, { createName = it }, label = { Text("Exact file or folder name") }, singleLine = true)
            if (workspace != null) Text("Creates inside: ${selectedDirectory?.name ?: workspace?.name}", color = ReversoColors.Muted, style = MaterialTheme.typography.bodySmall)
        } },
        confirmButton = { TextButton(onClick = {
            val name = createName
            val validationError = validateDocumentName(name)
            val parent = selectedDirectory?.takeIf { it.exists() && it.isDirectory } ?: workspace?.root
            if (validationError != null) status = validationError
            else if (workspace == null) {
                if (!createFolder) { val tab = EditorTab("new-${System.nanoTime()}", name); tabs.add(tab); activeId = tab.id; status = "Created $name" }
                else status = "Open a workspace before creating a folder"
            } else if (parent == null) status = "Open a workspace before creating files"
            else if (parent.findFile(name) != null) status = "A file or folder named $name already exists"
            else runCatching {
                    val file = if (createFolder) parent.createDirectory(name) else parent.createFile(mimeForDocumentName(name), name)
                    if (file == null) error("Could not create $name")
                    if (!createFolder) addTab(EditorTab(file.uri.toString(), name, file.uri, TextFieldValue("")))
                    status = "Created $name"; log("Created $name")
                    workspaceRevision++
                }.onFailure { status = "Create failed: ${it.message ?: "unknown error"}" }
            createDialog = false; createName = ""
        }) { Text("Create") } },
        dismissButton = { TextButton(onClick = { createDialog = false }) { Text("Cancel") } },
    )

    operationKind?.let { action ->
        val source = operationFile
        val destinationChoices = remember(workspace, treeItems) { workspace?.let { listWorkspaceDirectories(it.root) }.orEmpty() }
        var destinationMenuOpen by remember(action, source?.uri) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { operationKind = null },
            title = { Text("${action.name.lowercase().replaceFirstChar(Char::uppercase)} ${source?.name.orEmpty()}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (action) {
                        WorkspaceFileAction.RENAME -> OutlinedTextField(operationName, { operationName = it }, label = { Text("Exact new name") }, singleLine = true)
                        WorkspaceFileAction.DELETE -> Text("Delete this item from the selected storage provider? Open tabs for this file will close.")
                        WorkspaceFileAction.COPY, WorkspaceFileAction.MOVE -> Box {
                            OutlinedButton(onClick = { destinationMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Destination: ${operationDestination?.name ?: "Choose folder"}", maxLines = 1)
                            }
                            DropdownMenu(expanded = destinationMenuOpen, onDismissRequest = { destinationMenuOpen = false }) {
                                destinationChoices.forEach { directory -> DropdownMenuItem(
                                    text = { Text(directory.name ?: "Workspace") },
                                    onClick = { operationDestination = directory; destinationMenuOpen = false },
                                ) }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = {
                val file = source
                if (file == null) { operationKind = null; return@TextButton }
                coroutineScope.launch {
                    runCatching { withContext(Dispatchers.IO) { performWorkspaceFileAction(context, file, action, operationName, operationDestination) } }
                        .onSuccess { result ->
                            val openTab = tabs.firstOrNull { it.uri == file.uri }
                            when (action) {
                                WorkspaceFileAction.DELETE -> {
                                    tabs.removeAll { it.uri == file.uri }
                                    if (openTab?.id == activeId) activeId = tabs.lastOrNull()?.id
                                }
                                WorkspaceFileAction.RENAME, WorkspaceFileAction.MOVE -> if (openTab != null && result != null) {
                                    val index = tabs.indexOfFirst { it.id == openTab.id }
                                    val updated = openTab.copy(id = result.uri.toString(), uri = result.uri, title = result.name ?: openTab.title)
                                    if (index >= 0) tabs[index] = updated
                                    if (activeId == openTab.id) activeId = updated.id
                                }
                                WorkspaceFileAction.COPY -> Unit
                            }
                            status = "${action.name.lowercase().replaceFirstChar(Char::uppercase)} complete: ${file.name}"
                            log(status)
                            workspaceRevision++
                        }
                        .onFailure { status = "${action.name.lowercase().replaceFirstChar(Char::uppercase)} failed: ${it.message ?: "storage provider error"}" }
                    operationKind = null; operationFile = null
                }
            }) { Text(if (action == WorkspaceFileAction.DELETE) "Delete" else action.name.lowercase().replaceFirstChar(Char::uppercase)) } },
            dismissButton = { TextButton(onClick = { operationKind = null }) { Text("Cancel") } },
        )
    }

    if (showAiSettings) Dialog(
        onDismissRequest = { showAiSettings = false },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            shape = RoundedCornerShape(20.dp), color = ReversoColors.Background,
        ) {
            AiProviderSettingsScreen(
                initialConfig = aiConfig,
                configFor = { provider -> AiConfigRepository.loadFor(context, provider) },
                credentialSavedFor = { provider -> SecureCredentialStore.has(context, provider) },
                onSave = { config, key -> runCatching {
                    AiConfigRepository.save(context, config, key)
                    aiConfig = config
                    credentialSaved = SecureCredentialStore.has(context, config.provider)
                    null
                }.getOrElse { "Could not save provider: ${it.message ?: "secure storage failed"}" } },
                onClear = { provider -> runCatching {
                    AiConfigRepository.clear(context, provider)
                    if (aiConfig?.provider == provider) aiConfig = null
                    credentialSaved = aiConfig?.let { SecureCredentialStore.has(context, it.provider) } ?: false
                    aiMessages.clear()
                    null
                }.getOrElse { "Could not clear provider credentials: ${it.message ?: "secure storage failed"}" } },
                onDone = { showAiSettings = false },
            )
        }
    }

    val latestAiResponse = aiMessages.lastOrNull { it.role == "assistant" }?.content
    if (showApplyConfirmation) AlertDialog(
        onDismissRequest = { showApplyConfirmation = false },
        title = { Text("Replace ${active?.title ?: "editor"}?") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("This replaces the complete in-memory file with the latest AI response. Review the response before applying.")
            Text(latestAiResponse.orEmpty().take(800), color = ReversoColors.Muted, maxLines = 8)
        } },
        confirmButton = { TextButton(onClick = {
            val tab = active
            val response = latestAiResponse
            if (tab != null && response != null) updateTab(tab.copy(value = TextFieldValue(response), dirty = true))
            showApplyConfirmation = false
        }) { Text("Replace file") } },
        dismissButton = { TextButton(onClick = { showApplyConfirmation = false }) { Text("Cancel") } },
    )

    BoxWithConstraints(Modifier.fillMaxSize().background(ReversoColors.Background).onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown && event.isCtrlPressed && event.isShiftPressed && event.key == Key.S) { active?.let { pendingSaveTabId = it.id; saveDocument.launch(it.title) }; true }
        else if (event.type == KeyEventType.KeyDown && event.isCtrlPressed && event.key == Key.S) { active?.let { if (it.uri == null) { pendingSaveTabId = it.id; saveDocument.launch(it.title) } else saveTab(it) }; true }
        else if (event.type == KeyEventType.KeyDown && event.isCtrlPressed && event.key == Key.F) { findDialog = true; true }
        else false
    }) {
        val wide = maxWidth >= 720.dp
        val sidebarVisible = sidebarOverride ?: wide
        val availableScreenMaxHeight = maxHeight
        val availableScreenHeight = availableScreenMaxHeight.value
        Column(Modifier.fillMaxSize()) {
            Header(active?.title, sidebarVisible, wide, onToggleSidebar = { sidebarOverride = !sidebarVisible }, onHome = { activity = IdeActivity.EXPLORER; activeId = null; sidebarOverride = false }, onOpen = { openDocument.launch(arrayOf("text/*", "application/json", "application/xml", "application/octet-stream")) }, onSave = { active?.let { if (it.uri == null) { pendingSaveTabId = it.id; saveDocument.launch(it.title) } else saveTab(it) } }, onSaveAs = { active?.let { pendingSaveTabId = it.id; saveDocument.launch(it.title) } }, onFind = { findDialog = true }, onSplit = { if (tabs.size > 1) splitMode = !splitMode else status = "Open a second file before splitting the editor" }, onToggleBottom = { if (!bottomOpen) bottomTool = BottomTool.TERMINAL; bottomOpen = !bottomOpen })
            if (wide) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    AnimatedVisibility(
                        visible = sidebarVisible,
                        enter = expandHorizontally(expandFrom = Alignment.Start) + slideInHorizontally { -it / 3 } + fadeIn(),
                        exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + slideOutHorizontally { -it / 3 } + fadeOut(),
                    ) {
                        IdeSidebar(activity, true, workspace?.name, treeItems, tabs, activeId, query, aiMessages, aiConfig != null && credentialSaved, aiLoading, aiError,
                            updateHistory = updateResult.releases, currentVersion = currentAppVersion, latestRelease = latestAvailableRelease, checkedAt = updateResult.checkedAt, updateError = updateResult.error, checkingUpdates = checkingUpdates, automaticUpdates = automaticUpdates, betaUpdates = betaUpdates, updateNotifications = updateNotifications, notificationsPermissionGranted = notificationsPermissionGranted,
                            onAutomaticUpdates = { automaticUpdates = it; UpdatePreferences.setAutomatic(context, it); if (it) checkUpdates() }, onBetaUpdates = { betaUpdates = it; UpdatePreferences.setBeta(context, it); checkUpdates() }, onUpdateNotifications = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= 33 && !notificationsPermissionGranted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                else { updateNotifications = enabled; UpdatePreferences.setNotifications(context, enabled) }
                            }, onRequestNotificationPermission = { if (Build.VERSION.SDK_INT >= 33) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            onCheckUpdates = { checkUpdates() }, onDownloadUpdate = ::downloadUpdate, onViewRelease = { runCatching { uriHandler.openUri(it.url) } },
                            onActivity = { activity = it }, onQuery = { query = it }, onOpenFolder = { openFolder.launch(null) },
                            onOpenFile = { openDocument.launch(arrayOf("text/*", "application/json", "application/xml", "application/octet-stream")) },
                            onCreateFile = { createFolder = false; createDialog = true }, onCreateFolder = { createFolder = true; createDialog = true },
                            onRefresh = { workspaceRevision++ },
                            onFile = { if (it.isDirectory) { selectedDirectory = it.payload; if (it.id in expandedFolders) expandedFolders.remove(it.id) else expandedFolders.add(it.id) } else { selectedDirectory = it.payload.parentFile; openUri(it.uri, it.name) } },
                            onOperation = { file, action -> operationFile = file; operationKind = action; operationName = file.name.orEmpty(); operationDestination = workspace?.root },
                            onTab = { activeId = it.id; activity = IdeActivity.EXPLORER }, onAiConfigure = { showAiSettings = true }, onAiNewChat = { aiMessages.clear() },
                            onAiSend = ::sendAi, onAiApply = { if (active != null && latestAiResponse != null) showApplyConfirmation = true else aiError = "Open a file and request an AI response before applying." },
                            onRecent = { RecentProjects.open(context, it)?.let { ws -> workspace = ws; selectedDirectory = ws.root; expandedFolders.clear(); activity = IdeActivity.EXPLORER } },
                            modifier = Modifier.fillMaxHeight())
                    }
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        EditorArea(active, tabs, activeId, splitMode, workspace?.name, recents,
                            onSelect = { activeId = it.id }, onClose = { tabs.remove(it); if (activeId == it.id) activeId = tabs.lastOrNull()?.id }, onValue = ::updateTab,
                            onWorkspace = { activity = IdeActivity.EXPLORER }, onInsert = { active?.let { tab -> updateTab(insertAtSelection(tab, it)) } }, onWelcome = { activeId = null },
                            onNew = { newProjectDialog = true },
                            onOpen = { openDocument.launch(arrayOf("text/*", "application/json", "application/xml", "application/octet-stream")) }, onFolder = { openFolder.launch(null) }, onRecent = { RecentProjects.open(context, it)?.let { ws -> workspace = ws; selectedDirectory = ws.root; expandedFolders.clear(); activity = IdeActivity.EXPLORER } }, onDocs = { runCatching { uriHandler.openUri("https://github.com/mryuvraj1/Reverso/tree/main/project-knowledge") } }, modifier = Modifier.weight(1f))
                        AnimatedVisibility(visible = bottomOpen, enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(), exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()) {
                            BottomPanel(bottomTool, output, Modifier.height(bottomHeight.dp), onTool = { bottomTool = it }, onClose = { bottomOpen = false }, onDrag = { delta -> bottomHeight = (bottomHeight - delta).coerceIn(110f, 420f) })
                        }
                    }
                }
            } else {
                Column(Modifier.weight(1f).fillMaxWidth()) {
                    AnimatedContent(targetState = sidebarVisible, modifier = Modifier.weight(1f).fillMaxWidth(), label = "phone_sidebar_content") { showSidebar ->
                        if (!showSidebar) {
                            EditorArea(active, tabs, activeId, false, workspace?.name, recents, onSelect = { activeId = it.id }, onClose = { tabs.remove(it); if (activeId == it.id) activeId = tabs.lastOrNull()?.id }, onValue = ::updateTab, onWorkspace = { activity = IdeActivity.EXPLORER; sidebarOverride = true }, onInsert = { active?.let { tab -> updateTab(insertAtSelection(tab, it)) } }, onWelcome = { activeId = null }, onNew = { newProjectDialog = true }, onOpen = { openDocument.launch(arrayOf("text/*", "application/json", "application/xml", "application/octet-stream")) }, onFolder = { openFolder.launch(null) }, onRecent = { RecentProjects.open(context, it)?.let { ws -> workspace = ws; sidebarOverride = true } }, onDocs = { runCatching { uriHandler.openUri("https://github.com/mryuvraj1/Reverso/tree/main/project-knowledge") } })
                        } else {
                            IdeSidebar(activity, false, workspace?.name, treeItems, tabs, activeId, query, aiMessages, aiConfig != null && credentialSaved, aiLoading, aiError,
                                updateHistory = updateResult.releases, currentVersion = currentAppVersion, latestRelease = latestAvailableRelease, checkedAt = updateResult.checkedAt, updateError = updateResult.error, checkingUpdates = checkingUpdates, automaticUpdates = automaticUpdates, betaUpdates = betaUpdates, updateNotifications = updateNotifications, notificationsPermissionGranted = notificationsPermissionGranted,
                                onAutomaticUpdates = { automaticUpdates = it; UpdatePreferences.setAutomatic(context, it); if (it) checkUpdates() }, onBetaUpdates = { betaUpdates = it; UpdatePreferences.setBeta(context, it); checkUpdates() }, onUpdateNotifications = { enabled ->
                                    if (enabled && Build.VERSION.SDK_INT >= 33 && !notificationsPermissionGranted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    else { updateNotifications = enabled; UpdatePreferences.setNotifications(context, enabled) }
                                }, onRequestNotificationPermission = { if (Build.VERSION.SDK_INT >= 33) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                onCheckUpdates = { checkUpdates() }, onDownloadUpdate = ::downloadUpdate, onViewRelease = { runCatching { uriHandler.openUri(it.url) } },
                                onActivity = { activity = it }, onQuery = { query = it }, onOpenFolder = { openFolder.launch(null) },
                                onOpenFile = { openDocument.launch(arrayOf("text/*", "application/json", "application/xml", "application/octet-stream")) },
                                onCreateFile = { createFolder = false; createDialog = true }, onCreateFolder = { createFolder = true; createDialog = true },
                                onRefresh = { workspaceRevision++ },
                                onFile = { if (it.isDirectory) { selectedDirectory = it.payload; if (it.id in expandedFolders) expandedFolders.remove(it.id) else expandedFolders.add(it.id) } else { selectedDirectory = it.payload.parentFile; openUri(it.uri, it.name) } },
                                onOperation = { file, action -> operationFile = file; operationKind = action; operationName = file.name.orEmpty(); operationDestination = workspace?.root },
                                onTab = { activeId = it.id; activity = IdeActivity.EXPLORER; sidebarOverride = false }, onAiConfigure = { showAiSettings = true }, onAiNewChat = { aiMessages.clear() },
                                onAiSend = ::sendAi, onAiApply = { if (active != null && latestAiResponse != null) showApplyConfirmation = true else aiError = "Open a file and request an AI response before applying." },
                            onRecent = { RecentProjects.open(context, it)?.let { ws -> workspace = ws; selectedDirectory = ws.root; expandedFolders.clear(); activity = IdeActivity.EXPLORER; sidebarOverride = true } }, modifier = Modifier.fillMaxSize())
                        }
                    }
                    AnimatedVisibility(visible = bottomOpen, enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(), exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()) {
                        BottomPanel(bottomTool, output, Modifier.height(bottomHeight.dp.coerceAtMost(availableScreenMaxHeight * 0.58f)), onTool = { bottomTool = it }, onClose = { bottomOpen = false }, onDrag = { delta -> bottomHeight = (bottomHeight - delta).coerceIn(110f, availableScreenHeight * .7f) })
                    }
                }
            }
            if (active != null) StatusBar(active, status)
        }
    }

    pendingRelease?.let { release -> AlertDialog(onDismissRequest = { pendingRelease = null }, title = { Text("New Reverso update available") },
        text = { Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Current version: $currentAppVersion")
            Text("Latest version: ${release.tag}")
            Text("Published: ${release.publishedAt.take(10)} · ${if (release.prerelease) "Beta" else "Stable"}", color = ReversoColors.Muted, style = MaterialTheme.typography.bodySmall)
            if (release.notes.isNotBlank()) Text(release.notes, color = ReversoColors.Muted)
            Text(if (release.apk == null) "No compatible Android APK is attached. This release is available as source code only." else "APK: ${release.apk.name}. Android will show its normal installation confirmation.", color = ReversoColors.Muted, style = MaterialTheme.typography.bodySmall)
        } },
        confirmButton = { Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (release.apk != null) TextButton(onClick = { pendingRelease = null; downloadUpdate(release) }) { Text("Download Update") }
            TextButton(onClick = { runCatching { uriHandler.openUri(release.url) }; pendingRelease = null }) { Text("View Release Notes") }
        } },
        dismissButton = { TextButton(onClick = { UpdatePreferences.remindLater(context, release.tag); pendingRelease = null }) { Text("Remind Me Later") } }) }

    downloadingTag?.let { tag -> AlertDialog(onDismissRequest = { }, title = { Text("Downloading Reverso $tag") }, text = { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(24.dp)); Spacer(Modifier.width(12.dp)); Text("Downloading and verifying the GitHub release APK…") } }, confirmButton = {}) }
    downloadError?.let { message -> AlertDialog(onDismissRequest = { downloadError = null }, title = { Text("Update download failed") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { downloadError = null }) { Text("OK") } }) }
    downloadedUpdate?.let { update -> AlertDialog(
        onDismissRequest = { downloadedUpdate = null },
        title = { Text("APK ready to install") },
        text = { Text(if (update.checksumVerified) "GitHub's SHA-256 digest matches the downloaded APK. Android will ask you to confirm installation." else "GitHub did not provide a SHA-256 digest for this asset. The download came from the official Reverso release, but its checksum could not be verified. Continue only if you accept that limitation.") },
        confirmButton = { TextButton(onClick = { downloadedUpdate = null; openAndroidInstaller(update) }) { Text("Open Android Installer") } },
        dismissButton = { TextButton(onClick = { downloadedUpdate = null }) { Text("Cancel") } },
    ) }
    installError?.let { message -> AlertDialog(onDismissRequest = { installError = null }, title = { Text("Could not open installer") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { installError = null }) { Text("OK") } }) }
}

private fun fileIconFor(name: String) = when (name.substringAfterLast('.', "").lowercase()) {
    "json", "xml", "yaml", "yml" -> Icons.Filled.DataObject
    "md", "txt", "log" -> Icons.Filled.Description
    "png", "jpg", "jpeg", "gif", "webp" -> Icons.Filled.Image
    else -> Icons.Filled.Code
}

@Composable private fun Header(title: String?, sidebarVisible: Boolean, showSplit: Boolean, onToggleSidebar: () -> Unit, onHome: () -> Unit, onOpen: () -> Unit, onSave: () -> Unit, onSaveAs: () -> Unit, onFind: () -> Unit, onSplit: () -> Unit, onToggleBottom: () -> Unit) {
    var saveMenuOpen by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(58.dp).background(ReversoColors.Surface).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onToggleSidebar) { Icon(if (sidebarVisible) Icons.Filled.MenuOpen else Icons.Filled.Menu, "Toggle sidebar", tint = if (sidebarVisible) ReversoColors.Blue else ReversoColors.Muted) }
        ReversoMark(36.dp, Modifier.clickable(onClick = onHome)); Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f).clickable(onClick = onHome)) { Text("REVERSO", style = MaterialTheme.typography.titleMedium, letterSpacing = 1.3.sp); Text(title ?: "Code anywhere · Build everything", color = ReversoColors.Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
        IconButton(onClick = onOpen) { Icon(Icons.Filled.FolderOpen, "Open file", tint = ReversoColors.Muted) }
        Box {
            IconButton(onClick = { saveMenuOpen = true }) { Icon(Icons.Filled.Save, "Save options", tint = ReversoColors.Muted) }
            DropdownMenu(expanded = saveMenuOpen, onDismissRequest = { saveMenuOpen = false }) {
                DropdownMenuItem(text = { Text("Save") }, onClick = { saveMenuOpen = false; onSave() })
                DropdownMenuItem(text = { Text("Save As…") }, onClick = { saveMenuOpen = false; onSaveAs() })
            }
        }
        IconButton(onClick = onFind) { Icon(Icons.Filled.Search, "Find", tint = ReversoColors.Muted) }
        if (showSplit) IconButton(onClick = onSplit) { Icon(Icons.Filled.CallSplit, "Split editor", tint = ReversoColors.Muted) }
        IconButton(onClick = onToggleBottom) { Icon(Icons.Filled.Terminal, "Toggle bottom panel", tint = ReversoColors.Muted) }
    }
}

@Composable private fun ActivityRail(selected: IdeActivity, onSelect: (IdeActivity) -> Unit) {
    Column(Modifier.width(54.dp).fillMaxHeight().background(ReversoColors.Surface), horizontalAlignment = Alignment.CenterHorizontally) {
        IdeActivity.entries.forEach { item -> val icon = activityIcon(item); IconButton(onClick = { onSelect(item) }, modifier = Modifier.padding(vertical = 2.dp)) { Icon(icon, item.label, tint = if (selected == item) ReversoColors.Blue else ReversoColors.Muted) } }
    }
}

private fun activityIcon(item: IdeActivity) = when (item) {
    IdeActivity.EXPLORER -> Icons.Filled.AccountTree
    IdeActivity.SEARCH -> Icons.Filled.Search
    IdeActivity.SOURCE_CONTROL -> Icons.Filled.Source
    IdeActivity.RUN_DEBUG -> Icons.Filled.BugReport
    IdeActivity.EXTENSIONS -> Icons.Filled.Extension
    IdeActivity.AI_CHAT -> Icons.Filled.AutoAwesome
    IdeActivity.SETTINGS -> Icons.Filled.Settings
}

@Composable private fun IdeSidebar(
    activity: IdeActivity,
    wide: Boolean,
    workspace: String?,
    treeItems: List<WorkspaceItem<DocumentFile>>,
    tabs: List<EditorTab>,
    activeId: String?,
    query: String,
    aiMessages: List<AiMessage>,
    aiConfigured: Boolean,
    aiLoading: Boolean,
    aiError: String?,
    updateHistory: List<AvailableRelease>,
    currentVersion: String,
    latestRelease: AvailableRelease?,
    checkedAt: Long,
    updateError: String?,
    checkingUpdates: Boolean,
    automaticUpdates: Boolean,
    betaUpdates: Boolean,
    updateNotifications: Boolean,
    notificationsPermissionGranted: Boolean,
    onAutomaticUpdates: (Boolean) -> Unit,
    onBetaUpdates: (Boolean) -> Unit,
    onUpdateNotifications: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: (AvailableRelease) -> Unit,
    onViewRelease: (AvailableRelease) -> Unit,
    onActivity: (IdeActivity) -> Unit,
    onQuery: (String) -> Unit,
    onOpenFolder: () -> Unit,
    onOpenFile: () -> Unit,
    onCreateFile: () -> Unit,
    onCreateFolder: () -> Unit,
    onFile: (WorkspaceItem<DocumentFile>) -> Unit,
    onOperation: (DocumentFile, WorkspaceFileAction) -> Unit,
    onRefresh: () -> Unit,
    onTab: (EditorTab) -> Unit,
    onAiConfigure: () -> Unit,
    onAiNewChat: () -> Unit,
    onAiSend: (String, Boolean) -> Unit,
    onAiApply: () -> Unit,
    onRecent: (RecentProject) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.background(ReversoColors.Surface).then(if (wide) Modifier.width(if (activity == IdeActivity.AI_CHAT) 380.dp else 330.dp) else Modifier.fillMaxWidth())) {
        ActivityRail(activity, onActivity)
        when (activity) {
            IdeActivity.EXPLORER, IdeActivity.SEARCH -> SidePanel(
                activity, workspace, treeItems, tabs, activeId, query, onQuery, onOpenFolder, onOpenFile,
                onCreateFile, onCreateFolder, onFile, onOperation, onRefresh, onTab, Modifier.weight(1f),
            )
            IdeActivity.AI_CHAT -> AiChatScreen(
                messages = aiMessages, configured = aiConfigured, activeFileName = tabs.firstOrNull { it.id == activeId }?.title,
                loading = aiLoading, error = aiError, onConfigure = onAiConfigure, onNewChat = onAiNewChat, onSend = onAiSend, onApplyLatest = onAiApply,
                modifier = Modifier.weight(1f),
            )
            else -> SidebarToolPage(
                activity, workspace, onOpenFolder, onOpenFile, onAiConfigure,
                updateHistory, currentVersion, latestRelease, checkedAt, updateError, checkingUpdates, automaticUpdates, betaUpdates, updateNotifications, notificationsPermissionGranted,
                onAutomaticUpdates, onBetaUpdates, onUpdateNotifications, onRequestNotificationPermission, onCheckUpdates, onDownloadUpdate, onViewRelease,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable private fun SidePanel(activity: IdeActivity, workspace: String?, items: List<WorkspaceItem<DocumentFile>>, tabs: List<EditorTab>, activeId: String?, query: String, onQuery: (String) -> Unit, onOpenFolder: () -> Unit, onOpenFile: () -> Unit, onCreateFile: () -> Unit, onCreateFolder: () -> Unit, onFile: (WorkspaceItem<DocumentFile>) -> Unit, onOperation: (DocumentFile, WorkspaceFileAction) -> Unit, onRefresh: () -> Unit, onTab: (EditorTab) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxHeight().background(ReversoColors.Surface).padding(horizontal = 10.dp)) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) { Text(if (activity == IdeActivity.SEARCH) "SEARCH" else "EXPLORER", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = ReversoColors.Muted, letterSpacing = 1.sp); if (activity == IdeActivity.EXPLORER) { IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, "Refresh files") }; IconButton(onClick = onCreateFile) { Icon(Icons.Filled.NoteAdd, "New file") }; IconButton(onClick = onCreateFolder) { Icon(Icons.Filled.CreateNewFolder, "New folder") } } }
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
            LazyColumn { items(items, key = { it.id }) { item ->
                var menuOpen by remember(item.id) { mutableStateOf(false) }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onFile(item) }, modifier = Modifier.weight(1f).padding(start = (item.depth * 12).dp)) {
                        if (item.isDirectory) Icon(Icons.Filled.FolderOpen, null, tint = ReversoColors.Blue, modifier = Modifier.size(18.dp))
                        else Icon(fileIconFor(item.name), null, tint = ReversoColors.Muted, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text(item.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, color = ReversoColors.Ink)
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, "Actions for ${item.name}", tint = ReversoColors.Muted) }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            listOf(WorkspaceFileAction.RENAME, WorkspaceFileAction.COPY, WorkspaceFileAction.MOVE, WorkspaceFileAction.DELETE).forEach { action ->
                                DropdownMenuItem(text = { Text(action.name.lowercase().replaceFirstChar(Char::uppercase)) }, onClick = { menuOpen = false; onOperation(item.payload, action) })
                            }
                        }
                    }
                }
            } }
            Spacer(Modifier.weight(1f)); TextButton(onClick = onOpenFile, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.FileOpen, null); Text("Open File") }
        }
    }
}

@Composable private fun EditorArea(active: EditorTab?, tabs: List<EditorTab>, activeId: String?, split: Boolean, workspace: String?, recents: List<RecentProject>, onSelect: (EditorTab) -> Unit, onClose: (EditorTab) -> Unit, onValue: (EditorTab) -> Unit, onWorkspace: () -> Unit, onInsert: (String) -> Unit, onWelcome: () -> Unit, onNew: () -> Unit, onOpen: () -> Unit, onFolder: () -> Unit, onRecent: (RecentProject) -> Unit, onDocs: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().background(ReversoColors.Background)) {
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

@Composable private fun SidebarToolPage(
    activity: IdeActivity,
    workspace: String?,
    onOpenFolder: () -> Unit,
    onOpenDocument: () -> Unit,
    onAiSettings: () -> Unit,
    updateHistory: List<AvailableRelease>,
    currentVersion: String,
    latestRelease: AvailableRelease?,
    checkedAt: Long,
    updateError: String?,
    checkingUpdates: Boolean,
    automaticUpdates: Boolean,
    betaUpdates: Boolean,
    updateNotifications: Boolean,
    notificationsPermissionGranted: Boolean,
    onAutomaticUpdates: (Boolean) -> Unit,
    onBetaUpdates: (Boolean) -> Unit,
    onUpdateNotifications: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: (AvailableRelease) -> Unit,
    onViewRelease: (AvailableRelease) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(activity.label, style = MaterialTheme.typography.titleLarge)
        when (activity) {
            IdeActivity.SOURCE_CONTROL -> UnavailableCard("Git not connected", "Repository detection and Git status, staging, commits, and remotes are not implemented yet.")
            IdeActivity.RUN_DEBUG -> UnavailableCard("Debugger not connected", "Run configurations and a debugger are not available in this build.")
            IdeActivity.EXTENSIONS -> UnavailableCard("Reverso Hub not connected", "Extension discovery and installation are unavailable until the Hub service and safe extension runtime are implemented.")
            IdeActivity.SETTINGS -> {
                Text("Workspace: ${workspace ?: "None open"}", color = ReversoColors.Muted)
                Text("Editor engine: Android Compose text input. Rust/JNI connection is pending.", color = ReversoColors.Muted)
                Button(onClick = onAiSettings, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.AutoAwesome, null); Text("  Configure AI provider") }
                Text("AI keys are stored encrypted in Android Keystore. Requests are sent directly to the configured HTTPS provider.", color = ReversoColors.Muted, style = MaterialTheme.typography.bodySmall)
                HorizontalDivider(color = ReversoColors.Border)
                UpdateSettingsScreen(
                    currentVersion, latestRelease?.tag, updateHistory, checkedAt, updateError, checkingUpdates,
                    automaticUpdates, betaUpdates, updateNotifications, notificationsPermissionGranted,
                    onAutomaticUpdates, onBetaUpdates, onUpdateNotifications, onRequestNotificationPermission,
                    onCheckUpdates, onDownloadUpdate, onViewRelease,
                )
            }
            else -> {
                Text("Workspace: ${workspace ?: "No folder open"}", color = ReversoColors.Muted)
                OutlinedButton(onClick = onOpenFolder, modifier = Modifier.fillMaxWidth()) { Text("Open Folder") }
                OutlinedButton(onClick = onOpenDocument, modifier = Modifier.fillMaxWidth()) { Text("Open File") }
            }
        }
    }
}

@Composable private fun UnavailableCard(title: String, detail: String) { Surface(color = ReversoColors.Panel, shape = RoundedCornerShape(12.dp)) { Column(Modifier.padding(16.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(6.dp)); Text(detail, color = ReversoColors.Muted) } } }

@Composable private fun BottomPanel(tool: BottomTool, output: List<String>, modifier: Modifier, onTool: (BottomTool) -> Unit, onClose: () -> Unit, onDrag: (Float) -> Unit) {
    Column(modifier.fillMaxWidth().background(ReversoColors.Surface)) {
        Box(Modifier.fillMaxWidth().height(18.dp).pointerInput(Unit) { detectVerticalDragGestures { _, drag -> onDrag(drag) } }, contentAlignment = Alignment.Center) {
            Box(Modifier.width(42.dp).height(3.dp).background(ReversoColors.Muted, RoundedCornerShape(2.dp)))
        }
        Row(Modifier.fillMaxWidth().height(44.dp).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) { BottomTool.entries.forEach { TextButton(onClick = { onTool(it) }) { Text(it.label, color = if (tool == it) ReversoColors.Blue else ReversoColors.Muted) } }; Spacer(Modifier.weight(1f)); IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Close panel") } }
        when (tool) {
            BottomTool.OUTPUT -> LazyColumn(Modifier.weight(1f, fill = false).padding(horizontal = 12.dp)) { items(output) { Text(it, color = ReversoColors.Muted, style = MaterialTheme.typography.bodySmall) } }
            BottomTool.PROBLEMS -> Text("No language server is connected. Diagnostics are unavailable.", Modifier.padding(12.dp), color = ReversoColors.Muted)
            BottomTool.TERMINAL -> UnavailableCard("Termux terminal bridge unavailable", "REVERSO does not execute commands itself. Install/configure a supported authenticated Termux companion bridge when available. No terminal session is being faked.")
            BottomTool.DEBUG_CONSOLE -> Text("No debugger is connected.", Modifier.padding(12.dp), color = ReversoColors.Muted)
            BottomTool.PORTS -> Text("Port forwarding is not connected. No local or remote ports are being reported.", Modifier.padding(12.dp), color = ReversoColors.Muted)
        }
    }
}

@Composable private fun StatusBar(tab: EditorTab, message: String) {
    val cursor = tab.value.selection.end.coerceIn(0, tab.value.text.length)
    val before = tab.value.text.take(cursor)
    val line = before.count { it == '\n' } + 1
    val column = before.length - (before.lastIndexOf('\n') + 1) + 1
    Row(Modifier.fillMaxWidth().height(28.dp).background(ReversoColors.Blue).padding(horizontal = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Git unavailable", color = ReversoColors.Background, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        Spacer(Modifier.width(8.dp))
        Text("${languageForFile(tab.title)} · UTF-8 · Spaces: 4 · Ln $line, Col $column", Modifier.weight(1f), color = ReversoColors.Background, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(8.dp))
        Text(message, Modifier.weight(0.7f), color = ReversoColors.Background, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
