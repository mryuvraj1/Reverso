package dev.reverso.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.reverso.GitChange
import dev.reverso.GitChangeKind
import dev.reverso.GitSnapshot

internal data class GitScreenState(
    val snapshot: GitSnapshot?, val privateWorkspace: Boolean, val tokenSaved: Boolean,
    val loading: Boolean, val progress: String?, val error: String?, val commitMessage: String,
    val authorName: String, val authorEmail: String, val branchName: String,
)
internal data class GitScreenActions(
    val onClone: () -> Unit, val onRefresh: () -> Unit, val onStage: (GitChange) -> Unit,
    val onUnstage: (GitChange) -> Unit, val onStageAll: () -> Unit, val onCommit: () -> Unit,
    val onCommitMessage: (String) -> Unit, val onAuthorName: (String) -> Unit,
    val onAuthorEmail: (String) -> Unit, val onCreateBranch: () -> Unit,
    val onBranchName: (String) -> Unit, val onCheckout: (String) -> Unit,
    val onPull: () -> Unit, val onPush: () -> Unit, val onSaveToken: (String) -> Unit,
    val onClearToken: () -> Unit,
)

@Composable
internal fun GitPanelScreen(
    state: GitScreenState,
    actions: GitScreenActions,
    modifier: Modifier = Modifier,
) {
    val snapshot = state.snapshot
    var branchMenu by remember { mutableStateOf(false) }
    var showTokenEditor by remember(state.tokenSaved) { mutableStateOf(!state.tokenSaved) }
    var token by remember { mutableStateOf("") }
    Column(modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Source Control", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = ReversoColors.Ink)
            IconButton(onClick = actions.onRefresh, enabled = !state.loading && state.privateWorkspace) { Icon(Icons.Filled.Refresh, "Refresh Git status", tint = ReversoColors.Muted) }
            IconButton(onClick = actions.onClone, enabled = !state.loading) { Icon(Icons.Filled.CloudDownload, "Clone Git repository", tint = ReversoColors.Blue) }
        }
        if (snapshot == null) {
            Surface(color = ReversoColors.Panel, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (state.privateWorkspace) "No repository detected" else "Local Git workspace required", color = ReversoColors.Ink, style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (state.privateWorkspace) "This folder does not contain a Git repository. Clone a GitHub repository into Reverso's private workspace to use JGit operations."
                        else "SAF providers expose content URIs, not a general local filesystem path. JGit operations are available for repositories cloned into Reverso's private workspace; this SAF folder remains fully editable.",
                        color = ReversoColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = actions.onClone, modifier = Modifier.fillMaxWidth()) { Text("Clone Git Repository") }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Branch", color = ReversoColors.Muted, style = MaterialTheme.typography.labelMedium)
                Box {
                    OutlinedButton(onClick = { branchMenu = true }, enabled = !state.loading) { Text(snapshot.branch, maxLines = 1) }
                    DropdownMenu(expanded = branchMenu, onDismissRequest = { branchMenu = false }) {
                        snapshot.branches.forEach { branch -> DropdownMenuItem(text = { Text(branch) }, onClick = { branchMenu = false; actions.onCheckout(branch) }) }
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = actions.onPull, enabled = !state.loading) { Text("Pull") }
                TextButton(onClick = actions.onPush, enabled = !state.loading) { Icon(Icons.Filled.Upload, null); Text("Push") }
            }
            snapshot.remotes.forEach { (name, url) -> Text("$name · $url", maxLines = 1, color = ReversoColors.Muted, style = MaterialTheme.typography.labelSmall) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(state.branchName, actions.onBranchName, Modifier.weight(1f), label = { Text("New branch") }, singleLine = true)
                IconButton(onClick = actions.onCreateBranch, enabled = !state.loading && state.branchName.isNotBlank()) { Icon(Icons.Filled.Add, "Create branch", tint = ReversoColors.Blue) }
            }
            HorizontalDivider(color = ReversoColors.Border)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Changes (${snapshot.changes.size})", Modifier.weight(1f), color = ReversoColors.Ink, style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = actions.onStageAll, enabled = !state.loading && snapshot.changes.isNotEmpty()) { Text("Stage all") }
            }
            if (snapshot.changes.isEmpty()) Text("Working tree clean", color = ReversoColors.Muted, style = MaterialTheme.typography.bodySmall)
            else {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    snapshot.changes.forEach { change ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(change.path, color = ReversoColors.Ink, maxLines = 1)
                            Text(change.kind.label(), color = ReversoColors.Muted, style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { if (change.kind == GitChangeKind.STAGED) actions.onUnstage(change) else actions.onStage(change) }, enabled = !state.loading) {
                            Text(if (change.kind == GitChangeKind.STAGED) "Unstage" else "Stage")
                        }
                    }
                }
            }
            }
            OutlinedTextField(state.authorName, actions.onAuthorName, Modifier.fillMaxWidth(), label = { Text("Commit author") }, singleLine = true)
            OutlinedTextField(state.authorEmail, actions.onAuthorEmail, Modifier.fillMaxWidth(), label = { Text("Commit email") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            OutlinedTextField(state.commitMessage, actions.onCommitMessage, Modifier.fillMaxWidth(), label = { Text("Commit message") }, minLines = 1, maxLines = 3)
            Button(onClick = actions.onCommit, enabled = !state.loading && state.commitMessage.isNotBlank() && snapshot.changes.any { it.kind == GitChangeKind.STAGED }, modifier = Modifier.fillMaxWidth()) { Text("Commit staged changes") }
            Text("Recent commits", color = ReversoColors.Ink, style = MaterialTheme.typography.titleSmall)
            snapshot.history.take(8).forEach { commit -> Text("${commit.id.take(7)}  ${commit.message}", color = ReversoColors.Muted, maxLines = 1, style = MaterialTheme.typography.bodySmall) }
        }
        if (state.tokenSaved) TextButton(onClick = actions.onClearToken, enabled = !state.loading) { Text("Forget encrypted GitHub token") }
        else if (showTokenEditor) {
            OutlinedTextField(token, { token = it }, Modifier.fillMaxWidth(), label = { Text("Fine-grained GitHub token (optional for public clone)") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
            Text("Token is encrypted with Android Keystore. For private clone/push, grant the selected repositories Contents read/write access.", color = ReversoColors.Muted, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { if (token.isNotBlank()) { actions.onSaveToken(token); token = ""; showTokenEditor = false } }, enabled = token.isNotBlank() && !state.loading, modifier = Modifier.fillMaxWidth()) { Text("Save token securely") }
        }
        state.progress?.let { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Text(it, color = ReversoColors.Muted, style = MaterialTheme.typography.bodySmall) } }
        state.error?.let { Text(it, color = ReversoColors.Warning, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
internal fun GitCloneDialog(
    loading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onClone: (url: String, directory: String, token: String) -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var directory by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text("Clone GitHub repository") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Clone runs with JGit into Reverso's private app workspace. Your SAF folders remain separate and editable.", color = ReversoColors.Muted, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(url, { value -> url = value; if (directory.isBlank()) directory = value.trimEnd('/').substringAfterLast('/').removeSuffix(".git") }, Modifier.fillMaxWidth(), label = { Text("HTTPS GitHub repository URL") }, singleLine = true)
                OutlinedTextField(directory, { directory = it }, Modifier.fillMaxWidth(), label = { Text("Repository folder name") }, singleLine = true)
                OutlinedTextField(token, { token = it }, Modifier.fillMaxWidth(), label = { Text("Fine-grained token (private repositories)") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                error?.let { Text(it, color = ReversoColors.Warning, style = MaterialTheme.typography.bodySmall) }
                if (loading) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Text("Cloning repository…", color = ReversoColors.Muted) }
            }
        },
        confirmButton = { TextButton(onClick = { onClone(url, directory, token) }, enabled = !loading && url.isNotBlank() && directory.isNotBlank()) { Text("Clone") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !loading) { Text("Cancel") } },
    )
}

private fun GitChangeKind.label() = when (this) {
    GitChangeKind.STAGED -> "Staged"
    GitChangeKind.MODIFIED -> "Modified"
    GitChangeKind.UNTRACKED -> "Untracked"
    GitChangeKind.MISSING -> "Deleted"
    GitChangeKind.CONFLICT -> "Merge conflict"
}
