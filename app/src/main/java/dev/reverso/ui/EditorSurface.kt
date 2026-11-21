package dev.reverso.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.reverso.EditorTab
import dev.reverso.languageForFile

@Composable
fun EditorTabs(
    tabs: List<EditorTab>,
    activeId: String?,
    onSelect: (EditorTab) -> Unit,
    onClose: (EditorTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().background(ReversoColors.Background).horizontalScroll(rememberScrollState()),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val selected = tab.id == activeId
            Row(
                Modifier.background(if (selected) ReversoColors.Panel else ReversoColors.Background)
                    .padding(start = 14.dp, end = 5.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Code, null, tint = if (selected) ReversoColors.Blue else ReversoColors.Muted, modifier = Modifier.size(16.dp))
                TextButton(onClick = { onSelect(tab) }) {
                    Text((if (tab.dirty) "● " else "") + tab.title, color = if (selected) ReversoColors.Ink else ReversoColors.Muted, maxLines = 1)
                }
                IconButton(onClick = { onClose(tab) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Close, "Close ${tab.title}", tint = ReversoColors.Muted, modifier = Modifier.size(15.dp))
                }
            }
            VerticalDivider(color = ReversoColors.Border, modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun Breadcrumbs(workspaceName: String?, path: List<String>, onWorkspaceClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(ReversoColors.Background).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        TextButton(onClick = onWorkspaceClick, contentPadding = PaddingValues(0.dp)) {
            Text(workspaceName ?: "Reverso", color = ReversoColors.Muted, style = MaterialTheme.typography.labelMedium)
        }
        path.forEach { segment ->
            Icon(Icons.Filled.ChevronRight, null, tint = ReversoColors.Muted, modifier = Modifier.size(16.dp))
            Text(segment, color = ReversoColors.Ink, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@Composable
fun EditorSurface(
    tab: EditorTab,
    onValueChange: (EditorTab) -> Unit,
    fontSize: Int,
    modifier: Modifier = Modifier,
    showMinimap: Boolean = false,
) {
    val lineCount = remember(tab.value.text) { tab.value.text.count { it == '\n' } + 1 }
    val scroll = rememberScrollState()
    Row(modifier.fillMaxSize().background(ReversoColors.Panel)) {
        Column(
            Modifier.width(42.dp).fillMaxHeight().background(ReversoColors.Panel).verticalScroll(scroll)
                .padding(top = 12.dp, end = 10.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.End,
        ) {
            Text(
                (1..lineCount).joinToString("\n"),
                color = ReversoColors.Muted.copy(alpha = 0.7f),
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = fontSize.sp, lineHeight = (fontSize + 8).sp),
            )
        }
        BasicTextField(
            value = tab.value,
            onValueChange = { onValueChange(tab.copy(value = it, dirty = true)) },
            modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 8.dp, vertical = 10.dp),
            textStyle = TextStyle(color = ReversoColors.Ink, fontFamily = FontFamily.Monospace, fontSize = fontSize.sp, lineHeight = (fontSize + 8).sp),
            visualTransformation = ReversoSyntaxHighlight(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            maxLines = Int.MAX_VALUE,
        )
        if (showMinimap) CodeMinimap(tab.value.text, Modifier.width(38.dp).fillMaxHeight().padding(5.dp))
    }
}

@Composable
private fun CodeMinimap(source: String, modifier: Modifier = Modifier) {
    val lines = remember(source) { source.lines().take(100) }
    Canvas(modifier) {
        val lineHeight = size.height / maxOf(lines.size, 1)
        lines.forEachIndexed { index, line ->
            val width = (line.length * 0.55f).coerceIn(4f, size.width)
            drawRoundRect(
                color = if (index % 5 == 0) ReversoColors.Purple.copy(alpha = 0.55f) else ReversoColors.Blue.copy(alpha = 0.33f),
                topLeft = Offset(0f, index * lineHeight),
                size = Size(width, maxOf(1f, lineHeight * 0.42f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f),
            )
        }
    }
}

@Composable
fun KeyboardToolbar(onInsert: (String) -> Unit) {
    val keys = listOf("Tab" to "\t", "{" to "{}", "(" to "()", "[" to "[]", "->" to "->", "=" to "=", ";" to ";", "\"" to "\"", "'" to "'", "#" to "#")
    Row(
        Modifier.fillMaxWidth().background(ReversoColors.Raised).horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        keys.forEach { (label, value) ->
            TextButton(
                onClick = { onInsert(value) },
                modifier = Modifier.heightIn(min = 38.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = ReversoColors.Ink),
            ) { Text(label, fontFamily = FontFamily.Monospace) }
        }
    }
}
