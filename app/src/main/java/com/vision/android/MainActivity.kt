package com.vision.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

data class ChatMessage(val text: String, val fromUser: Boolean)

enum class EngineMode { LOCAL, REMOTE }

interface AssistantEngine {
    suspend fun reply(prompt: String): String
}

/** Safe starter engine: explicitly reports that real model inference is not installed yet. */
class LocalEngine : AssistantEngine {
    override suspend fun reply(prompt: String): String =
        "El motor local todavía no está conectado. El siguiente paso es integrar y descargar un modelo GGUF compatible."
}

class VisionViewModel : ViewModel() {
    var messages by mutableStateOf(listOf(ChatMessage(
        "Hola. Soy Vision. Esta es la primera versión del prototipo.", false
    )))
        private set
    var mode by mutableStateOf(EngineMode.LOCAL)
        private set
    var draft by mutableStateOf("")
    var status by mutableStateOf("Modo local · modelo pendiente")

    fun setMode(value: EngineMode) {
        mode = value
        status = if (value == EngineMode.LOCAL) "Modo local · modelo pendiente"
                 else "Modo remoto · falta configurar un servidor"
    }

    fun send() {
        val text = draft.trim()
        if (text.isEmpty()) return
        messages = messages + ChatMessage(text, true)
        draft = ""
        val answer = if (mode == EngineMode.LOCAL) {
            "Recibí tu mensaje. El runtime de IA local aún debe integrarse; por ahora no voy a simular una respuesta inteligente."
        } else {
            "El modo remoto está preparado en la interfaz, pero todavía falta configurar un endpoint seguro."
        }
        messages = messages + ChatMessage(answer, false)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VisionApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisionApp(vm: VisionViewModel = viewModel()) {
    val bg = Color(0xFF0B1020)
    val panel = Color(0xFF151D31)
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Color(0xFF8B9CFF),
        background = bg,
        surface = panel
    )) {
        Scaffold(containerColor = bg) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column {
                    Text("VISION", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                    Text("Tu asistente personal · prototipo 0.1", color = Color.LightGray)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = vm.mode == EngineMode.LOCAL,
                        onClick = { vm.setMode(EngineMode.LOCAL) }, label = { Text("Local") })
                    FilterChip(selected = vm.mode == EngineMode.REMOTE,
                        onClick = { vm.setMode(EngineMode.REMOTE) }, label = { Text("Nube") })
                }
                Surface(color = panel, shape = RoundedCornerShape(14.dp)) {
                    Text(vm.status, Modifier.fillMaxWidth().padding(12.dp),
                        color = Color(0xFFB9C5E8), style = MaterialTheme.typography.bodySmall)
                }
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(vm.messages) { message ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                color = if (message.fromUser) Color(0xFF303F70) else panel,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.widthIn(max = 310.dp)
                            ) {
                                Text(message.text, Modifier.padding(12.dp), color = Color.White)
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = vm.draft,
                        onValueChange = { vm.draft = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Escribile a Vision…") },
                        maxLines = 3
                    )
                    Button(onClick = { vm.send() }) { Text("Enviar") }
                }
            }
        }
    }
}
