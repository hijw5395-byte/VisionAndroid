package com.vision.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class ChatMessage(val text: String, val fromUser: Boolean)
enum class EngineMode { LOCAL, REMOTE }

class VisionViewModel : ViewModel() {
    var messages by mutableStateOf(listOf(ChatMessage("Hola. Soy VISION. Configurá un motor remoto en Ajustes para conversar.", false)))
        private set
    var mode by mutableStateOf(EngineMode.REMOTE)
        private set
    var draft by mutableStateOf("")
    var endpoint by mutableStateOf("")
    var model by mutableStateOf("")
    var apiKey by mutableStateOf("")
    var status by mutableStateOf("Motor remoto · configuración pendiente")
    var busy by mutableStateOf(false)

    fun setMode(value: EngineMode) {
        mode = value
        status = if (value == EngineMode.LOCAL) "Modo local · modelo aún no instalado"
                 else "Modo remoto · " + if (endpoint.isBlank()) "configuración pendiente" else "endpoint configurado"
    }

    fun send() {
        val prompt = draft.trim()
        if (prompt.isEmpty() || busy) return
        messages = messages + ChatMessage(prompt, true)
        draft = ""
        if (mode == EngineMode.LOCAL) {
            messages = messages + ChatMessage("El modelo local todavía no está instalado. No voy a simular respuestas.", false)
            return
        }
        if (endpoint.isBlank() || model.isBlank()) {
            messages = messages + ChatMessage("Falta configurar la URL del endpoint y el nombre del modelo en Ajustes.", false)
            return
        }
        busy = true
        messages = messages + ChatMessage("Conectando con el motor…", false)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        connectTimeout = 20000
                        readTimeout = 60000
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                        if (apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer " + apiKey)
                    }
                    try {
                        val payload = JSONObject().put("model", model)
                            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
                            .put("stream", false)
                        connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                        val code = connection.responseCode
                        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                        if (code !in 200..299) "Error HTTP " + code + ": " + body
                        else JSONObject(body).optJSONArray("choices")?.optJSONObject(0)
                            ?.optJSONObject("message")?.optString("content")?.takeIf { it.isNotBlank() }
                            ?: "El servidor respondió, pero no devolvió choices[0].message.content."
                    } finally { connection.disconnect() }
                } catch (e: Exception) {
                    "No se pudo conectar: " + (e.message ?: "error desconocido") + ". Revisá URL, clave y conexión."
                }
            }
            messages = messages.dropLast(1) + ChatMessage(result, false)
            busy = false
            status = if (result.startsWith("No se pudo conectar") || result.startsWith("Error HTTP"))
                "Motor remoto · revisar conexión" else "Motor remoto · conectado"
        }
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
    var showSettings by remember { mutableStateOf(true) }
    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF8B9CFF), background = bg, surface = panel)) {
        Scaffold(containerColor = bg) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text("VISION", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                    Text("Tu asistente personal", color = Color.LightGray)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = vm.mode == EngineMode.REMOTE,
                        onClick = { vm.setMode(EngineMode.REMOTE) }, label = { Text("Nube") })
                    FilterChip(selected = vm.mode == EngineMode.LOCAL,
                        onClick = { vm.setMode(EngineMode.LOCAL) }, label = { Text("Local") })
                }
                Surface(color = panel, shape = RoundedCornerShape(14.dp)) {
                    Text(vm.status, Modifier.fillMaxWidth().padding(12.dp), color = Color(0xFFB9C5E8))
                }
                TextButton(onClick = { showSettings = !showSettings }) {
                    Text(if (showSettings) "Ocultar ajustes" else "⚙ Configurar motor")
                }
                if (showSettings) {
                    OutlinedTextField(vm.endpoint, { vm.endpoint = it }, Modifier.fillMaxWidth(),
                        label = { Text("URL API compatible con OpenAI") },
                        placeholder = { Text("https://servidor/v1/chat/completions") }, singleLine = true)
                    OutlinedTextField(vm.model, { vm.model = it }, Modifier.fillMaxWidth(),
                        label = { Text("Modelo") }, singleLine = true)
                    OutlinedTextField(vm.apiKey, { vm.apiKey = it }, Modifier.fillMaxWidth(),
                        label = { Text("Clave API (solo memoria)") }, singleLine = true)
                    Text("La clave permanece solo en memoria mientras la app está abierta.",
                        color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(vm.messages) { message ->
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start) {
                            Surface(color = if (message.fromUser) Color(0xFF303F70) else panel,
                                shape = RoundedCornerShape(16.dp), modifier = Modifier.widthIn(max = 310.dp)) {
                                Text(message.text, Modifier.padding(12.dp), color = Color.White)
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(vm.draft, { vm.draft = it }, Modifier.weight(1f),
                        placeholder = { Text("Escribile a VISION…") }, maxLines = 3)
                    Button(onClick = { vm.send() }, enabled = !vm.busy) { Text("Enviar") }
                }
            }
        }
    }
}
