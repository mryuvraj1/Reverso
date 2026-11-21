package dev.reverso

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import org.json.JSONArray
import org.json.JSONObject

internal object RecentProjects {
    private const val PREFS = "reverso-workspaces"
    private const val KEY = "recent"
    private const val MAX_RECENTS = 8

    fun load(context: Context): List<RecentProject> = runCatching {
        val array = JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]"))
        (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.let {
                RecentProject(it.optString("name"), it.optString("uri"), it.optString("child_name").takeIf(String::isNotBlank))
            }
                ?.takeIf { it.name.isNotBlank() && it.uri.isNotBlank() }
        }
    }.getOrDefault(emptyList())

    fun remember(context: Context, project: RecentProject) {
        val items = (listOf(project) + load(context).filterNot { it.uri == project.uri && it.childName == project.childName }).take(MAX_RECENTS)
        val json = JSONArray().apply {
            items.forEach { put(JSONObject().put("name", it.name).put("uri", it.uri).put("child_name", it.childName)) }
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, json.toString()).apply()
    }

    fun open(context: Context, recent: RecentProject): ProjectWorkspace? = runCatching {
        val treeUri = Uri.parse(recent.uri)
        val selectedTree = DocumentFile.fromTreeUri(context, treeUri)?.takeIf { it.exists() && it.isDirectory } ?: return null
        val root = recent.childName?.let(selectedTree::findFile) ?: selectedTree
        if (!root.exists() || !root.isDirectory) return null
        ProjectWorkspace(root.uri, root, recent.name, treeUri)
    }.getOrNull()
}

internal fun validateDocumentName(name: String): String? = when {
    name.isEmpty() -> "Enter a file or folder name"
    name == "." || name == ".." -> "That name is reserved"
    name.any { it == '/' || it == '\\' || it.code < 32 || it.code == 127 } -> "Names cannot contain path separators or control characters"
    else -> null
}

internal fun mimeForDocumentName(name: String): String {
    val extension = name.substringAfterLast('.', "").lowercase()
    val known = mapOf(
        "txt" to "text/plain", "md" to "text/markdown", "py" to "text/x-python", "js" to "text/javascript",
        "jsx" to "text/javascript", "mjs" to "text/javascript", "ts" to "text/typescript", "tsx" to "text/typescript",
        "html" to "text/html", "htm" to "text/html", "css" to "text/css", "json" to "application/json",
        "xml" to "application/xml", "kt" to "text/x-kotlin", "kts" to "text/x-kotlin", "rs" to "text/x-rust",
        "go" to "text/x-go", "sh" to "text/x-shellscript", "yml" to "application/yaml", "yaml" to "application/yaml",
    )
    return known[extension] ?: "application/octet-stream"
}

internal fun listWorkspaceDirectories(root: DocumentFile, maxDepth: Int = 5): List<DocumentFile> {
    val result = mutableListOf(root)
    fun walk(directory: DocumentFile, depth: Int) {
        if (depth >= maxDepth) return
        runCatching { directory.listFiles().toList() }.getOrDefault(emptyList())
            .filter { it.isDirectory }
            .sortedBy { it.name.orEmpty().lowercase() }
            .forEach { child -> result += child; walk(child, depth + 1) }
    }
    walk(root, 0)
    return result
}

internal enum class WorkspaceFileAction { RENAME, DELETE, COPY, MOVE }

internal fun performWorkspaceFileAction(
    context: Context,
    source: DocumentFile,
    action: WorkspaceFileAction,
    name: String? = null,
    destination: DocumentFile? = null,
): DocumentFile? {
    when (action) {
        WorkspaceFileAction.RENAME -> {
            val newName = requireNotNull(name)
            validateDocumentName(newName)?.let { error(it) }
            val parent = source.parentFile ?: error("The storage provider did not expose the parent folder")
            if (parent.listFiles().any { it.uri != source.uri && it.name == newName }) error("A file or folder named $newName already exists")
            if (!source.renameTo(newName)) error("This storage provider does not support renaming")
            return parent.findFile(newName) ?: error("The renamed document could not be reopened")
        }
        WorkspaceFileAction.DELETE -> {
            if (!source.delete()) error("This storage provider could not delete the document")
            return null
        }
        WorkspaceFileAction.COPY, WorkspaceFileAction.MOVE -> {
            val target = requireNotNull(destination) { "Choose a destination folder" }
            require(target.isDirectory && target.uri != source.uri) { "Choose a different destination folder" }
            if (source.isDirectory) {
                var ancestor: DocumentFile? = target
                repeat(32) {
                    if (ancestor?.uri == source.uri) error("A folder cannot be moved or copied into itself")
                    ancestor = ancestor?.parentFile
                }
            }
            if (target.findFile(source.name.orEmpty()) != null) error("The destination already contains ${source.name}")
            val resultUri = when (action) {
                WorkspaceFileAction.COPY -> DocumentsContract.copyDocument(context.contentResolver, source.uri, target.uri)
                WorkspaceFileAction.MOVE -> {
                    val sourceParent = source.parentFile ?: error("The storage provider did not expose the source folder")
                    DocumentsContract.moveDocument(context.contentResolver, source.uri, sourceParent.uri, target.uri)
                }
                else -> error("Unsupported file operation")
            } ?: error("The storage provider does not support ${action.name.lowercase()}")
            return DocumentFile.fromSingleUri(context, resultUri)
                ?: error("The resulting document could not be reopened")
        }
    }
}

internal fun listWorkspaceItems(
    root: DocumentFile,
    expandedFolders: Set<String>,
    maxDepth: Int = 5,
): List<WorkspaceItem<DocumentFile>> {
    val result = mutableListOf<WorkspaceItem<DocumentFile>>()
    fun walk(directory: DocumentFile, depth: Int) {
        if (depth > maxDepth) return
        val children = runCatching { directory.listFiles().toList() }.getOrDefault(emptyList())
        val items = sortWorkspaceItems(children.map { child ->
            WorkspaceItem(child.uri.toString(), child.name ?: "Untitled", child.uri, child.isDirectory, depth, child)
        })
        items.forEach { item ->
            result += item
            if (item.isDirectory && item.id in expandedFolders) walk(item.payload, depth + 1)
        }
    }
    walk(root, 0)
    return result
}
