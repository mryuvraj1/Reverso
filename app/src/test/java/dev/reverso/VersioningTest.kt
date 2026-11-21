package dev.reverso

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VersioningTest {
    @Test fun comparesReleaseNumbers() {
        assertEquals(1, compareSemanticVersions("v1.10.0", "1.9.9"))
        assertEquals(-1, compareSemanticVersions("0.1.0", "0.2.0"))
        assertEquals(0, compareSemanticVersions("v2.3.4", "2.3.4"))
    }

    @Test fun stableReleaseIsNewerThanPrerelease() {
        assertEquals(1, compareSemanticVersions("1.0.0", "1.0.0-rc.2"))
        assertEquals(-1, compareSemanticVersions("1.0.0-alpha", "1.0.0"))
        assertEquals(1, compareSemanticVersions("1.0.0-rc.10", "1.0.0-rc.2"))
    }

    @Test fun rejectsNonVersionReleaseTags() {
        assertNull(parseSemanticVersion("Reverso"))
    }
}
