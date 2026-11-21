package dev.reverso

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseUpdateTest {
    private fun release(tag: String, beta: Boolean = false, apk: ReleaseAsset? = null) = AvailableRelease(
        tag, tag, "notes", "https://github.com/mryuvraj1/Reverso/releases/tag/$tag", "2026-11-22T00:00:00Z", beta, apk,
    )

    @Test fun newerStableIsSelectedAndInstalledVersionDoesNotTriggerAnUpdate() {
        val stable = release("v0.2.0")
        assertEquals(stable, selectNewerRelease(listOf(stable), "0.1.0", includeBeta = false))
        assertNull(selectNewerRelease(listOf(stable), "0.2.0", includeBeta = false))
    }

    @Test fun betaReleaseIsIgnoredUntilBetaChannelIsEnabled() {
        val beta = release("0.3.0-rc.1", beta = true)
        assertNull(selectNewerRelease(listOf(beta), "0.2.0", includeBeta = false))
        assertEquals(beta, selectNewerRelease(listOf(beta), "0.2.0", includeBeta = true))
    }

    @Test fun aReleaseWithoutAnApkRemainsVisibleAsSourceOnly() {
        val sourceOnly = release("0.2.0")
        assertNull(sourceOnly.apk)
        assertEquals(sourceOnly, selectNewerRelease(listOf(sourceOnly), "0.1.0", includeBeta = false))
    }

    @Test fun notificationAndChecksumHelpersRejectDuplicatesAndMismatches() {
        assertTrue(releaseNeedsNotification("0.2.0", null))
        assertFalse(releaseNeedsNotification("0.2.0", "0.2.0"))
        assertTrue(sha256DigestMatches("sha256:ABC123", "abc123"))
        assertFalse(sha256DigestMatches("sha256:ABC123", "wrong"))
        assertFalse(sha256DigestMatches("md5:ABC123", "abc123"))
    }

    @Test fun failedNetworkCheckKeepsCachedReleaseAndReportsTheFailure() {
        val cached = listOf(release("0.2.0"))
        val result = releaseCheckFailure(cached, 1234L, "offline")
        assertEquals(cached, result.releases)
        assertEquals(1234L, result.checkedAt)
        assertEquals("offline", result.error)
    }
}
