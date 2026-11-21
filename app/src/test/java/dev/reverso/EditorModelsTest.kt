package dev.reverso

import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextRange
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorModelsTest {
    @Test fun foldersSortBeforeFilesAndNamesSortCaseInsensitively() {
        val input = listOf("z.rs" to false, "src" to true, "Assets" to true, "A.kt" to false)
        val sorted = input.sortedWith { a, b -> compareWorkspaceEntries(a.second, a.first, b.second, b.first) }
        assertEquals(listOf("Assets", "src", "A.kt", "z.rs"), sorted.map { it.first })
    }

    @Test fun languageIsDerivedFromFileExtension() {
        assertEquals("Kotlin", languageForFile("MainActivity.kt"))
        assertEquals("Plain Text", languageForFile("README"))
    }

    @Test fun keyboardToolbarReplacesSelectionAndMovesCaret() {
        val tab = EditorTab("a", "a.rs", value = TextFieldValue("abcd", TextRange(1, 3)))
        val updated = insertAtSelection(tab, "{}")
        assertEquals("a{}d", updated.value.text)
        assertEquals(TextRange(3), updated.value.selection)
    }
}
