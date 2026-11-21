package dev.reverso.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.reverso.AiMessage
import dev.reverso.AiProvider
import dev.reverso.AiProviderConfig

@Composable
internal fun AiProviderSettingsScreen(
    initialConfig: AiProviderConfig?,
    configFor: (AiProvider) -> AiProviderConfig,
    credentialSavedFor: (AiProvider) -> Boolean,
    onSave: (AiProviderConfig, String) -> String?,
    onClear: (AiProvider) -> String?,
    onDone: () -> Unit,
) {
    var provider by remember { mutableStateOf(initialConfig?.provider ?: AiProvider.OPENAI) }
    var endpoint by remember { mutableStateOf(initialConfig?.endpoint ?: AiProvider.OPENAI.defaultEndpoint) }
    var model by remember { mutableStateOf(initialConfig?.model ?: AiProvider.OPENAI.defaultModel) }
    var apiKey by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val credentialSaved = credentialSavedFor(provider)

    Column(
        Modifier.fillMaxWidth().heightIn(max = 640.dp).verticalScroll(rememberScrollState())
            .background(ReversoColors.Background).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AutoAwesome, null, tint = ReversoColors.Purple)
            Text("  AI provider", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = onDone) { Text("Done") }
        }
        Text("Requests go directly from this device to the selected provider over HTTPS. Reverso does not proxy or log them. Provider billing and data terms apply to your account.", color = ReversoColors.Muted, style = MaterialTheme.typography.bodySmall)
        Box {
            OutlinedButton(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("Provider: ${provider.label}") }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                AiProvider.entries.forEach { candidate -> DropdownMenuItem(
                    text = { Text(candidate.label) },
                    onClick = {
                        provider = candidate
                        val candidateConfig = configFor(candidate)
                        endpoint = candidateConfig.endpoint
                        model = candidateConfig.model
                        menuOpen = false
                    },
                ) }
            }
        }
        OutlinedTextField(endpoint, { endpoint = it }, Modifier.fillMaxWidth(), label = { Text("HTTPS API endpoint") }, singleLine = true)
        OutlinedTextField(model, { model = it }, Modifier.fillMaxWidth(), label = { Text("Model ID from your provider") }, singleLine = true)
        OutlinedTextField(
            apiKey, { apiKey = it }, Modifier.fillMaxWidth(),
            label = { Text(if (credentialSaved) "API key (leave blank to keep saved key)" else "API key") },
            visualTransformation = PasswordVisualTransformation(), singleLine = true,
        )
        Text(
            if (credentialSaved) "An API key is stored encrypted with Android Keystore."
            else "No API key is saved yet. Reverso requires a key before sending requests.",
            color = ReversoColors.Muted, style = MaterialTheme.typography.bodySmall,
        )
        notice?.let { Text(it, color = ReversoColors.Warning, style = MaterialTheme.typography.bodySmall) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(enabled = !busy, onClick = {
                val config = AiProviderConfig(provider, endpoint.trim(), model.trim())
                notice = config.validationError()
                if (notice == null) {
                    busy = true
                    notice = onSave(config, apiKey).also { busy = false }
                    if (notice == null) { apiKey = ""; onDone() }
                }
            }, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.Check, null); Text(" Save") }
            OutlinedButton(enabled = !busy && credentialSaved, onClick = {
                notice = onClear(provider)
                if (notice == null) { apiKey = ""; onDone() }
            }) { Text("Clear key") }
        }
    }
}

@Composable
internal fun AiChatScreen(
    messages: List<AiMessage>,
    configured: Boolean,
    activeFileName: String?,
    loading: Boolean,
    error: String?,
    onConfigure: () -> Unit,
    onNewChat: () -> Unit,
    onSend: (String, Boolean) -> Unit,
    onApplyLatest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf("") }
    var includeFile by remember(activeFileName) { mutableStateOf(false) }
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, loading) {
        val lastIndex = messages.size + if (loading) 1 else 0
        if (lastIndex > 0) listState.animateScrollToItem(lastIndex - 1)
    }

    Column(modifier.fillMaxSize().background(ReversoColors.Surface)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AutoAwesome, null, tint = ReversoColors.Purple)
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text("Reverso AI", style = MaterialTheme.typography.titleMedium)
                Text(if (configured) "Connected provider" else "Provider setup required", color = ReversoColors.Muted, style = MaterialTheme.typography.labelSmall)
            }
            if (messages.isNotEmpty()) TextButton(onClick = onNewChat, enabled = !loading) { Text("New chat") }
            TextButton(onClick = onConfigure) { Text("Configure") }
        }
        HorizontalDivider(color = ReversoColors.Border)
        if (!configured) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Connect an AI provider to start a chat. Your key stays encrypted on this device.", color = ReversoColors.Muted)
                Spacer(Modifier.height(8.dp)); OutlinedButton(onClick = onConfigure) { Text("Set up provider") }
            }
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            state = listState, contentPadding = PaddingValues(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(messages) { message ->
                val isUser = message.role == "user"
                Surface(
                    color = if (isUser) ReversoColors.Raised else ReversoColors.Background,
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(if (isUser) "You" else "Reverso AI", color = if (isUser) ReversoColors.Blue else ReversoColors.Purple, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(5.dp))
                        Text(message.content, color = ReversoColors.Ink, fontFamily = if (message.content.contains("```")) FontFamily.Monospace else FontFamily.Default)
                        if (!isUser && message == messages.lastOrNull()) {
                            Spacer(Modifier.height(6.dp))
                            TextButton(onClick = onApplyLatest) { Text("Apply response to editor…") }
                        }
                    }
                }
            }
            if (loading) item { Text("Waiting for provider…", color = ReversoColors.Muted, modifier = Modifier.padding(8.dp)) }
        }
        if (error != null) Text(error, color = ReversoColors.Warning, modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = includeFile, onCheckedChange = { includeFile = it }, enabled = activeFileName != null)
            Text(if (activeFileName == null) "No active file" else "Include $activeFileName", color = ReversoColors.Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = draft, onValueChange = { draft = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("Ask about code, or describe a change…") }, minLines = 1, maxLines = 4,
                enabled = configured && !loading,
            )
            IconButton(
                onClick = { val text = draft.trim(); if (text.isNotEmpty()) { onSend(text, includeFile); draft = "" } },
                enabled = configured && !loading && draft.isNotBlank(),
            ) { Icon(Icons.Filled.Send, "Send prompt", tint = ReversoColors.Blue) }
        }
    }
}
