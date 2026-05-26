package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.AppDatabase
import com.example.data.model.PCProfile
import com.example.data.model.QuickCommand
import com.example.data.repository.RemoteControlRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.MainViewModelFactory
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.animateContentSize
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = RemoteControlRepository(database.remoteControlDao())
        viewModel = ViewModelProvider(this, MainViewModelFactory(repository, applicationContext))[MainViewModel::class.java]
        
        setContent {
            MyApplicationTheme {
                MainScreen(viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val profiles by viewModel.allProfiles.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val quickCommands by viewModel.allCommands.collectAsStateWithLifecycle()
    val isConnecting by viewModel.isConnecting.collectAsStateWithLifecycle()
    val isExecuting by viewModel.isExecuting.collectAsStateWithLifecycle()
    val connectionResult by viewModel.connectionTestResult.collectAsStateWithLifecycle()
    val terminalLogs by viewModel.terminalLogs.collectAsStateWithLifecycle()
    val lastResult by viewModel.lastResult.collectAsStateWithLifecycle()
    val telemetryStats by viewModel.telemetryStats.collectAsStateWithLifecycle()
    val isTelemetryLoading by viewModel.isTelemetryLoading.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showProfileEditor by remember { mutableStateOf(false) }
    var showCommandEditor by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<PCProfile?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopConnectionHeader(activeProfile, isExecuting)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF3F4F9)) // Sleek interface background
        ) {
            // Central Tab Navigation Vibe
            androidx.compose.material3.ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color(0xFFEEEFF4),
                contentColor = Color(0xFF1B1B1F),
                edgePadding = 12.dp,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Color(0xFF001453)
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Conexiones", fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp) },
                    selectedContentColor = Color(0xFF001453),
                    unselectedContentColor = Color(0xFF44474F),
                    modifier = Modifier.testTag("tab_connections")
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Atajos", fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp) },
                    selectedContentColor = Color(0xFF001453),
                    unselectedContentColor = Color(0xFF44474F),
                    modifier = Modifier.testTag("tab_shortcuts")
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Terminal", fontWeight = if (selectedTabIndex == 2) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp) },
                    selectedContentColor = Color(0xFF001453),
                    unselectedContentColor = Color(0xFF44474F),
                    modifier = Modifier.testTag("tab_terminal")
                )
                Tab(
                    selected = selectedTabIndex == 3,
                    onClick = { selectedTabIndex = 3 },
                    text = { Text("Archivos", fontWeight = if (selectedTabIndex == 3) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp) },
                    selectedContentColor = Color(0xFF001453),
                    unselectedContentColor = Color(0xFF44474F),
                    modifier = Modifier.testTag("tab_storage")
                )
                Tab(
                    selected = selectedTabIndex == 4,
                    onClick = { selectedTabIndex = 4 },
                    text = { Text("Copiloto IA", fontWeight = if (selectedTabIndex == 4) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp) },
                    selectedContentColor = Color(0xFF001453),
                    unselectedContentColor = Color(0xFF44474F),
                    modifier = Modifier.testTag("tab_copiloto")
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTabIndex) {
                    0 -> ConnectionTabContent(
                        profiles = profiles,
                        activeProfile = activeProfile,
                        isConnecting = isConnecting,
                        connectionResult = connectionResult,
                        telemetryStats = telemetryStats,
                        isTelemetryLoading = isTelemetryLoading,
                        onRefreshTelemetry = { viewModel.fetchTelemetry(it) },
                        onProfileSelect = { viewModel.selectActiveProfile(it) },
                        onProfileEdit = {
                            editingProfile = it
                            showProfileEditor = true
                        },
                        onProfileDelete = { viewModel.deleteProfile(it) },
                        onProfileAddClick = {
                            editingProfile = null
                            showProfileEditor = true
                        },
                        onTestConnection = { viewModel.testConnection(it) },
                        onClearTestResult = { viewModel.clearConnectionTestResult() }
                    )
                    1 -> ShortcutsTabContent(
                        commands = quickCommands,
                        activeProfile = activeProfile,
                        isExecuting = isExecuting,
                        onCommandClick = { viewModel.executeRemoteCommand(it.name, it.commandCode) },
                        onCommandDelete = { viewModel.deleteQuickCommand(it) },
                        onAddCommandClick = { showCommandEditor = true }
                    )
                    2 -> InteractiveTerminalTab(
                        terminalLogs = terminalLogs,
                        activeProfile = activeProfile,
                        isExecuting = isExecuting,
                        onExecuteCommand = { viewModel.executeRemoteCommand("Consola", it) },
                        onClearLogs = { viewModel.clearTerminalLogs() }
                    )
                    3 -> StorageTabContent(
                        viewModel = viewModel
                    )
                    4 -> CopilotoIaTabContent(
                        viewModel = viewModel
                    )
                }
            }

            // HUD Footer alerting latest command execution result
            lastResult?.let { log ->
                ExecutionStatusHUD(log)
            }
        }
    }

    // Modal dialogs for adding/editing profile or commands
    if (showProfileEditor) {
        ProfileEditorDialog(
            profile = editingProfile,
            onDismiss = { showProfileEditor = false },
            onSave = {
                viewModel.saveProfile(it)
                showProfileEditor = false
            }
        )
    }

    if (showCommandEditor) {
        CommandEditorDialog(
            onDismiss = { showCommandEditor = false },
            onSave = { name, code, category, icon ->
                viewModel.addQuickCommand(name, code, category, icon)
                showCommandEditor = false
            }
        )
    }
}

// Ensure UI scrolls terminal to show latest entries immediately
@Composable
fun SchedzeTerminalScroll(logs: List<MainViewModel.TerminalLog>) {
    // Left empty for logical hook or reactive listener in InteractiveTerminalTab
}

