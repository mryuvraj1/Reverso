package dev.reverso

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.transport.RefSpec
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

internal enum class GitChangeKind { STAGED, MODIFIED, UNTRACKED, MISSING, CONFLICT }
internal data class GitChange(val path: String, val kind: GitChangeKind)
internal data class GitCommit(val id: String, val message: String, val author: String, val timeSeconds: Int)
internal data class GitSnapshot(val branch: String, val changes: List<GitChange>, val branches: List<String>, val remotes: Map<String, String>, val history: List<GitCommit>)

/** JGit works on a real filesystem. Reverso keeps cloned repos in its private files directory. */
internal object GitRepository {
    private const val DIRECTORY = "git-repositories"
    private val githubUrl = Regex("^https://github\\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+(?:\\.git)?/?$")

    fun validateCloneUrl(value: String): String? = when {
        value.length > 2_048 -> "Repository URL is too long"
        !githubUrl.matches(value.trim()) -> "Use an HTTPS GitHub URL such as https://github.com/owner/repository.git"
        value.contains("..") -> "Repository URL contains an invalid path"
        else -> null
    }

    fun validateDirectoryName(value: String): String? = validateDocumentName(value.trim())

    fun clone(
        context: Context,
        url: String,
        directoryName: String,
        token: String?,
        progress: ProgressMonitor? = null,
    ): File {
        validateCloneUrl(url)?.let { error(it) }
        validateDirectoryName(directoryName)?.let { error(it) }
        val base = File(context.filesDir, DIRECTORY).canonicalFile.apply { if (!exists() && !mkdirs()) error("Could not create Reverso's private repository folder") }
        val target = File(base, directoryName.trim()).canonicalFile
        require(target.parentFile == base && !target.exists()) { "A repository already uses that name" }
        try {
            val command = Git.cloneRepository().setURI(url.trim()).setDirectory(target)
            if (!token.isNullOrBlank()) command.setCredentialsProvider(credentials(token))
            if (progress != null) command.setProgressMonitor(progress)
            command.call().use { }
            return target
        } catch (failure: Exception) {
            target.deleteRecursively()
            throw GitOperationException(friendlyError(failure), failure)
        }
    }

    fun snapshot(root: File): GitSnapshot = withGit(root) { git ->
        val status = git.status().call()
        val changes = buildList {
            status.added.forEach { add(GitChange(it, GitChangeKind.STAGED)) }
            status.changed.forEach { add(GitChange(it, GitChangeKind.STAGED)) }
            status.removed.forEach { add(GitChange(it, GitChangeKind.STAGED)) }
            status.modified.forEach { add(GitChange(it, GitChangeKind.MODIFIED)) }
            status.missing.forEach { add(GitChange(it, GitChangeKind.MISSING)) }
            status.untracked.forEach { add(GitChange(it, GitChangeKind.UNTRACKED)) }
            status.conflicting.forEach { add(GitChange(it, GitChangeKind.CONFLICT)) }
        }.distinctBy { it.path to it.kind }.sortedBy { it.path.lowercase() }
        val branches = git.branchList().call().map { it.name.removePrefix("refs/heads/") }.sorted()
        val remotes = git.remoteList().call().associate { it.name to (it.uris.firstOrNull()?.toString() ?: "") }
        val history = git.log().setMaxCount(40).call().map { commit ->
            GitCommit(commit.name, commit.shortMessage, commit.authorIdent.name, commit.commitTime)
        }
        GitSnapshot(git.repository.branch ?: "(detached HEAD)", changes, branches, remotes, history)
    }

    fun stage(root: File, path: String) = withGit(root) { git ->
        validateRepoPath(path)
        val file = File(root, path).canonicalFile
        require(file.toPath().startsWith(root.canonicalFile.toPath())) { "Repository path escapes the workspace" }
        if (file.exists()) git.add().addFilepattern(path).call() else git.rm().addFilepattern(path).call()
    }

    fun stageAll(root: File) = withGit(root) { git -> git.add().addFilepattern(".").call() }

    fun unstage(root: File, path: String) = withGit(root) { git ->
        validateRepoPath(path)
        git.reset().addPath(path).call()
    }

    fun commit(root: File, message: String, authorName: String, authorEmail: String) = withGit(root) { git ->
        require(message.isNotBlank()) { "Enter a commit message" }
        require(message.length <= 500) { "Commit message is limited to 500 characters" }
        require(authorName.isNotBlank() && authorEmail.contains('@')) { "Set a Git author name and email before committing" }
        git.commit().setMessage(message.trim()).setAuthor(authorName.trim(), authorEmail.trim()).setCommitter(authorName.trim(), authorEmail.trim()).call().name
    }

    fun createBranch(root: File, name: String) = withGit(root) { git ->
        validateBranchName(name)
        git.branchCreate().setName(name.trim()).call()
        git.checkout().setName(name.trim()).call()
    }

