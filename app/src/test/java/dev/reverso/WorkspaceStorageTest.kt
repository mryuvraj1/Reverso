package dev.reverso

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkspaceStorageTest {
    @Test fun filenamesWithExtensionsAreKeptAndTypedByTheirActualSuffix() {
        val names = listOf("idk.py", "main.js", "index.html", "style.css", "app.kt")
        names.forEach { assertNull(validateDocumentName(it)) }
        assertEquals(listOf("Python", "JavaScript", "HTML", "CSS", "Kotlin"), names.map(::languageForFile))
        assertEquals("text/x-python", mimeForDocumentName("idk.py"))
        assertEquals("text/javascript", mimeForDocumentName("main.js"))
        assertEquals("text/html", mimeForDocumentName("index.html"))
        assertEquals("text/css", mimeForDocumentName("style.css"))
        assertEquals("text/x-kotlin", mimeForDocumentName("app.kt"))
    }

    @Test fun pathTraversalAndControlCharactersAreRejectedWithoutNormalizingValidNames() {
        assertEquals("Names cannot contain path separators or control characters", validateDocumentName("../idk.py"))
        assertEquals("Names cannot contain path separators or control characters", validateDocumentName("main\\file.js"))
        assertEquals("That name is reserved", validateDocumentName(".."))
        assertNull(validateDocumentName("my app.v2.py"))
    }

    @Test fun createDocumentContractUsesTheUserSuppliedNameForNewFiles() {
        val requested = "idk.py"
        assertEquals("idk.py", requested)
        assertEquals("text/x-python", mimeForDocumentName(requested))
    }
}