// Cool Dynamic Header showing active status
@Composable
fun TopConnectionHeader(activeProfile: PCProfile?, isExecuting: Boolean) {
    Surface(
        color = Color(0xFFF3F4F9),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 42.dp, bottom = 14.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LINUX CONTROLLER",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1B1B1F),
                        letterSpacing = 1.5.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (activeProfile != null) Color(0xFF4CAF50) else Color(0xFFFF9800))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = activeProfile?.let { "${it.name} (${it.host})" } ?: "Ningún PC asignado",
                            fontSize = 12.sp,
                            color = if (activeProfile != null) Color(0xFF44474F) else Color(0xFFE65100),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (isExecuting) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .border(1.dp, Color(0xFF001453).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = Color(0xFF001453)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SSH EXEC",
                        fontSize = 10.sp,
                        color = Color(0xFF001453),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            } else if (activeProfile != null) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFD9E7CB), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "SSH ONLINE",
                        fontSize = 10.sp,
                        color = Color(0xFF141E0D),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

// 1. CONNECTIONS TAB CONTENT
@Composable
fun ConnectionTabContent(
    profiles: List<PCProfile>,
    activeProfile: PCProfile?,
    isConnecting: Boolean,
    connectionResult: String?,
    telemetryStats: com.example.service.SshManager.TelemetryStats?,
    isTelemetryLoading: Boolean,
    onRefreshTelemetry: (PCProfile) -> Unit,
    onProfileSelect: (PCProfile) -> Unit,
    onProfileEdit: (PCProfile) -> Unit,
    onProfileDelete: (PCProfile) -> Unit,
    onProfileAddClick: () -> Unit,
    onTestConnection: (PCProfile) -> Unit,
    onClearTestResult: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Perfiles de PC",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B1B1F)
            )

            Button(
                onClick = onProfileAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDDE1FF)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.testTag("add_profile_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar Perfil",
                    tint = Color(0xFF001453)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nuevo PC", color = Color(0xFF001453), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Connection test alert overlay
        connectionResult?.let { result ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE1E2EC)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .border(1.dp, Color(0xFFC7C6D0), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (result.contains("exitosa")) Icons.Default.Info else Icons.Default.Warning,
                            contentDescription = "Test Connection",
                            tint = if (result.contains("exitosa")) Color(0xFF1B5E20) else Color(0xFFC62828),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = result,
                            color = Color(0xFF1B1B1F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(
                        onClick = onClearTestResult,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF44474F),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (profiles.isEmpty()) {
            EmptyStatePlaceholder(
                title = "Sin servidores o computadoras",
                tip = "Los perfiles permiten almacenar la IP, puerto y credenciales de acceso SSH de tus computadoras Linux para controlarlas de forma remota y rápida.",
                actionLabel = "Registrar primera PC",
                onAction = onProfileAddClick
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(profiles) { profile ->
                    val isActive = activeProfile?.id == profile.id
                    PCProfileCard(
                        profile = profile,
                        isActive = isActive,
                        isConnecting = isConnecting,
                        telemetryStats = if (isActive) telemetryStats else null,
                        isTelemetryLoading = if (isActive) isTelemetryLoading else false,
                        onRefreshTelemetry = { onRefreshTelemetry(profile) },
                        onSelect = { onProfileSelect(profile) },
                        onEdit = { onProfileEdit(profile) },
                        onDelete = { onProfileDelete(profile) },
                        onTest = { onTestConnection(profile) }
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PCProfileCard(
    profile: PCProfile,
    isActive: Boolean,
    isConnecting: Boolean,
    telemetryStats: com.example.service.SshManager.TelemetryStats?,
    isTelemetryLoading: Boolean,
    onRefreshTelemetry: () -> Unit,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFFE1E2EC) else Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.2.dp,
                color = if (isActive) Color(0xFF001453) else Color(0xFFC7C6D0),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    RadioButton(
                        selected = isActive,
                        onClick = onSelect,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFF001453),
                            unselectedColor = Color(0xFFC7C6D0)
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = profile.name,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B1B1F),
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${profile.username}@${profile.host}:${profile.port}",
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF44474F),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = Color(0xFF44474F),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Borrar",
                            tint = Color(0xFF601410),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (profile.authType == "PASSWORD") Color(0xFFE0E2EC) else Color(0xFFDDE1FF),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (profile.authType == "PASSWORD") "Contraseña" else "Clave SSH",
                            fontSize = 11.sp,
                            color = if (profile.authType == "PASSWORD") Color(0xFF1B1B1F) else Color(0xFF001453),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (isActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFD9E7CB), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "ACTIVO",
                                fontSize = 11.sp,
                                color = Color(0xFF141E0D),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onTest,
                    border = BorderStroke(1.dp, Color(0xFF001453)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF001453)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    if (isConnecting && isActive) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color = Color(0xFF001453)
                        )
                    } else {
                        Text("Probar SSH", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isActive) {
                Spacer(modifier = Modifier.height(14.dp))
                androidx.compose.material3.HorizontalDivider(color = Color(0xFFC7C6D0).copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Dashboard",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF001453)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (isTelemetryLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.2.dp,
                                color = Color(0xFF001453)
                            )
                        }
                    }

                    IconButton(
                        onClick = onRefreshTelemetry,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refrescar Telemetría",
                            tint = Color(0xFF001453),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (telemetryStats != null) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // CPU Mini Card
                        TelemetryMiniCard(
                            title = "CPU",
                            value = telemetryStats.cpuUsage,
                            icon = "🖥️",
                            progress = try { telemetryStats.cpuUsage.replace("%", "").trim().toFloat() / 100f } catch (e: Exception) { 0.12f },
                            modifier = Modifier.width(106.dp)
                        )
                        // RAM Mini Card
                        TelemetryMiniCard(
                            title = "RAM",
                            value = "${telemetryStats.ramUsed}/${telemetryStats.ramTotal}",
                            icon = "💾",
                            progress = try { telemetryStats.ramPercent.replace("%", "").trim().toFloat() / 100f } catch (e: Exception) { 0.40f },
                            modifier = Modifier.width(106.dp)
                        )
                        // Storage Mini Card
                        TelemetryMiniCard(
                            title = "DISCO",
                            value = "${telemetryStats.diskUsed}/${telemetryStats.diskTotal}",
                            icon = "📁",
                            progress = try { telemetryStats.diskPercent.replace("%", "").trim().toFloat() / 100f } catch (e: Exception) { 0.38f },
                            modifier = Modifier.width(106.dp)
                        )
                        // Network Mini Card
                        TelemetryMiniCard(
                            title = "RED",
                            value = telemetryStats.networkSpeed,
                            icon = "🌐",
                            progress = null,
                            modifier = Modifier.width(106.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Conectando para actualizar telemetría...",
                        fontSize = 11.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = Color(0xFF44474F)
                    )
                }
            }
        }
    }
}

