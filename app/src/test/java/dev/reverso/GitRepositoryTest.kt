package dev.reverso

import org.eclipse.jgit.api.Git
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class GitRepositoryTest {
    @Test fun urlValidationRequiresCredentialFreeGitHubHttpsUrl() {
        assertNull(GitRepository.validateCloneUrl("https://github.com/kotlin/kotlinx.coroutines.git"))
        assertNotNull(GitRepository.validateCloneUrl("http://github.com/owner/repo.git"))
        assertNotNull(GitRepository.validateCloneUrl("https://user:secret@github.com/owner/repo.git"))
        assertNotNull(GitRepository.validateCloneUrl("https://example.com/owner/repo.git"))
        assertNotNull(GitRepository.validateCloneUrl("https://github.com/owner/../other.git"))
    }

    @Test fun jgitStatusStageCommitBranchAndHistoryOperateOnRealFiles() {
        val directory = Files.createTempDirectory("reverso-git-test").toFile()
        try {
            Git.init().setDirectory(directory).call().use { git ->
                git.repository.config.setString("user", null, "name", "Test User")
                git.repository.config.setString("user", null, "email", "test@example.invalid")
                git.repository.config.save()
            }
            File(directory, "main.py").writeText("print('hello')\n")
            assertEquals(GitChangeKind.UNTRACKED, GitRepository.snapshot(directory).changes.single().kind)
            GitRepository.stageAll(directory)
            assertEquals(GitChangeKind.STAGED, GitRepository.snapshot(directory).changes.single().kind)
            GitRepository.commit(directory, "Add Python file", "Test User", "test@example.invalid")
            val committed = GitRepository.snapshot(directory)
            assertTrue(committed.changes.isEmpty())
            assertEquals("Add Python file", committed.history.first().message)
            GitRepository.createBranch(directory, "feature/editor")
            assertEquals("feature/editor", GitRepository.snapshot(directory).branch)
            assertTrue("feature/editor" in GitRepository.snapshot(directory).branches)
            GitRepository.checkout(directory, committed.branch)
            assertEquals(committed.branch, GitRepository.snapshot(directory).branch)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun repositoryRelativePathsAndCommitMessagesAreValidated() {
        assertNotNull(GitRepository.validateDirectoryName("../outside"))
        assertNotNull(GitRepository.validateDirectoryName(""))
        assertNotNull(GitRepository.validateCloneUrl("https://github.com/owner/repo.git?token=secret"))
    }
}
