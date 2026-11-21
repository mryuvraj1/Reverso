package dev.reverso

import android.net.Uri
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextRange
import androidx.documentfile.provider.DocumentFile

internal enum class IdeActivity(val label: String) {
    EXPLORER("Explorer"), SEARCH("Search"), SOURCE_CONTROL("Source Control"),
    RUN_DEBUG("Run & Debug"), EXTENSIONS("Extensions"), SETTINGS("Settings"),
}

internal enum class BottomTool(val label: String) {
    TERMINAL("Terminal"), PROBLEMS("Problems"), OUTPUT("Output"), DEBUG_CONSOLE("Debug Console"), GIT("Git"),
}

internal data class EditorTab(
    val id: String,
    val title: String,
    val uri: Uri? = null,
    val value: TextFieldValue = TextFieldValue(""),
    val dirty: Boolean = false,
)

internal data class WorkspaceItem<T>(
    val id: String,
    val name: String,
    val uri: Uri,
    val isDirectory: Boolean,
    val depth: Int,
    val payload: T,
)

internal fun <T> sortWorkspaceItems(items: List<WorkspaceItem<T>>): List<WorkspaceItem<T>> =
    items.sortedWith { left, right -> compareWorkspaceEntries(left.isDirectory, left.name, right.isDirectory, right.name) }

internal fun compareWorkspaceEntries(leftDirectory: Boolean, leftName: String, rightDirectory: Boolean, rightName: String): Int =
    compareValues(!leftDirectory, !rightDirectory).takeIf { it != 0 }
        ?: leftName.lowercase().compareTo(rightName.lowercase())

internal fun languageForFile(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
    "kt", "kts" -> "Kotlin"
    "rs" -> "Rust"
    "go" -> "Go"
    "ts", "tsx" -> "TypeScript"
    "js", "jsx" -> "JavaScript"
    "json" -> "JSON"
    "xml" -> "XML"
    "html", "htm" -> "HTML"
    "css" -> "CSS"
    "md", "markdown" -> "Markdown"
    "py" -> "Python"
    "sh", "bash" -> "Shell"
    "yml", "yaml" -> "YAML"
    else -> "Plain Text"
}

internal fun insertAtSelection(tab: EditorTab, inserted: String, indentation: String = "    "): EditorTab {
    val selection = tab.value.selection
    val start = minOf(selection.start, selection.end)
    val end = maxOf(selection.start, selection.end)
    val insertion = if (inserted == "\t") indentation else inserted
    val text = tab.value.text.replaceRange(start, end, insertion)
    val cursor = start + insertion.length
    return tab.copy(value = TextFieldValue(text, TextRange(cursor)))
}

internal data class ProjectWorkspace(val rootUri: Uri, val root: DocumentFile, val name: String)
internal data class RecentProject(val name: String, val uri: String)