@Composable
fun TelemetryMiniCard(
    title: String,
    value: String,
    icon: String,
    progress: Float?,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier.border(1.dp, Color(0xFFC7C6D0).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = title, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, color = Color(0xFF44474F))
                Text(text = icon, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color(0xFF1B1B1F),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            if (progress != null) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    color = when {
                        progress > 0.85f -> Color(0xFFC62828)
                        progress > 0.65f -> Color(0xFFF9A825)
                        else -> Color(0xFF001453)
                    },
                    trackColor = Color(0xFFE1E2EC),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
fun EmptyStatePlaceholder(
    title: String,
    tip: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = Color(0xFFC7C6D0),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B1B1F),
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = tip,
            color = Color(0xFF44474F),
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 18.sp
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDDE1FF)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(text = actionLabel, color = Color(0xFF001453), fontWeight = FontWeight.Bold)
            }
        }
    }
}


// 2. SHORTCUTS TAB CONTENT
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShortcutsTabContent(
    commands: List<QuickCommand>,
    activeProfile: PCProfile?,
    isExecuting: Boolean,
    onCommandClick: (QuickCommand) -> Unit,
    onCommandDelete: (QuickCommand) -> Unit,
    onAddCommandClick: () -> Unit
) {
    var isDeleteMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Comandos Rápidos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B1B1F)
            )

            IconButton(
                onClick = { isDeleteMode = !isDeleteMode },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isDeleteMode) Icons.Default.Close else Icons.Default.Delete,
                    contentDescription = "Borrar atajos",
                    tint = if (isDeleteMode) Color(0xFF601410) else Color(0xFF44474F)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onAddCommandClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDDE1FF)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Agregar comando",
                tint = Color(0xFF001453)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Agregar Comando", color = Color(0xFF001453), fontWeight = FontWeight.Bold)
        }

        if (activeProfile == null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF2B8B5)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .border(1.dp, Color(0xFF601410).copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFF601410),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Debes seleccionar un perfil activo de PC (en la pestaña de Conexiones) antes de que puedas disparar comandos rápidos mediante SSH.",
                        color = Color(0xFF601410),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Group commands by category beautifully
        val categories = commands.groupBy { it.category }

        if (commands.isEmpty()) {
            EmptyStatePlaceholder(
                title = "Sin botones de comando",
                tip = "Los comandos rápidos te permiten automatizar tareas redundantes en bash: por ejemplo, silenciar audio, pausar videos, suspender la PC o consultar espacio en disco."
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                categories.forEach { (category, cmds) ->
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = category.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF001453),
                                letterSpacing = 1.5.sp,
                                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                cmds.forEach { cmd ->
                                    CommandGridItem(
                                        command = cmd,
                                        isExecuting = isExecuting,
                                        isDeleteMode = isDeleteMode,
                                        onTrigger = { onCommandClick(cmd) },
                                        onDelete = { onCommandDelete(cmd) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommandGridItem(
    command: QuickCommand,
    isExecuting: Boolean,
    isDeleteMode: Boolean,
    onTrigger: () -> Unit,
    onDelete: () -> Unit
) {
    // Elegant quick action backgrounds based on categories / defaults from design instructions:
    // e.g., Reboot (Power) gets #DDE1FF / #001453 text, Lock gets #E0E2EC, Stop gets #F2B8B5. Let's make it colourful!
    val (bg, iconColor) = when (command.category.lowercase()) {
        "system" -> Color(0xFFDDE1FF) to Color(0xFF001453)
        "media" -> Color(0xFFD9E7CB) to Color(0xFF141E0D)
        "audio" -> Color(0xFFE0E2EC) to Color(0xFF1B1B1F)
        else -> Color.White to Color(0xFF001453)
    }

    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 108.dp)
            .background(if (isDeleteMode) Color(0xFFF2B8B5) else bg, RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (isDeleteMode) Color(0xFF601410).copy(alpha = 0.5f) else Color(0xFFC7C6D0),
                RoundedCornerShape(16.dp)
            )
            .clickable(enabled = !isDeleteMode, onClick = onTrigger)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = getIconForName(command.iconName),
                contentDescription = null,
                tint = if (isDeleteMode) Color(0xFF601410) else iconColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = command.name,
                fontWeight = FontWeight.SemiBold,
                color = if (isDeleteMode) Color(0xFF601410) else Color(0xFF1B1B1F),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            if (isDeleteMode) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Borrar",
                        tint = Color(0xFF601410),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}


// Helper function mapping strings from Database to safe vector icons
fun getIconForName(name: String): ImageVector {
    return when (name) {
        "volume_off" -> Icons.Default.Notifications
        "volume_up" -> Icons.Default.KeyboardArrowUp
        "volume_down" -> Icons.Default.KeyboardArrowDown
        "play_arrow" -> Icons.Default.PlayArrow
        "lock" -> Icons.Default.Lock
        "nights_stay" -> Icons.Default.Star
        "power_settings_new" -> Icons.Default.Close
        "info" -> Icons.Default.Info
        "thermostat" -> Icons.Default.Warning
        "memory" -> Icons.Default.Build
        "storage" -> Icons.Default.List
        "schedule" -> Icons.Default.Refresh
        else -> Icons.Default.Settings
    }
}


// 3. INTERACTIVE TERMINAL TAB
@Composable
fun InteractiveTerminalTab(
    terminalLogs: List<MainViewModel.TerminalLog>,
    activeProfile: PCProfile?,
    isExecuting: Boolean,
    onExecuteCommand: (String) -> Unit,
    onClearLogs: () -> Unit
) {
    var rawInputState by remember { mutableStateOf("") }
    val consoleListState = rememberLazyListState()

    // Whenever logs output increases, auto-scroll terminal down
    LaunchedEffect(terminalLogs.size) {
        if (terminalLogs.isNotEmpty()) {
            try {
                consoleListState.animateScrollToItem(0)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Consola de Comandos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B1B1F)
            )

            IconButton(
                onClick = onClearLogs,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Limpiar Consola",
                    tint = Color(0xFF44474F)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Monitor connection warning
        if (activeProfile == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF2B8B5), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF601410).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Consola desconectada. Selecciona un perfil activo para activar SSH.",
                    color = Color(0xFF601410),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Real-looking scrolling black terminal canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1B1B1F))
                .border(1.2.dp, Color(0xFFC7C6D0), RoundedCornerShape(24.dp))
                .padding(14.dp)
        ) {
            if (terminalLogs.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TERMINAL CLIENT INICIALIZADO",
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFE1E2EC).copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "esperando entrada remota...",
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFC7C6D0),
                        fontSize = 11.sp
                    )
                }
            } else {
                LazyColumn(
                    state = consoleListState,
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(terminalLogs) { log ->
                        TerminalLogLine(log)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Linux interactive CLI Input field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = rawInputState,
                onValueChange = { rawInputState = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("console_input_field"),
                placeholder = {
                    Text(
                        "Escribe un comando... (p.ej: ls, free, htop)",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF74777F)
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF1B1B1F)
                ),
                leadingIcon = {
                    val promptText = activeProfile?.username ?: "user"
                    Text(
                        text = "$promptText$",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF001453),
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                    )
                },
                trailingIcon = {
                    if (rawInputState.isNotEmpty()) {
                        IconButton(onClick = { rawInputState = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Limpiar input",
                                tint = Color(0xFF44474F),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF001453),
                    unfocusedBorderColor = Color(0xFFC7C6D0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            IconButton(
                onClick = {
                    if (rawInputState.isNotBlank()) {
                        onExecuteCommand(rawInputState)
                        rawInputState = ""
                    }
                },
                enabled = !isExecuting && activeProfile != null && rawInputState.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (rawInputState.isNotBlank() && activeProfile != null) Color(0xFF001453) else Color(
                            0xFFE1E2EC
                        )
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Enviar",
                    tint = if (rawInputState.isNotBlank() && activeProfile != null) Color.White else Color(0xFF74777F),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun TerminalLogLine(log: MainViewModel.TerminalLog) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Line Title: Timestamp & Prompt command input
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "[${log.timestamp}]",
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF91909A),
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$ ${log.command}",
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFE1E2EC),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Stdout display
        if (log.output.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = log.output,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFC7C6D0),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        // Stderr display
        if (log.error.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = log.error,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFE53935),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}


// Bottom overlay HUD presenting absolute output results
@Composable
fun ExecutionStatusHUD(log: MainViewModel.TerminalLog) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val lineColor = if (log.success) Color(0xFF43A047).copy(alpha = 0.3f) else Color(0xFFE53935).copy(alpha = 0.3f)
        Surface(
            color = if (log.success) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(lineColor)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (log.success) Color(0xFF2E7D32) else Color(0xFFC62828))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (log.success) "Comando Ejecutado" else "Comando Fallido",
                        color = if (log.success) Color(0xFF2E7D32) else Color(0xFFC62828),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF74777F))) {
                                append("Run: ")
                            }
                            append(if (log.command.length > 50) log.command.take(47) + "..." else log.command)
                        },
                        color = Color(0xFF44474F),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
          }
        }
    }
}


