package dev.reverso

import android.content.Context
import android.net.Uri
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
            array.optJSONObject(index)?.let { RecentProject(it.optString("name"), it.optString("uri")) }
                ?.takeIf { it.name.isNotBlank() && it.uri.isNotBlank() }
        }
    }.getOrDefault(emptyList())

    fun remember(context: Context, project: RecentProject) {
        val items = (listOf(project) + load(context).filterNot { it.uri == project.uri }).take(MAX_RECENTS)
        val json = JSONArray().apply {
            items.forEach { put(JSONObject().put("name", it.name).put("uri", it.uri)) }
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, json.toString()).apply()
    }

    fun open(context: Context, recent: RecentProject): ProjectWorkspace? = runCatching {
        val uri = Uri.parse(recent.uri)
        val root = DocumentFile.fromTreeUri(context, uri)?.takeIf { it.exists() && it.isDirectory } ?: return null
        ProjectWorkspace(uri, root, recent.name)
    }.getOrNull()
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