    fun checkout(root: File, name: String) = withGit(root) { git ->
        require(name in git.branchList().call().map { it.name.removePrefix("refs/heads/") }) { "Branch does not exist" }
        git.checkout().setName(name).call()
    }

    fun pull(root: File, token: String?) = withGit(root) { git ->
        require(git.repository.config.getString("branch", git.repository.branch, "remote") != null) { "The current branch has no remote tracking branch" }
        val command = git.pull()
        if (!token.isNullOrBlank()) command.setCredentialsProvider(credentials(token))
        val result = command.call()
        if (!result.isSuccessful) error("Pull did not complete cleanly (${result.mergeResult?.mergeStatus ?: "${result.rebaseResult?.status}"})")
        result
    }

    fun push(root: File, token: String?) = withGit(root) { git ->
        val branch = requireNotNull(git.repository.branch) { "Push requires a checked-out local branch" }
        val command = git.push().setRemote("origin")
            .setRefSpecs(RefSpec("refs/heads/$branch:refs/heads/$branch"))
        if (!token.isNullOrBlank()) command.setCredentialsProvider(credentials(token))
        command.call().forEach { result ->
            val rejection = result.remoteUpdates.firstOrNull { it.status.name.contains("REJECTED") || it.status.name.contains("ERROR") }
            if (rejection != null) error("Push rejected: ${rejection.message ?: rejection.status}")
        }
    }

    fun repositoryDirectory(context: Context, relativeName: String): File? {
        validateDirectoryName(relativeName)?.let { return null }
        val base = File(context.filesDir, DIRECTORY).canonicalFile
        val directory = File(base, relativeName).canonicalFile
        return directory.takeIf { it.parentFile == base && it.isDirectory && File(it, ".git").isDirectory }
    }

    fun isManagedRepository(context: Context, file: File): Boolean = runCatching {
        val base = File(context.filesDir, DIRECTORY).canonicalFile
        val canonical = file.canonicalFile
        canonical.path.startsWith(base.path + File.separator) && File(canonical, ".git").isDirectory
    }.getOrDefault(false)

    private fun <T> withGit(root: File, block: (Git) -> T): T {
        require(root.isDirectory && File(root, ".git").exists()) { "This folder is not a local Git repository" }
        require(root.canonicalFile == root) { "Repository path is not canonical" }
        return try {
            Git.open(root).use(block)
        } catch (failure: Exception) {
            throw GitOperationException(friendlyError(failure), failure)
        }
    }

    private fun validateRepoPath(path: String) {
        require(path.isNotBlank() && !path.startsWith('/') && path.split('/').none { it == ".." || it.isEmpty() }) {
            "Invalid repository-relative path"
        }
    }

    private fun validateBranchName(name: String) {
        require(name.isNotBlank() && name.length <= 100 && !name.startsWith('-') && !name.contains("..") &&
            !name.contains("//") && name.none { it.isWhitespace() || it in "~^:?*[\\" }) { "Invalid branch name" }
    }

    private fun credentials(token: String): CredentialsProvider = UsernamePasswordCredentialsProvider("x-access-token", token)

    private fun friendlyError(failure: Exception): String {
        val raw = failure.message.orEmpty()
        return when {
            raw.contains("not authorized", true) || raw.contains("authentication", true) || raw.contains("401") || raw.contains("403") -> "GitHub rejected authentication or repository access. Check the fine-grained token's repository and Contents permissions."
            failure is GitAPIException && raw.isNotBlank() -> raw.take(240)
            raw.isNotBlank() -> raw.take(240)
            else -> "Git operation failed (${failure.javaClass.simpleName})"
        }
    }
}

internal class GitOperationException(message: String, cause: Throwable) : Exception(message, cause)

/** Encrypts a user-supplied GitHub fine-grained PAT with a non-exportable Android Keystore key. */
internal object GitCredentialStore {
    private const val PREFS = "reverso-git-secret-v1"
    private const val KEY = "github_pat"
    private const val ALIAS = "reverso.git.github-pat.v1"
    private const val IV_BYTES = 12

    fun has(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(KEY)

    fun put(context: Context, token: String) {
        require(token.isNotBlank()) { "GitHub token cannot be blank" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.iv + cipher.doFinal(token.trim().toByteArray(Charsets.UTF_8))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply()
    }

    fun get(context: Context): String? = runCatching {
        val value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return null
        val bytes = Base64.decode(value, Base64.NO_WRAP)
        require(bytes.size > IV_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, IV_BYTES)))
        }
        cipher.doFinal(bytes.copyOfRange(IV_BYTES, bytes.size)).toString(Charsets.UTF_8)
    }.getOrNull()

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }

    private fun key(): java.security.Key {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        store.getKey(ALIAS, null)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build())
        }.generateKey()
    }
}