// PROFILE ADD / EDIT EDITOR MODAL
@Composable
fun ProfileEditorDialog(
    profile: PCProfile?,
    onDismiss: () -> Unit,
    onSave: (PCProfile) -> Unit
) {
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var host by remember { mutableStateOf(profile?.host ?: "") }
    var port by remember { mutableStateOf(profile?.port?.toString() ?: "22") }
    var username by remember { mutableStateOf(profile?.username ?: "root") }
    var authType by remember { mutableStateOf(profile?.authType ?: "PASSWORD") }
    var password by remember { mutableStateOf(profile?.password ?: "") }
    var privateKey by remember { mutableStateOf(profile?.privateKey ?: "") }
    var sudoPassword by remember { mutableStateOf(profile?.sudoPassword ?: "") }
    val isDefault = profile?.isDefault ?: false

    var isPasswordVisible by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.2.dp, Color(0xFFC7C6D0), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (profile == null) "Agregar Perfil PC" else "Editar Perfil PC",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1B1F)
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Perfil (e.g., Mi Desktop)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_edit"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF001453),
                        unfocusedBorderColor = Color(0xFFC7C6D0),
                        focusedLabelColor = Color(0xFF001453),
                        unfocusedLabelColor = Color(0xFF74777F),
                        focusedTextColor = Color(0xFF1B1B1F),
                        unfocusedTextColor = Color(0xFF1B1B1F)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("IP o Dominio") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_host_edit"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF001453),
                            unfocusedBorderColor = Color(0xFFC7C6D0),
                            focusedLabelColor = Color(0xFF001453),
                            unfocusedLabelColor = Color(0xFF74777F),
                            focusedTextColor = Color(0xFF1B1B1F),
                            unfocusedTextColor = Color(0xFF1B1B1F)
                        )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Puerto") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF001453),
                            unfocusedBorderColor = Color(0xFFC7C6D0),
                            focusedLabelColor = Color(0xFF001453),
                            unfocusedLabelColor = Color(0xFF74777F),
                            focusedTextColor = Color(0xFF1B1B1F),
                            unfocusedTextColor = Color(0xFF1B1B1F)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Usuario SSH") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF001453),
                        unfocusedBorderColor = Color(0xFFC7C6D0),
                        focusedLabelColor = Color(0xFF001453),
                        unfocusedLabelColor = Color(0xFF74777F),
                        focusedTextColor = Color(0xFF1B1B1F),
                        unfocusedTextColor = Color(0xFF1B1B1F)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Método de Autenticación",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1B1F),
                    fontSize = 12.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { authType = "PASSWORD" }
                    ) {
                        RadioButton(
                            selected = authType == "PASSWORD",
                            onClick = { authType = "PASSWORD" },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF001453))
                        )
                        Text("Contraseña", color = Color(0xFF1B1B1F), fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { authType = "PRIVATE_KEY" }
                    ) {
                        RadioButton(
                            selected = authType == "PRIVATE_KEY",
                            onClick = { authType = "PRIVATE_KEY" },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF001453))
                        )
                        Text("Clave Privada RSA", color = Color(0xFF1B1B1F), fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (authType == "PASSWORD") {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña SSH") },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Close else Icons.Default.Info,
                                    contentDescription = "Ver contraseña",
                                    tint = Color(0xFF44474F),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF001453),
                            unfocusedBorderColor = Color(0xFFC7C6D0),
                            focusedLabelColor = Color(0xFF001453),
                            unfocusedLabelColor = Color(0xFF74777F),
                            focusedTextColor = Color(0xFF1B1B1F),
                            unfocusedTextColor = Color(0xFF1B1B1F)
                        )
                    )
                } else {
                    OutlinedTextField(
                        value = privateKey,
                        onValueChange = { privateKey = it },
                        label = { Text("Llave Privada PEM") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(144.dp),
                        maxLines = 6,
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF1B1B1F)
                        ),
                        placeholder = {
                            Text(
                                "-----BEGIN OPENSSH PRIVATE KEY-----\n...",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF74777F)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF001453),
                            unfocusedBorderColor = Color(0xFFC7C6D0),
                            focusedTextColor = Color(0xFF1B1B1F),
                            unfocusedTextColor = Color(0xFF1B1B1F),
                            focusedLabelColor = Color(0xFF001453),
                            unfocusedLabelColor = Color(0xFF74777F)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                var isSudoPasswordVisible by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = sudoPassword,
                    onValueChange = { sudoPassword = it },
                    label = { Text("Contraseña Sudo/Root (Opcional)") },
                    singleLine = true,
                    visualTransformation = if (isSudoPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isSudoPasswordVisible = !isSudoPasswordVisible }) {
                            Icon(
                                imageVector = if (isSudoPasswordVisible) Icons.Default.Close else Icons.Default.Info,
                                contentDescription = "Ver contraseña root",
                                tint = Color(0xFF44474F),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF001453),
                        unfocusedBorderColor = Color(0xFFC7C6D0),
                        focusedLabelColor = Color(0xFF001453),
                        unfocusedLabelColor = Color(0xFF74777F),
                        focusedTextColor = Color(0xFF1B1B1F),
                        unfocusedTextColor = Color(0xFF1B1B1F)
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButtonCompat(
                        onClick = onDismiss,
                        label = "Cancelar"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && host.isNotBlank()) {
                                onSave(
                                    PCProfile(
                                        id = profile?.id ?: 0,
                                        name = name,
                                        host = host,
                                        port = port.toIntOrNull() ?: 22,
                                        username = username,
                                        authType = authType,
                                        password = password,
                                        privateKey = privateKey,
                                        isDefault = isDefault,
                                        sudoPassword = sudoPassword
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF001453)),
                        shape = RoundedCornerShape(24.dp),
                        enabled = name.isNotBlank() && host.isNotBlank()
                    ) {
                        Text(
                            text = if (profile == null) "Agregar" else "Actualizar",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Inline TextButton replacement to guarantee clean compiler imports
@Composable
fun TextButtonCompat(onClick: () -> Unit, label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(text = label, color = Color(0xFF44474F), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}


// COMMAND INITIALIZER / RECIPE EXECUTOR DIALOG
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CommandEditorDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Personalizado") }
    var iconName by remember { mutableStateOf("code") }

    val categories = listOf("Audio", "Media", "System", "Utility", "Personalizado")
    val icons = listOf(
        "volume_off" to "Silencio",
        "volume_up" to "Subir Vol",
        "volume_down" to "Bajar Vol",
        "play_arrow" to "Play/Pause",
        "lock" to "Bloquear",
        "nights_stay" to "Suspender",
        "power_settings_new" to "Apagar",
        "info" to "Información",
        "thermostat" to "Temperatura",
        "memory" to "Memoria",
        "storage" to "Disco",
        "schedule" to "Tiempo",
        "settings" to "Atajo Ssh"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.2.dp, Color(0xFFC7C6D0), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Registrar Atajo Remoto",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1B1F)
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del botón (e.g., Silenciar PC)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF001453),
                        unfocusedBorderColor = Color(0xFFC7C6D0),
                        focusedLabelColor = Color(0xFF001453),
                        unfocusedLabelColor = Color(0xFF74777F),
                        focusedTextColor = Color(0xFF1B1B1F),
                        unfocusedTextColor = Color(0xFF1B1B1F)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Comando Bash a ejecutar") },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    placeholder = { Text("amixer set Master toggle", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF74777F)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF001453),
                        unfocusedBorderColor = Color(0xFFC7C6D0),
                        focusedLabelColor = Color(0xFF001453),
                        unfocusedLabelColor = Color(0xFF74777F),
                        focusedTextColor = Color(0xFF1B1B1F),
                        unfocusedTextColor = Color(0xFF1B1B1F)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Categoría",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1B1F),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = category == cat
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) Color(0xFFE1E2EC) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF001453) else Color(0xFFC7C6D0),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { category = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) Color(0xFF001453) else Color(0xFF44474F),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Icono",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1B1F),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Select Icon representation using a clean, native FlowRow (avoids nested scrolling crashes)
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFC7C6D0), RoundedCornerShape(12.dp))
                        .background(Color(0xFFF9F9FF))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    icons.forEach { (icName, label) ->
                        val isSelected = iconName == icName
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) Color(0xFFE1E2EC) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF001453) else Color(0xFFC7C6D0).copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { iconName = icName }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = getIconForName(icName),
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFF001453) else Color(0xFF44474F),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = label,
                                    color = if (isSelected) Color(0xFF001453) else Color(0xFF44474F),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButtonCompat(
                        onClick = onDismiss,
                        label = "Cancelar"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && code.isNotBlank()) {
                                onSave(name, code, category, iconName)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF001453)),
                        shape = RoundedCornerShape(24.dp),
                        enabled = name.isNotBlank() && code.isNotBlank()
                    ) {
                        Text(
                            text = "Agregar",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StorageTabContent(viewModel: MainViewModel) {
    val context = LocalContext.current
    val localDir = remember {
        val extDir = context.getExternalFilesDir(null)
        if (extDir != null) {
            java.io.File(extDir, "PC_Transfers")
        } else {
            java.io.File(context.filesDir, "PC_Transfers")
        }
    }

    // Collect States
    val remotePath by viewModel.remotePath.collectAsStateWithLifecycle()
    val remoteFiles by viewModel.remoteFiles.collectAsStateWithLifecycle()
    val isBrowsing by viewModel.isBrowsing.collectAsStateWithLifecycle()
    val remoteDiskInfo by viewModel.remoteDiskInfo.collectAsStateWithLifecycle()
    val localFiles by viewModel.localFiles.collectAsStateWithLifecycle()
    val storageStatus by viewModel.storageStatus.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()

    var customPathInput by remember(remotePath) { mutableStateOf(remotePath) }

    // Init and Live listeners
    LaunchedEffect(activeProfile, remotePath) {
        viewModel.loadLocalFiles(localDir)
        if (activeProfile != null) {
            viewModel.loadRemoteFilesAndDisk()
        }
    }

    // Upload Contract
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            var name = "archivo_subido"
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            name = it.getString(nameIndex)
                        }
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                val lastSegment = uri.lastPathSegment
                if (!lastSegment.isNullOrBlank()) {
                    name = lastSegment
                }
            }
            viewModel.uploadLocalFileToRemote(context.contentResolver, uri, name, localDir)
        }
    }

    // Measure Android Space
    val androidStorageInfo = remember(localFiles) {
        try {
            val statFs = android.os.StatFs(context.filesDir.path)
            val totalBytes = statFs.blockCountLong * statFs.blockSizeLong
            val availableBytes = statFs.availableBlocksLong * statFs.blockSizeLong
            val usedBytes = totalBytes - availableBytes
            val percentage = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()) else 0f
            Triple(totalBytes, availableBytes, percentage)
        } catch (e: Exception) {
            Triple(1L, 1L, 0f)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- STATUS TRANSFFER HUD BAR ---
        storageStatus?.let { status ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDDE1FF)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Estado",
                            tint = Color(0xFF001453),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = status,
                            color = Color(0xFF001453),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearStorageStatus() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color(0xFF001453),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (isBrowsing) {
                        LinearProgressIndicator(
                            color = Color(0xFF001453),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // --- SECTION 1: STORAGE OVERVIEW ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Android Local Usage Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.2.dp, Color(0xFFC7C6D0), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Android (Local)",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B1B1F),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val totalGB = androidStorageInfo.first / (1024 * 1024 * 1024)
                        val availGB = androidStorageInfo.second / (1024 * 1024 * 1024)
                        val usedGB = totalGB - availGB

                        LinearProgressIndicator(
                            progress = { androidStorageInfo.third },
                            color = Color(0xFF001453),
                            trackColor = Color(0xFFE1E2EC),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${usedGB}GB de ${totalGB}GB usados",
                            color = Color(0xFF44474F),
                            fontSize = 11.sp
                        )
                    }
                }

                // PC Storage Disk Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.2.dp, Color(0xFFC7C6D0), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "PC (Remoto)",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B1B1F),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        if (activeProfile == null) {
                            Text(
                                text = "Sin conexión activa",
                                color = Color(0xFF601410),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            val disk = remoteDiskInfo
                            if (disk != null) {
                                val percentFloat = (disk.usePercent.replace("%", "").trim().toFloatOrNull() ?: 0f) / 100f
                                LinearProgressIndicator(
                                    progress = { percentFloat },
                                    color = Color(0xFF2E7D32),
                                    trackColor = Color(0xFFD9E7CB),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${disk.used} de ${disk.total} usados",
                                    color = Color(0xFF44474F),
                                    fontSize = 11.sp
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.loadRemoteFilesAndDisk() }
                                ) {
                                    Text(
                                        text = "Cargar capacidad...",
                                        color = Color(0xFF001453),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION 2: REMOTE PC BROWSING ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, Color(0xFFC7C6D0), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Navegador de PC Remoto",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B1B1F)
                        )

                        IconButton(
                            onClick = { viewModel.loadRemoteFilesAndDisk() },
                            enabled = activeProfile != null && !isBrowsing
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Cargar Directorio",
                                tint = Color(0xFF001453)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (activeProfile == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF2B8B5), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Para ver e intercambiar archivos con tu PC, primero debes configurar y activar un perfil de PC en la pestaña 'Conexiones'.",
                                color = Color(0xFF601410),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        // Interactive Address bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val current = remotePath
                                    if (current != "." && current != "/") {
                                        val parent = if (current.contains("/")) {
                                            val idx = current.lastIndexOf("/")
                                            if (idx == 0) "/" else current.substring(0, idx)
                                        } else {
                                            "."
                                        }
                                        viewModel.setRemotePath(parent)
                                        viewModel.loadRemoteFilesAndDisk()
                                    }
                                },
                                enabled = remotePath != "." && remotePath != "/"
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Directorio Padre",
                                    tint = if (remotePath != "." && remotePath != "/") Color(0xFF001453) else Color.Gray
                                )
                            }

                            OutlinedTextField(
                                value = customPathInput,
                                onValueChange = { customPathInput = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Directorio Remoto", fontSize = 11.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF001453),
                                    unfocusedBorderColor = Color(0xFFC7C6D0),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedTextColor = Color(0xFF1B1B1F),
                                    unfocusedTextColor = Color(0xFF1B1B1F)
                                )
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Button(
                                onClick = {
                                    viewModel.setRemotePath(customPathInput)
                                    viewModel.loadRemoteFilesAndDisk()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF001453))
                            ) {
                                Text("Ir", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Files listing Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .border(1.dp, Color(0xFFC7C6D0), RoundedCornerShape(14.dp))
                                .background(Color(0xFFF9F9FF))
                                .padding(6.dp)
                        ) {
                            if (isBrowsing) {
                                CircularProgressIndicator(
                                    color = Color(0xFF001453),
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            } else if (remoteFiles.isEmpty()) {
                                Text(
                                    text = "Ningún archivo listado o conexión pendiente.",
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(16.dp),
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(remoteFiles) { file ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (file.isDirectory) Color(0xFFDDE1FF).copy(alpha = 0.2f) else Color.Transparent)
                                                .clickable {
                                                    if (file.isDirectory) {
                                                        viewModel.setRemotePath(file.path)
                                                        viewModel.loadRemoteFilesAndDisk()
                                                    }
                                                }
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (file.isDirectory) "📁" else "📄",
                                                fontSize = 18.sp,
                                                modifier = Modifier.padding(end = 6.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = file.name,
                                                    fontWeight = if (file.isDirectory) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF1B1B1F)
                                                )
                                                if (!file.isDirectory) {
                                                    Text(
                                                        text = formatSize(file.size),
                                                        fontSize = 10.sp,
                                                        color = Color(0xFF44474F)
                                                    )
                                                }
                                            }

                                            if (!file.isDirectory) {
                                                IconButton(
                                                    onClick = { viewModel.downloadRemoteFile(file, localDir) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.KeyboardArrowDown,
                                                        contentDescription = "Descargar a Android",
                                                        tint = Color(0xFF001453),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Trigger Device -> PC Upload
                        Button(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF001453)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Subir Archivo de Android a PC",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // --- SECTION 3: ANDROID LOCAL TRANSFERS TRACK ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, Color(0xFFC7C6D0), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Descargas Locales en Android",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B1B1F)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (localFiles.isEmpty()) {
                        Text(
                            text = "Aún no se han descargado archivos desde la PC.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            localFiles.forEach { file ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF9F9FF), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = null,
                                        tint = Color(0xFF001453),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF1B1B1F)
                                        )
                                        Text(
                                            text = formatSize(file.length()),
                                            fontSize = 10.sp,
                                            color = Color(0xFF44474F)
                                        )
                                    }

                                    // Local Actions: Share
                                    IconButton(
                                        onClick = {
                                            try {
                                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.fileprovider",
                                                    file
                                                )
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "*/*"
                                                    putExtra(Intent.EXTRA_TITLE, file.name)
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                val chooser = Intent.createChooser(intent, "Compartir con").apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(chooser)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Compartir",
                                            tint = Color(0xFF001453),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Local Actions: Delete
                                    IconButton(
                                        onClick = { viewModel.deleteLocalFile(file, localDir) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar local",
                                            tint = Color(0xFF601410),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatSize(sizeInBytes: Long): String {
    if (sizeInBytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(sizeInBytes.toDouble()) / Math.log10(1024.0)).toInt()
    if (digitGroups >= units.size) return "$sizeInBytes B"
    return String.format(java.util.Locale.US, "%.1f %s", sizeInBytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

@Composable
fun CopilotoIaTabContent(viewModel: MainViewModel) {
    val apiKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()
    val modelName by viewModel.geminiModel.collectAsStateWithLifecycle()
    val systemPrompt by viewModel.geminiSystemPrompt.collectAsStateWithLifecycle()
    val groundingEnabled by viewModel.geminiGroundingEnabled.collectAsStateWithLifecycle()

    val chatHistory by viewModel.aiChatHistory.collectAsStateWithLifecycle()
    val isLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val aiError by viewModel.aiError.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()

    var showSettings by remember { mutableStateOf(false) }
    var inputApiKey by remember { mutableStateOf(apiKey) }
    var inputModelName by remember { mutableStateOf(modelName) }
    var inputSystemPrompt by remember { mutableStateOf(systemPrompt) }
    var inputGrounding by remember { mutableStateOf(groundingEnabled) }

    LaunchedEffect(apiKey, modelName, systemPrompt, groundingEnabled) {
        inputApiKey = apiKey
        inputModelName = modelName
        inputSystemPrompt = systemPrompt
        inputGrounding = groundingEnabled
    }

    var userMessageText by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()

    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            try {
                lazyListState.animateScrollToItem(chatHistory.size - 1)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().animateContentSize()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSettings = !showSettings },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes de IA",
                            tint = Color(0xFF001453),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Configuración Copiloto Gemini & Gemma",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B1B1F)
                        )
                    }
                    Icon(
                        imageVector = if (showSettings) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expandir/Colapsar",
                        tint = Color(0xFF74777F)
                    )
                }

                if (showSettings) {
                    Spacer(modifier = Modifier.height(12.dp))

                    var isKeyVisible by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = inputApiKey,
                        onValueChange = { inputApiKey = it },
                        label = { Text("Gemini API Key") },
                        singleLine = true,
                        placeholder = { Text("AIzaSy...") },
                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                Icon(
                                    imageVector = if (isKeyVisible) Icons.Default.Close else Icons.Default.Info,
                                    contentDescription = "Ver clave",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1B1B1F),
                            unfocusedTextColor = Color(0xFF1B1B1F),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Seleccionar Modelo:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF44474F)
                    )
                    
                    val modelsList = listOf(
                        "gemma-4-31b-it" to "Gemma 4 31B",
                        "gemini-3.5-flash" to "Gemini 3.5 Flash",
                        "gemini-3.1-pro-preview" to "Gemini 3.1 Pro (Preview)",
                        "gemini-2.5-flash" to "Gemini 2.5 Flash",
                        "gemini-2.5-pro" to "Gemini 2.5 Pro"
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        modelsList.forEach { (modelId, label) ->
                            val isSelected = inputModelName == modelId
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF001453) else Color(0xFFC7C6D0)),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFFE8DDFF).copy(alpha = 0.5f) else Color.Transparent
                                ),
                                modifier = Modifier
                                    .clickable { inputModelName = modelId }
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF001453) else Color(0xFF44474F),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputSystemPrompt,
                        onValueChange = { inputSystemPrompt = it },
                        label = { Text("System Prompt (Instrucción)") },
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1B1B1F),
                            unfocusedTextColor = Color(0xFF1B1B1F)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Búsqueda de Google (Grounding)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B1B1F)
                            )
                            Text(
                                text = "Habilita la consulta de información en tiempo real mediante Google Search.",
                                fontSize = 11.sp,
                                color = Color(0xFF74777F)
                            )
                        }
                        Switch(
                            checked = inputGrounding,
                            onCheckedChange = { inputGrounding = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF001453),
                                checkedTrackColor = Color(0xFFE8DDFF)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            viewModel.updateGeminiSettings(
                                apiKey = inputApiKey,
                                model = inputModelName,
                                systemPrompt = inputSystemPrompt,
                                grounding = inputGrounding
                            )
                            showSettings = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF001453)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Guardar Configuración", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val effectiveKey = viewModel.getEffectiveApiKey()
        if (effectiveKey.isBlank() || effectiveKey == "MY_GEMINI_API_KEY") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F1)),
                border = BorderStroke(1.dp, Color(0xFFFF9800)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Clave faltante",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Llave de Gemini no guardada",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF601410)
                        )
                        Text(
                            text = "Para usar el Copiloto, expande el menú de configuración arriba y añade tu API Key de Gemini API.",
                            fontSize = 11.sp,
                            color = Color(0xFF601410)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFEEEFF4), RoundedCornerShape(12.dp))
        ) {
            if (chatHistory.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Copiloto listo para ayudarte",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF74777F)
                    )
                    Text(
                        text = "Escribe una instrucción para recomendarte comandos.",
                        fontSize = 12.sp,
                        color = Color(0xFF74777F)
                    )
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chatHistory) { msg ->
                        val isUser = msg.role == "user"
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(0.9f),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isUser) Color(0xFFECEEFE) else Color(0xFFF3F4F9)
                                    ),
                                    shape = RoundedCornerShape(
                                        topStart = 12.dp,
                                        topEnd = 12.dp,
                                        bottomStart = if (isUser) 12.dp else 2.dp,
                                        bottomEnd = if (isUser) 2.dp else 12.dp
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = msg.text,
                                            fontSize = 13.sp,
                                            color = Color(0xFF1B1B1F)
                                        )
                                        
                                        if (msg.commands.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Comandos sugeridos (toca para ejecutar):",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF001453)
                                            )
                                            msg.commands.forEach { cmd ->
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                                    border = BorderStroke(1.dp, Color(0xFF001453).copy(alpha = 0.3f)),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp)
                                                        .clickable {
                                                            if (activeProfile == null) {
                                                                // No action
                                                            } else {
                                                                viewModel.executeRemoteCommand("Copiloto IA", cmd)
                                                            }
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.PlayArrow,
                                                            contentDescription = "Ejecutar comando",
                                                            tint = Color(0xFF2E7D32),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = cmd,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF1B1B1F)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Text(
                                            text = msg.timestamp,
                                            fontSize = 9.sp,
                                            color = Color(0xFF74777F),
                                            modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (isLoading) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF001453)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Copiloto está pensando...",
                                    fontSize = 12.sp,
                                    color = Color(0xFF74777F)
                                )
                            }
                        }
                    }
                }
            }
        }

        aiError?.let { err ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = err,
                        fontSize = 12.sp,
                        color = Color(0xFFD32F2F)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.clearAiChat() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Limpiar chat",
                    tint = Color(0xFF601410)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            OutlinedTextField(
                value = userMessageText,
                onValueChange = { userMessageText = it },
                placeholder = { Text("Pregunta por comandos o tareas...") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF1B1B1F),
                    unfocusedTextColor = Color(0xFF1B1B1F)
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = {
                    if (userMessageText.isNotBlank()) {
                        viewModel.sendAiMessage(userMessageText)
                        userMessageText = ""
                    }
                },
                enabled = userMessageText.isNotBlank() && !isLoading,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Enviar",
                    tint = if (userMessageText.isNotBlank()) Color(0xFF001453) else Color(0xFFC7C6D0)
                )
            }
        }
    }
}
