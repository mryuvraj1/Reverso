package dev.reverso

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.io.File

private val Ink = Color(0xFFF4F6FC)
private val Base = Color(0xFF0B0F19)
private val Surface = Color(0xFF151B2B)
private val Violet = Color(0xFF7C8CFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReversoApp(File(filesDir, "workspace")) }
    }
}

@Composable
private fun ReversoApp(workspace: File) {
    var files by remember { mutableStateOf(emptyList<File>()) }
    var selected by remember { mutableStateOf<File?>(null) }
    var content by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Local workspace") }
    fun refresh() { workspace.mkdirs(); files = workspace.listFiles()?.filter { it.isFile }?.sortedBy { it.name } ?: emptyList() }
    LaunchedEffect(Unit) { refresh() }
    MaterialTheme(colorScheme = darkColorScheme(primary = Violet, background = Base, surface = Surface, onSurface = Ink)) {
        Column(Modifier.fillMaxSize().background(Base).padding(16.dp)) {
            Text("REVERSO", style = MaterialTheme.typography.titleLarge, color = Ink)
            Text(message, style = MaterialTheme.typography.labelMedium, color = Color(0xFFA0ABC0))
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    workspace.mkdirs(); val f = File(workspace, "untitled-${System.currentTimeMillis()}.txt")
                    f.writeText(""); selected = f; content = ""; refresh(); message = "Created ${f.name}"
                }) { Text("New") }
                Button(enabled = selected != null, onClick = {
                    selected?.let { it.writeText(content); message = "Saved ${it.name}"; refresh() }
                }) { Text("Save") }
            }
            Spacer(Modifier.height(8.dp))
            Text("PROJECT FILES", style = MaterialTheme.typography.labelLarge, color = Violet)
            LazyColumn(Modifier.heightIn(max = 140.dp)) {
                items(files, key = { it.name }) { file ->
                    TextButton(onClick = {
                        runCatching { content = file.readText(); selected = file; message = "Opened ${file.name}" }
                            .onFailure { message = "Could not open ${file.name}: ${it.message}" }
                    }) { Text(if (selected == file) "● ${file.name}" else file.name, color = Ink) }
                }
            }
            Text(selected?.name ?: "No file open", color = Color(0xFFA0ABC0), style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            BasicTextField(value = content, onValueChange = { content = it }, modifier = Modifier.fillMaxSize().background(Surface).padding(12.dp), textStyle = MaterialTheme.typography.bodyMedium.copy(color = Ink), decorationBox = { inner ->
                Box { if (content.isEmpty()) Text("Open or create a file to start editing", color = Color(0xFFA0ABC0)); inner() }
            })
        }
    }
}
