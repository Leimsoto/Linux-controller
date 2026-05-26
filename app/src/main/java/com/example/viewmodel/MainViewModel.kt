package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.PCProfile
import com.example.data.model.QuickCommand
import com.example.data.repository.RemoteControlRepository
import com.example.service.SshManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(
    private val repository: RemoteControlRepository,
    private val context: android.content.Context
) : ViewModel() {

    // --- Gemini config options via SharedPreferences ---
    private val sharedPrefs = context.getSharedPreferences("gemini_prefs", android.content.Context.MODE_PRIVATE)

    private val _geminiApiKey = MutableStateFlow(sharedPrefs.getString("api_key", "") ?: "")
    val geminiApiKey = _geminiApiKey.asStateFlow()

    private val _geminiModel = MutableStateFlow(sharedPrefs.getString("model_name", "gemini-3.5-flash") ?: "gemini-3.5-flash")
    val geminiModel = _geminiModel.asStateFlow()

    private val _geminiSystemPrompt = MutableStateFlow(
        sharedPrefs.getString(
            "system_prompt",
            "Eres un asistente de terminal Linux experto para el sistema \"Linux Controller\". Tu trabajo es ayudar al usuario de manera didáctica, estructurada y muy práctica.\n\n" +
            "Para cada consulta del usuario, sigue siempre esta estructura en tu respuesta:\n" +
            "1. **PENSAMIENTO Y ANÁLISIS **: Explica paso a paso tu razonamiento, analizando qué necesita el usuario, cómo funciona el sistema internamente y qué precauciones o detalles debe tener en cuenta.\n" +
            "2. **CATÁLOGO DE COMANDOS SUGERIDOS**: Ofrece una gran variedad de opciones y variaciones útiles (ej. opción básica, comando rápido, versión avanzada, comando con filtros o tuberías, alternativas seguras, etc.) para que el usuario tenga suficiente abanico de comandos donde elegir.\n\n" +
            "REGLA CRÍTICA: Cada comando individual sugerido DEBE colocarse en un bloque de código markdown (por ejemplo: ```bash\nls -la\n```) o entre acentos graves simples (como `ls -la`) si es corto y directo. Coloca de forma separada los diferentes comandos para que el extractor de la app los reconozca de manera independiente y los convierta en accesos directos interactivos ejecutables con un solo toque. Responde siempre en español."
        ) ?: ""
    )
    val geminiSystemPrompt = _geminiSystemPrompt.asStateFlow()

    private val _geminiGroundingEnabled = MutableStateFlow(sharedPrefs.getBoolean("grounding_enabled", true))
    val geminiGroundingEnabled = _geminiGroundingEnabled.asStateFlow()

    fun updateGeminiSettings(apiKey: String, model: String, systemPrompt: String, grounding: Boolean) {
        _geminiApiKey.value = apiKey
        _geminiModel.value = model
        _geminiSystemPrompt.value = systemPrompt
        _geminiGroundingEnabled.value = grounding
        sharedPrefs.edit()
            .putString("api_key", apiKey)
            .putString("model_name", model)
            .putString("system_prompt", systemPrompt)
            .putBoolean("grounding_enabled", grounding)
            .apply()
    }

    fun getEffectiveApiKey(): String {
        val configured = _geminiApiKey.value
        if (configured.isNotBlank()) return configured
        return try {
            com.example.BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
    }

    // --- Gemini Chat States & Flows ---
    data class ChatMessage(
        val role: String, // "user" or "model"
        val text: String,
        val timestamp: String,
        val commands: List<String> = emptyList()
    )

    private val _aiChatHistory = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                role = "model",
                text = "¡Hola! Soy tu Copiloto de Linux con Inteligencia Artificial. Estoy aquí para sugerir comandos, ayudarte a resolver problemas o realizar tareas en tu terminal. ¿Qué deseas hacer hoy?",
                timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            )
        )
    )
    val aiChatHistory = _aiChatHistory.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading = _isAiLoading.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError = _aiError.asStateFlow()

    fun clearAiChat() {
        val now = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _aiChatHistory.value = listOf(
            ChatMessage(
                role = "model",
                text = "Chat reiniciado. ¿En qué te puedo asistir ahora?",
                timestamp = now
            )
        )
        _aiError.value = null
    }

    fun sendAiMessage(prompt: String) {
        val key = getEffectiveApiKey()
        if (key.isBlank() || key == "MY_GEMINI_API_KEY") {
            _aiError.value = "Por favor, ingresa una Gemini API Key en el panel superior de Configuración para poder interactuar con la IA."
            return
        }

        val now = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val userMsg = ChatMessage(role = "user", text = prompt, timestamp = now)
        val expandedHistory = _aiChatHistory.value + userMsg
        _aiChatHistory.value = expandedHistory
        _isAiLoading.value = true
        _aiError.value = null

        viewModelScope.launch {
            try {
                val conversationHistory = if (expandedHistory.size > 12) {
                    expandedHistory.takeLast(12)
                } else {
                    expandedHistory
                }

                val selectedModel = if (_geminiModel.value.isNotBlank()) _geminiModel.value else "gemini-3.5-flash"
                val isGemma = selectedModel.contains("gemma")

                val apiContents = conversationHistory.mapIndexed { index, msg ->
                    val textContent = if (isGemma && index == conversationHistory.lastIndex && _geminiSystemPrompt.value.isNotBlank()) {
                        "${_geminiSystemPrompt.value}\n\n[Consulta del usuario]:\n${msg.text}"
                    } else {
                        msg.text
                    }
                    com.example.service.GeminiContent(
                        parts = listOf(com.example.service.GeminiPart(text = textContent))
                    )
                }

                val systemInstruction = if (!isGemma && _geminiSystemPrompt.value.isNotBlank()) {
                    com.example.service.GeminiContent(
                        parts = listOf(com.example.service.GeminiPart(text = _geminiSystemPrompt.value))
                    )
                } else null

                val tools = if (!isGemma && _geminiGroundingEnabled.value) {
                    listOf(mapOf("googleSearch" to emptyMap<String, Any>()))
                } else null

                val requestBody = com.example.service.GeminiRequest(
                    contents = apiContents,
                    systemInstruction = systemInstruction,
                    tools = tools,
                    generationConfig = com.example.service.GeminiGenerationConfig(temperature = 0.5f)
                )

                val response = com.example.service.GeminiClient.api.generateContent(
                    model = selectedModel,
                    apiKey = key,
                    request = requestBody
                )

                val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                    ?: "No se obtuvo respuesta del modelo. Verifica tu conexión de red o API Key."

                val listCommands = parseCommands(replyText)
                val aiReply = ChatMessage(
                    role = "model",
                    text = replyText,
                    timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                    commands = listCommands
                )
                _aiChatHistory.value = _aiChatHistory.value + aiReply
            } catch (e: Exception) {
                e.printStackTrace()
                _aiError.value = "Error al conectar con Gemini: ${e.localizedMessage}"
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    private fun parseCommands(text: String): List<String> {
        val result = mutableListOf<String>()
        val triplePattern = "```(?:bash|sh|shell)?\\n?([\\s\\S]*?)```".toRegex()
        val tripleMatches = triplePattern.findAll(text)
        for (m in tripleMatches) {
            val cmd = m.groupValues[1].trim()
            if (cmd.isNotBlank()) {
                result.add(cmd)
            }
        }
        val singlePattern = "`([^`\\n]+)`".toRegex()
        val singleMatches = singlePattern.findAll(text)
        for (m in singleMatches) {
            val cmd = m.groupValues[1].trim()
            if (cmd.isNotBlank() && !result.contains(cmd) && cmd.length > 2) {
                result.add(cmd)
            }
        }
        return result
    }

    // Profiles & Commands flows from Room database
    val allProfiles: StateFlow<List<PCProfile>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProfile: StateFlow<PCProfile?> = repository.defaultProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allCommands: StateFlow<List<QuickCommand>> = repository.allCommands
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Execution & connection operations
    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val _connectionTestResult = MutableStateFlow<String?>(null)
    val connectionTestResult: StateFlow<String?> = _connectionTestResult.asStateFlow()

    // Console logs / Interactive Terminal
    data class TerminalLog(
        val command: String,
        val output: String,
        val error: String,
        val timestamp: String,
        val success: Boolean
    )

    private val _terminalLogs = MutableStateFlow<List<TerminalLog>>(emptyList())
    val terminalLogs: StateFlow<List<TerminalLog>> = _terminalLogs.asStateFlow()

    private val _lastResult = MutableStateFlow<TerminalLog?>(null)
    val lastResult: StateFlow<TerminalLog?> = _lastResult.asStateFlow()

    private val _telemetryStats = MutableStateFlow<SshManager.TelemetryStats?>(null)
    val telemetryStats: StateFlow<SshManager.TelemetryStats?> = _telemetryStats.asStateFlow()

    private val _isTelemetryLoading = MutableStateFlow(false)
    val isTelemetryLoading: StateFlow<Boolean> = _isTelemetryLoading.asStateFlow()

    private var telemetryJob: kotlinx.coroutines.Job? = null

    fun fetchTelemetry(profile: PCProfile) {
        telemetryJob?.cancel()
        telemetryJob = viewModelScope.launch {
            _isTelemetryLoading.value = true
            try {
                val stats = SshManager.getRemoteTelemetry(profile)
                _telemetryStats.value = stats
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                _isTelemetryLoading.value = false
            }
        }
    }

    init {
        // Ensure default mock commands or predefines are set on DB start
        viewModelScope.launch {
            repository.ensureDefaultCommands()
        }
        viewModelScope.launch {
            activeProfile.collect { profile ->
                if (profile != null) {
                    fetchTelemetry(profile)
                } else {
                    _telemetryStats.value = null
                }
            }
        }
    }

    // Database Actions
    fun saveProfile(profile: PCProfile) {
        viewModelScope.launch {
            if (profile.id == 0) {
                val newId = repository.insertProfile(profile)
                // If this is the only profile, automatically make it default
                if (allProfiles.value.isEmpty()) {
                    repository.setProfileDefault(profile.copy(id = newId.toInt()))
                }
            } else {
                repository.updateProfile(profile)
            }
        }
    }

    fun deleteProfile(profile: PCProfile) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
            // If we deleted the active profile, reset default
            if (profile.isDefault && allProfiles.value.isNotEmpty()) {
                val nextAvailable = allProfiles.value.firstOrNull { it.id != profile.id }
                if (nextAvailable != null) {
                    repository.setProfileDefault(nextAvailable)
                }
            }
        }
    }

    fun selectActiveProfile(profile: PCProfile) {
        viewModelScope.launch {
            repository.setProfileDefault(profile)
            _connectionTestResult.value = null // Clear old test results
        }
    }

    fun addQuickCommand(name: String, code: String, category: String, icon: String) {
        viewModelScope.launch {
            val cmd = QuickCommand(
                name = name,
                commandCode = code,
                category = category,
                iconName = icon
            )
            repository.insertCommand(cmd)
        }
    }

    fun deleteQuickCommand(command: QuickCommand) {
        viewModelScope.launch {
            repository.deleteCommand(command)
        }
    }

    // Remote Actions
    fun testConnection(profile: PCProfile) {
        viewModelScope.launch {
            _isConnecting.value = true
            _connectionTestResult.value = null
            
            val result = SshManager.testConnection(profile)
            
            _isConnecting.value = false
            if (result.success) {
                _connectionTestResult.value = "Conexión exitosa a ${profile.host}: ${result.output}"
            } else {
                _connectionTestResult.value = "Error de conexión: ${result.error}"
            }
        }
    }

    fun clearConnectionTestResult() {
        _connectionTestResult.value = null
    }

    fun executeRemoteCommand(commandName: String, commandCode: String) {
        val target = activeProfile.value
        if (target == null) {
            addTerminalLog(
                command = commandName,
                output = "",
                error = "Error: No se ha configurado o seleccionado ningún perfil de PC de Linux activo.",
                success = false
            )
            return
        }

        viewModelScope.launch {
            _isExecuting.value = true
            
            addTerminalLog(
                command = "sh -c \"$commandCode\"",
                output = "Ejecutando en ${target.name} (${target.host})...",
                error = "",
                success = true
            )

            val result = SshManager.executeCommand(target, commandCode)
            
            _isExecuting.value = false
            
            addTerminalLog(
                command = commandName,
                output = result.output,
                error = result.error,
                success = result.success
            )
        }
    }

    fun clearTerminalLogs() {
        _terminalLogs.value = emptyList()
        _lastResult.value = null
    }

    private fun addTerminalLog(command: String, output: String, error: String, success: Boolean) {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val now = formatter.format(Date())
        
        val log = TerminalLog(
            command = command,
            output = output,
            error = error,
            timestamp = now,
            success = success
        )
        
        _terminalLogs.value = listOf(log) + _terminalLogs.value
        _lastResult.value = log
    }

    // --- Storage States ---
    private val _remotePath = MutableStateFlow(".")
    val remotePath: StateFlow<String> = _remotePath.asStateFlow()

    private val _remoteFiles = MutableStateFlow<List<SshManager.RemoteFile>>(emptyList())
    val remoteFiles: StateFlow<List<SshManager.RemoteFile>> = _remoteFiles.asStateFlow()

    private val _isBrowsing = MutableStateFlow(false)
    val isBrowsing: StateFlow<Boolean> = _isBrowsing.asStateFlow()

    private val _remoteDiskInfo = MutableStateFlow<SshManager.RemoteDiskInfo?>(null)
    val remoteDiskInfo: StateFlow<SshManager.RemoteDiskInfo?> = _remoteDiskInfo.asStateFlow()

    private val _localFiles = MutableStateFlow<List<java.io.File>>(emptyList())
    val localFiles: StateFlow<List<java.io.File>> = _localFiles.asStateFlow()

    private val _storageStatus = MutableStateFlow<String?>(null)
    val storageStatus: StateFlow<String?> = _storageStatus.asStateFlow()

    fun setRemotePath(path: String) {
        _remotePath.value = path
    }

    fun loadRemoteFilesAndDisk() {
        val target = activeProfile.value
        if (target == null) {
            _storageStatus.value = "Selecciona un perfil activo primero."
            return
        }
        viewModelScope.launch {
            _isBrowsing.value = true
            _storageStatus.value = "Listando archivos de ${target.name}..."
            try {
                val files = SshManager.listRemoteDirectory(target, _remotePath.value)
                _remoteFiles.value = files
                _storageStatus.value = "Directorio listado con éxito."

                val disk = SshManager.getRemoteDiskUsage(target)
                _remoteDiskInfo.value = disk
            } catch (e: Exception) {
                _storageStatus.value = "Error al listar: ${e.localizedMessage}"
            } finally {
                _isBrowsing.value = false
            }
        }
    }

    fun loadLocalFiles(localDir: java.io.File) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                if (!localDir.exists()) {
                    localDir.mkdirs()
                }
                val files = localDir.listFiles()?.toList() ?: emptyList()
                _localFiles.value = files.sortedByDescending { it.lastModified() }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun downloadRemoteFile(remoteFile: SshManager.RemoteFile, localDir: java.io.File) {
        val target = activeProfile.value
        if (target == null) {
            _storageStatus.value = "Selecciona un perfil activo primero."
            return
        }
        viewModelScope.launch {
            _isBrowsing.value = true
            _storageStatus.value = "Descargando ${remoteFile.name}..."
            try {
                val success = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    if (!localDir.exists()) {
                        localDir.mkdirs()
                    }
                    val localFile = java.io.File(localDir, remoteFile.name)
                    val outStream = java.io.FileOutputStream(localFile)
                    SshManager.downloadFile(target, remoteFile.path, outStream)
                }
                if (success) {
                    _storageStatus.value = "Descargado con éxito: ${remoteFile.name}"
                    loadLocalFiles(localDir)
                } else {
                    _storageStatus.value = "Falló la descarga de ${remoteFile.name}"
                }
            } catch (e: Throwable) {
                _storageStatus.value = "Error en descarga: ${e.localizedMessage}"
            } finally {
                _isBrowsing.value = false
            }
        }
    }

    fun uploadLocalFileToRemote(
        contentResolver: android.content.ContentResolver,
        uri: android.net.Uri,
        fileName: String,
        localDirForRefresh: java.io.File
    ) {
        val target = activeProfile.value
        if (target == null) {
            _storageStatus.value = "Selecciona un perfil activo primero."
            return
        }
        viewModelScope.launch {
            _isBrowsing.value = true
            _storageStatus.value = "Subiendo archivo a PC: $fileName..."
            try {
                val success = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val inputStream = contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val remoteDest = if (_remotePath.value.endsWith("/")) "${_remotePath.value}$fileName" else "${_remotePath.value}/$fileName"
                        SshManager.uploadFile(target, inputStream, remoteDest)
                    } else {
                        false
                    }
                }
                if (success) {
                    _storageStatus.value = "Subido con éxito: $fileName"
                    loadRemoteFilesAndDisk()
                } else {
                    _storageStatus.value = "Falló la subida de $fileName"
                }
            } catch (e: Throwable) {
                _storageStatus.value = "Error en subida: ${e.localizedMessage}"
            } finally {
                _isBrowsing.value = false
            }
        }
    }

    fun deleteLocalFile(file: java.io.File, localDirForRefresh: java.io.File) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                if (file.exists() && file.delete()) {
                    _storageStatus.value = "Archivo local eliminado: ${file.name}"
                    loadLocalFiles(localDirForRefresh)
                } else {
                    _storageStatus.value = "No se pudo eliminar el archivo"
                }
            } catch (e: Throwable) {
                _storageStatus.value = "Error: ${e.localizedMessage}"
            }
        }
    }

    fun clearStorageStatus() {
        _storageStatus.value = null
    }
}

class MainViewModelFactory(
    private val repository: RemoteControlRepository,
    private val context: android.content.Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, context.applicationContext) as T
        }
        throw java.lang.IllegalArgumentException("Unknown ViewModel class class: ${modelClass.name}")
    }
}
