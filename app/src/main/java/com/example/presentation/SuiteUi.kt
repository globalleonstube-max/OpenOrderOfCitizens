package com.example.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*

// Custom Color Palette for the Cosmic P2P Brotherhood - Redefined for Positive Light Google/Apple style
val SpaceDark = Color(0xFFF1F5F9) // Warm, premium light slate gray background
val CosmicGray = Color(0xFFFFFFFF) // Pure white, clean cards
val AccentTeal = Color(0xFF1A73E8) // Bright, positive modern blue (Google/Apple style)
val HighContrastGold = Color(0xFFD97706) // Legible honey amber for reputation scores
val CyberGreen = Color(0xFF16A34A) // Calm, positive grass green for success
val SoftCyan = Color(0xFF0284C7) // Sky blue for secondary elements
val TextLight = Color(0xFF0F172A) // Sleek slate-black text (formerly TextLight prefix)
val TextMuted = Color(0xFF64748B) // Slate 500 gray for subtext
val AlertRed = Color(0xFFDC2626) // Pristine, professional red

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenOrderSocialOSApp(viewModel: SuiteViewModel) {
    val currentAgentId by viewModel.currentAgentId.collectAsState()
    val selectedForkId by viewModel.selectedForkId.collectAsState()
    val agents by viewModel.agents.collectAsState()
    val proposals by viewModel.proposals.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val disputes by viewModel.disputes.collectAsState()
    val forks by viewModel.forks.collectAsState()
    val syncLogs by viewModel.syncLogs.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val consoleOutput by viewModel.consoleOutput.collectAsState()
    val showOnboarding by viewModel.showOnboarding.collectAsState()
    val isSimulationEnabled by viewModel.isSimulationEnabled.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Идеология, 1: Совет & Управа, 2: Суд, 3: Форки & Сеть
    var showRegisterDialog by remember { mutableStateOf(false) }
    var showGoogleSetupDialog by remember { mutableStateOf(false) }
    var inputGoogleAppPass by remember { mutableStateOf("") }

    val context = androidx.compose.ui.platform.LocalContext.current

    val handleGoogleJoin = {
        val googleEmail = "leonidyasin@gmail.com"
        val googleName = "Leonid Yasin"
        // Register Agent with Google Email directly!
        viewModel.registerAgent(
            id = googleEmail,
            name = googleName,
            role = "Knight",
            initialRep = 60.0,
            customPubKey = "0x" + java.util.UUID.randomUUID().toString().replace("-", "").take(16).uppercase(),
            autoLogin = true
        )
        // Auto-configure POP3/SMTP/IMAP servers for Gmail
        viewModel.updateMailSettings(
            smtpH = "smtp.gmail.com",
            smtpP = 465,
            imapH = "imap.gmail.com",
            imapP = 993,
            usr = googleEmail,
            pas = "", // Will let user enter as App Password
            ssl = true
        )
        viewModel.dismissOnboarding()
        viewModel.writeConsole("Вход через Google: $googleEmail. Автонастройка Mail P2P (Gmail SMTP/IMAP).")
        showGoogleSetupDialog = true
        activeTab = 3 // Switch directly to network configuration to complete setup if needed!
    }

    val currentAgent = agents.find { it.id == currentAgentId }
    val currentFork = forks.find { it.id == selectedForkId }

    var isConsoleVisible by remember { mutableStateOf(true) } // Enable console by default so the user immediately sees the beautiful CLI Chat!
    var isLeftDrawerOpen by remember { mutableStateOf(false) }
    var isRightDrawerOpen by remember { mutableStateOf(false) }
    var consoleViewMode by remember { mutableStateOf("split") } // "split", "full"
    var cliInputText by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SpaceDark,
                        titleContentColor = AccentTeal
                    ),
                    navigationIcon = {
                        IconButton(onClick = { isLeftDrawerOpen = true }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Navigation Menu",
                                tint = AccentTeal
                            )
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Order Logo",
                                tint = AccentTeal,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Column {
                                Text(
                                    text = "ОТКРЫТЫЙ ОРДЕН",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp,
                                        fontSize = 14.sp
                                    )
                                )
                                Text(
                                        "Social Operating System • P2P Kernel v1.0",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextMuted,
                                        fontSize = 9.sp
                                    )
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { activeTab = 0 },
                            modifier = Modifier.testTag("console_toggle_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "CLI Чат",
                                tint = if (activeTab == 0) AccentTeal else TextMuted
                            )
                        }
                        IconButton(
                            onClick = { viewModel.triggerSync() },
                            modifier = Modifier.testTag("sync_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync P2P Mail",
                                tint = AccentTeal
                            )
                        }
                        IconButton(
                            onClick = { isRightDrawerOpen = true },
                            modifier = Modifier.testTag("right_panel_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Open Profile & Node Status",
                                tint = AccentTeal
                            )
                        }
                    }
                )
            },
        bottomBar = {
            NavigationBar(
                containerColor = CosmicGray,
                contentColor = TextLight,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Chat, "Chat") },
                    label = { Text("Чат CLI", fontSize = 10.sp, fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = AccentTeal,
                        indicatorColor = AccentTeal,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.List, "Map") },
                    label = { Text("Карта", fontSize = 10.sp, fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = AccentTeal,
                        indicatorColor = AccentTeal,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.Home, "Architecture") },
                    label = { Text("Идея", fontSize = 10.sp, fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = AccentTeal,
                        indicatorColor = AccentTeal,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(Icons.Default.CheckCircle, "Council") },
                    label = { Text("Совет", fontSize = 10.sp, fontWeight = if (activeTab == 3) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = AccentTeal,
                        indicatorColor = AccentTeal,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 4,
                    onClick = { activeTab = 4 },
                    icon = { Icon(Icons.Default.Lock, "Judiciary") },
                    label = { Text("Суд", fontSize = 10.sp, fontWeight = if (activeTab == 4) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = AccentTeal,
                        indicatorColor = AccentTeal,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 5,
                    onClick = { activeTab = 5 },
                    icon = { Icon(Icons.Default.Share, "Network") },
                    label = { Text("Сеть", fontSize = 10.sp, fontWeight = if (activeTab == 5) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = AccentTeal,
                        indicatorColor = AccentTeal,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SpaceDark)
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    0 -> CliChatTab(viewModel)
                    1 -> MindMapTab(viewModel) { destination ->
                        activeTab = when (destination) {
                            0 -> 1
                            1 -> 2
                            2 -> 3
                            3 -> 4
                            4 -> 5
                            else -> 1
                        }
                    }
                    2 -> ArchitectureTab(viewModel) { activeTab = 0 }
                    3 -> CouncilAndTasksTab(viewModel, proposals, tasks, currentAgentId, selectedForkId) { activeTab = 0 }
                    4 -> JudiciaryTab(viewModel, disputes, agents, currentAgentId) { activeTab = 0 }
                    5 -> RepositoryForksTab(viewModel, forks, syncLogs, selectedForkId, agents) { activeTab = 0 }
                    else -> CliChatTab(viewModel)
                }
            }

            if (showRegisterDialog) {
                var regId by remember { mutableStateOf("") }
                var regName by remember { mutableStateOf("") }
                var regRole by remember { mutableStateOf("Knight") }
                val roles = listOf("Adept", "Knight", "Arbiter", "Scribe", "Grandmaster")
                var regRoleExpanded by remember { mutableStateOf(false) }
                var regInitialRep by remember { mutableStateOf(20f) }
                var regAutoLogin by remember { mutableStateOf(true) }
                var regCustomPubKey by remember { mutableStateOf("") }

                LaunchedEffect(showRegisterDialog) {
                    if (showRegisterDialog) {
                        regId = ""
                        regName = ""
                        regCustomPubKey = "0x" + UUID.randomUUID().toString().replace("-", "").take(16).uppercase()
                        regInitialRep = 20f
                    }
                }

                AlertDialog(
                    onDismissRequest = { showRegisterDialog = false },
                    title = {
                        Column {
                            Text(
                                text = "ВСТУПИТЬ В ОТКРЫТЫЙ ОРДЕН",
                                color = HighContrastGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Создание децентрализованного манифеста и ключей доступа",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Google Quick Sign-In Option
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        handleGoogleJoin()
                                        showRegisterDialog = false
                                    }
                                    .testTag("google_quick_join_btn"),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFDADCE0))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("G ", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Быстрый вход через Google", color = Color(0xFF3C4043), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f).height(1.dp).background(TextMuted.copy(alpha = 0.2f)))
                                Text(" ИЛИ ВВЕСТИ ВРУЧНУЮ ", color = TextMuted, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 8.dp))
                                Box(modifier = Modifier.weight(1f).height(1.dp).background(TextMuted.copy(alpha = 0.2f)))
                            }

                            OutlinedTextField(
                                value = regId,
                                onValueChange = { regId = it },
                                label = { Text("P2P ID (например, user@orden.p2p)") },
                                placeholder = { Text("id@orden.p2p") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentTeal,
                                    focusedLabelColor = AccentTeal,
                                    unfocusedBorderColor = TextMuted,
                                    unfocusedLabelColor = TextMuted,
                                    focusedTextColor = TextLight,
                                    unfocusedTextColor = TextLight
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("reg_id_input"),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = regName,
                                onValueChange = { regName = it },
                                label = { Text("Имя / Псевдоним") },
                                placeholder = { Text("Антон") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentTeal,
                                    focusedLabelColor = AccentTeal,
                                    unfocusedBorderColor = TextMuted,
                                    unfocusedLabelColor = TextMuted,
                                    focusedTextColor = TextLight,
                                    unfocusedTextColor = TextLight
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("reg_name_input"),
                                singleLine = true
                            )

                            // Role selector Dropdown
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = when (regRole) {
                                        "Grandmaster" -> "Грандмастер (Grandmaster)"
                                        "Knight" -> "Рыцарь (Knight)"
                                        "Arbiter" -> "Арбитр (Arbiter)"
                                        "Scribe" -> "Писец (Scribe)"
                                        else -> "Адепт (Adept)"
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Выбор Орденской Роли") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AccentTeal,
                                        focusedLabelColor = AccentTeal,
                                        unfocusedBorderColor = TextMuted,
                                        unfocusedLabelColor = TextMuted,
                                        focusedTextColor = TextLight,
                                        unfocusedTextColor = TextLight
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { regRoleExpanded = true },
                                    trailingIcon = {
                                        IconButton(onClick = { regRoleExpanded = true }) {
                                            Icon(Icons.Default.ArrowDropDown, "Select Role", tint = AccentTeal)
                                        }
                                    }
                                )
                                DropdownMenu(
                                    expanded = regRoleExpanded,
                                    onDismissRequest = { regRoleExpanded = false },
                                    modifier = Modifier.background(CosmicGray).fillMaxWidth(0.8f)
                                ) {
                                    roles.forEach { r ->
                                        val rName = when (r) {
                                            "Grandmaster" -> "Грандмастер"
                                            "Knight" -> "Рыцарь"
                                            "Arbiter" -> "Арбитр"
                                            "Scribe" -> "Писец"
                                            else -> "Адепт"
                                        }
                                        DropdownMenuItem(
                                            text = { Text("$rName ($r)", color = TextLight) },
                                            onClick = {
                                                regRole = r
                                                regRoleExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Pub key display
                            OutlinedTextField(
                                value = regCustomPubKey,
                                onValueChange = { regCustomPubKey = it },
                                label = { Text("Ed25519 Публичный ключ (генерируется)") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentTeal,
                                    focusedLabelColor = AccentTeal,
                                    unfocusedBorderColor = TextMuted,
                                    unfocusedLabelColor = TextMuted,
                                    focusedTextColor = TextMuted,
                                    unfocusedTextColor = TextMuted
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = {
                                        regCustomPubKey = "0x" + UUID.randomUUID().toString().replace("-", "").take(16).uppercase()
                                    }) {
                                        Icon(Icons.Default.Refresh, "Re-generate key", tint = AccentTeal)
                                    }
                                }
                            )

                            // Initial Reputation
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Стартовая репутация:", color = TextLight, fontSize = 12.sp)
                                    Text("${regInitialRep.toInt()} REP", color = HighContrastGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Slider(
                                    value = regInitialRep,
                                    onValueChange = { regInitialRep = it },
                                    valueRange = 10f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = AccentTeal,
                                        activeTrackColor = AccentTeal,
                                        inactiveTrackColor = TextMuted.copy(alpha = 0.3f)
                                    )
                                )
                            }

                            // Auto login checkbox
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { regAutoLogin = !regAutoLogin },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = regAutoLogin,
                                    onCheckedChange = { regAutoLogin = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = AccentTeal,
                                        uncheckedColor = TextMuted
                                    )
                                )
                                Text("Автоматически войти под этой сессией", color = TextLight, fontSize = 13.sp)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (regName.isBlank() || regId.isBlank()) return@Button
                                val finalId = if (regId.contains("@")) regId else "${regId.trim()}@orden.p2p"
                                viewModel.registerAgent(
                                    id = finalId,
                                    name = regName.trim(),
                                    role = regRole,
                                    initialRep = regInitialRep.toDouble(),
                                    customPubKey = regCustomPubKey,
                                    autoLogin = regAutoLogin
                                )
                                showRegisterDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = SpaceDark)
                        ) {
                            Text("ВСТУПИТЬ В ОРДЕН", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRegisterDialog = false }) {
                            Text("ОТМЕНА", color = TextMuted)
                        }
                    },
                    containerColor = CosmicGray,
                    tonalElevation = 8.dp
                )
            }

            if (showOnboarding) {
                OnboardingWelcomeDialog(
                    onDismiss = { viewModel.dismissOnboarding() },
                    onGoogleJoin = handleGoogleJoin,
                    onQuickJoin = {
                        viewModel.registerAgent(
                            id = "leonid_yasin@orden.p2p",
                            name = "Леонид Ясин (Рекрут)",
                            role = "Knight"
                        )
                        viewModel.dismissOnboarding()
                        activeTab = 0
                    },
                    onStartQuest = {
                        viewModel.dismissOnboarding()
                        activeTab = 0
                    }
                )
            }

            if (showGoogleSetupDialog) {
                AlertDialog(
                    onDismissRequest = { showGoogleSetupDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("G ", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Интеграция с Google Mail", color = HighContrastGold, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Узел OpenOrder Social OS настроен на криптографическую подпись и Gmail P2P транспорт через ваш аккаунт leonidyasin@gmail.com.",
                                color = TextLight,
                                fontSize = 13.sp
                            )
                            Text(
                                "Для защиты ваших личных данных Google блокирует вход внешних программ по основному паролю. Вам необходимо создать специальный безопасный пароль приложения.\n\nШаги:\n1️⃣ Перейдите в настройки своего аккаунта Google (Безопасность → Двухэтапная аутентификация → Пароли приложений)\n2️⃣ Сгенерируйте и скопируйте 16-значный код пароля приложения для 'OpenOrder'\n3️⃣ Вставьте сгенерированный код ниже:",
                                color = TextLight,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                            
                            OutlinedTextField(
                                value = inputGoogleAppPass,
                                onValueChange = { inputGoogleAppPass = it },
                                label = { Text("Пароль приложения Google (App Password)") },
                                placeholder = { Text("например, abcd efgh ijkl mnop") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentTeal,
                                    focusedLabelColor = AccentTeal,
                                    unfocusedBorderColor = TextMuted,
                                    unfocusedLabelColor = TextMuted,
                                    focusedTextColor = TextLight,
                                    unfocusedTextColor = TextLight
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("google_app_pass_input"),
                                singleLine = true
                            )
                            
                            Button(
                                onClick = {
                                    try {
                                        val uri = android.net.Uri.parse("https://myaccount.google.com/apppasswords")
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        viewModel.showToast("Не удалось открыть браузер: ${e.message}")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CosmicGray, contentColor = AccentTeal),
                                border = BorderStroke(1.dp, Color(0xFFDADCE0)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🔗 Настройки аккаунта Google", fontSize = 11.sp)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (inputGoogleAppPass.isNotBlank()) {
                                    viewModel.updateMailSettings(
                                        smtpH = "smtp.gmail.com",
                                        smtpP = 465,
                                        imapH = "imap.gmail.com",
                                        imapP = 993,
                                        usr = "leonidyasin@gmail.com",
                                        pas = inputGoogleAppPass.trim(),
                                        ssl = true
                                    )
                                    viewModel.writeConsole("Пароль приложения Google применен для P2P-ноды.")
                                    viewModel.triggerSync()
                                }
                                showGoogleSetupDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = SpaceDark)
                        ) {
                            Text("ЗАВЕРШИТЬ НАСТРОЙКУ", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showGoogleSetupDialog = false }) {
                            Text(
                                text = if (isSimulationEnabled) "ПОЗЖЕ / ИСПОЛЬЗОВАТЬ СИМУЛЯЦИЮ" else "ПОЗЖЕ (БЕЗ СИМУЛЯЦИИ)",
                                color = TextMuted
                            )
                        }
                    },
                    containerColor = CosmicGray,
                    tonalElevation = 8.dp
                )
            }
        }
    }

    // Left Drawer Overlay
        AnimatedVisibility(
            visible = isLeftDrawerOpen,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
            modifier = Modifier.zIndex(100f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.4f))
                    .clickable { isLeftDrawerOpen = false }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(300.dp)
                        .align(Alignment.CenterStart)
                        .clickable(enabled = false) {}
                        .shadow(16.dp),
                    color = CosmicGray,
                    shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(20.dp)
                    ) {
                        // Drawer Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AccentTeal.copy(0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Menu Icon",
                                    tint = AccentTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Навигация",
                                    fontWeight = FontWeight.Bold,
                                    color = TextLight,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Social OS P2P",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = TextMuted.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Menu items list
                        val menuItems = listOf(
                            Triple("💬 Чат CLI & ИИ-Проводник", "Интерфейс в стиле ChatGPT", 0),
                            Triple("🗺️ Интерактивная Карта", "Схема связи модулей системы", 1),
                            Triple("🏛️ Идея & Код", "Догматы ордена, архитектура & классы", 2),
                            Triple("📋 Совет & Управа", "Дела, задачи, REP кворумы & законы", 3),
                            Triple("⚖️ Братский Судебник", "Разборы присяжных, суверенитет", 4),
                            Triple("📡 Связь, Ключи & Сеть", "P2P SMTP транспорт & форки", 5)
                        )

                        menuItems.forEach { (title, subtitle, index) ->
                            val isSelected = activeTab == index
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        activeTab = index
                                        isLeftDrawerOpen = false
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) AccentTeal.copy(0.12f) else Color.Transparent
                                ),
                                border = if (isSelected) BorderStroke(1.dp, AccentTeal.copy(0.3f)) else null,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) AccentTeal else Color(0xFFF1F5F9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = title.take(2),
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = title.drop(2),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) AccentTeal else TextLight,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = subtitle,
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "P2P Взаимодействие:",
                                    fontWeight = FontWeight.Bold,
                                    color = TextLight,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "ИДЕЯ → ЗАДАЧИ → КОНФЛИКТЫ → СИНХРОНИЗАЦИЯ\n\nВсе компоненты жестко связаны репутационным REP-стейком участников.",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Right Drawer Overlay
        AnimatedVisibility(
            visible = isRightDrawerOpen,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.zIndex(100f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.4f))
                    .clickable { isRightDrawerOpen = false }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(300.dp)
                        .align(Alignment.CenterEnd)
                        .clickable(enabled = false) {}
                        .shadow(16.dp),
                    color = CosmicGray,
                    shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(18.dp)
                    ) {
                        // Drawer Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(HighContrastGold.copy(0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile Settings",
                                    tint = HighContrastGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Профиль и Сессия",
                                    fontWeight = FontWeight.Bold,
                                    color = TextLight,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Права, REP и Ноды",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = TextMuted.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Section 1: Active Agent profile
                        Text(
                            text = "Действующий Агент:",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        var expandedDropdown by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedDropdown = true },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = AccentTeal,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = currentAgent?.name ?: "Неизвестен",
                                            color = TextLight,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text("▾", color = TextMuted, fontSize = 13.sp)
                                }
                            }

                            DropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false },
                                modifier = Modifier
                                    .width(260.dp)
                                    .background(CosmicGray)
                            ) {
                                agents.forEach { agent ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = "${agent.name} [${agent.role}]",
                                                    color = if (agent.id == currentAgentId) AccentTeal else TextLight,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = "Репутация: ${agent.reputationScore} REP",
                                                    color = TextMuted,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectAgent(agent.id)
                                            expandedDropdown = false
                                        }
                                    )
                                }
                                HorizontalDivider(color = TextMuted.copy(alpha = 0.2f))
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add",
                                                tint = HighContrastGold,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Создать нового агента", color = HighContrastGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    },
                                    onClick = {
                                        showRegisterDialog = true
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Core Specs Info Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CosmicGray),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Ранг:", color = TextMuted, fontSize = 11.sp)
                                    Text(
                                        currentAgent?.role ?: "Абитуриент",
                                        color = AccentTeal,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Репутация (REP):", color = TextMuted, fontSize = 11.sp)
                                    Text(
                                        "${currentAgent?.reputationScore ?: 0.0} REP",
                                        color = HighContrastGold,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Адрес:", color = TextMuted, fontSize = 11.sp)
                                    Text(
                                        currentAgent?.publicKey?.take(10) ?: "0x-",
                                        fontFamily = FontFamily.Monospace,
                                        color = TextLight,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Активный P2P Форк Кода:",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SoftCyan.copy(0.06f)),
                            border = BorderStroke(1.dp, SoftCyan.copy(0.2f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        tint = SoftCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = currentFork?.title ?: "Ядро ордена",
                                        color = SoftCyan,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Вся переписка, законы и судебные споры закреплены за хэшем данного форка.",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Buttons inside drawer
                        Button(
                            onClick = {
                                viewModel.triggerSync()
                                isRightDrawerOpen = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Синхронизировать SMTP", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedButton(
                            onClick = {
                                showRegisterDialog = true
                                isRightDrawerOpen = false
                            },
                            border = BorderStroke(1.dp, HighContrastGold),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Создать новый Агент Ключ", color = HighContrastGold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingWelcomeDialog(
    onDismiss: () -> Unit,
    onGoogleJoin: () -> Unit,
    onQuickJoin: () -> Unit,
    onStartQuest: () -> Unit
) {
    var step by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = AccentTeal,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "🌌 ОТКРЫТЫЙ ОРДЕН",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HighContrastGold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Stepper indicator
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(2) { index ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (index <= step) AccentTeal else SpaceDark)
                        )
                    }
                }

                when (step) {
                    0 -> {
                        Text(
                            text = "Приветствуем в Social OS!",
                            style = MaterialTheme.typography.titleSmall,
                            color = AccentTeal,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Вы открыли интерфейс децентрализованной цифровой республики со встроенным суверенитетом и властью репутации.\n\nЗдесь нет паролей, центральных баз данных или модераторов: все решения и данные синхронизируются пир-ту-пир прямо поверх электронной почты!",
                            color = TextLight,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                    1 -> {
                        Text(
                            text = "4 Ключевых Цифровых Института",
                            style = MaterialTheme.typography.titleSmall,
                            color = AccentTeal,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "🏛️ Законодательный Совет\nГолосование репутацией за реформы ордена и форки кода.\n\n🔧 Исполнительное Братство\nБрать на себя и закрывать задачи, зарабатывая очки REP.\n\n⚖️ Независимый Суд\nКонституционные споры присяжных для соблюдения законов.\n\n📡 Стек Сетевой Синхронизации\nP2P транспорт через SMTP/IMAP поверх любых почтовых узлов.",
                            color = TextLight,
                            fontSize = 13.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (step < 1) {
                    Button(
                        onClick = { step++ },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = SpaceDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Далее ➡️", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Google Sign-In button
                        Button(
                            onClick = onGoogleJoin,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF3C4043)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("onboarding_google_join_btn")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Войти по Google", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                        Button(
                            onClick = onQuickJoin,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = SpaceDark),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("onboarding_quick_join_btn")
                        ) {
                            Text("⚔️ Вступить!", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        dismissButton = {
            if (step > 0) {
                TextButton(onClick = { step-- }) {
                    Text("⬅️ Назад", color = TextMuted, fontSize = 11.sp)
                }
            } else {
                TextButton(
                    onClick = { onDismiss() },
                    modifier = Modifier.testTag("onboarding_skip_btn")
                ) {
                    Text("Пропустить", color = TextMuted, fontSize = 11.sp)
                }
            }
        },
        containerColor = CosmicGray,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 8.dp
    )
}

// -------------------------------------------------------------
// CHATGPT-STYLE AI GUIDE & P2P RUNTIME CLI
// -------------------------------------------------------------
@Composable
fun CliChatTab(viewModel: SuiteViewModel) {
    val cliLogs by viewModel.cliLogs.collectAsState()
    var cliInputText by remember { mutableStateOf("") }
    val scrollState = rememberLazyListState()
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(cliLogs.size) {
        if (cliLogs.isNotEmpty()) {
            try {
                scrollState.animateScrollToItem(cliLogs.size - 1)
            } catch (e: Exception) {
                // Safeguard against unattached scroll state
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        // Welcoming ChatGPT-style header with direct Diagnostic Export button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicGray),
            border = BorderStroke(1.dp, AccentTeal.copy(0.15f)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(AccentTeal.copy(0.12f), shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "AI Agent",
                        tint = AccentTeal,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ОРДЕНСКИЙ ИИ-ПРОВОДНИК & CLI",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = AccentTeal,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = "Задайте вопрос ИИ или коснитесь сообщения для копирования",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                            val maskedKey = if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                                "ОТСУТСТВУЕТ (Заглушка)"
                            } else {
                                "АКТИВЕН (" + apiKey.take(6) + "..." + apiKey.takeLast(4) + ")"
                            }
                            val activeModel = com.example.data.GeminiService.getActiveModelName()
                            val report = com.example.data.DiagnosticsTracker.getDiagnosticsReport(maskedKey, activeModel)
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(report))
                            viewModel.showToast("Диагностика скопирована!")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Копировать лог диагностики",
                            tint = AccentTeal
                        )
                    }

                    IconButton(
                        onClick = {
                            val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                            val maskedKey = if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                                "ОТСУТСТВУЕТ (Заглушка)"
                            } else {
                                "АКТИВЕН (" + apiKey.take(6) + "..." + apiKey.takeLast(4) + ")"
                            }
                            val activeModel = com.example.data.GeminiService.getActiveModelName()
                            val report = com.example.data.DiagnosticsTracker.getDiagnosticsReport(maskedKey, activeModel)
                            com.example.data.DiagnosticsTracker.shareDiagnostics(context, report)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Поделиться диагностикой",
                            tint = HighContrastGold
                        )
                    }
                }
            }
        }

        // Message Feed (ChatGPT-style bubbles)
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (cliLogs.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Empty Logo",
                            tint = AccentTeal.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Консоль узла готова.",
                            color = TextLight,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Введите команду или спросите ИИ-Агента о принципах Ордена.",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(cliLogs) { log ->
                    val isCommand = log.type == "command"
                    val isUserMsg = log.text.startsWith("agent@p2p:~$") || log.text.startsWith("chat[") || log.text.startsWith("chat:~#") || isCommand
                    val isPeerMsg = log.type.startsWith("peer_")
                    val displayName = if (isUserMsg) {
                        "ВЫ"
                    } else if (isPeerMsg) {
                        val peerName = log.type.substringAfter("peer_")
                        "${peerName.uppercase()} [СОРАТНИК]"
                    } else if (log.type == "help" || log.text.contains("Проводник:")) {
                        "ГИД-ПРОВОДНИК [ИИ]"
                    } else {
                        "СИСТЕМА [CLI]"
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUserMsg) Arrangement.End else Arrangement.Start
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .padding(vertical = 2.dp),
                            horizontalAlignment = if (isUserMsg) Alignment.End else Alignment.Start
                        ) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isUserMsg -> AccentTeal
                                        isPeerMsg -> Color(0xFFD97706)
                                        log.type == "help" -> HighContrastGold
                                        else -> TextMuted
                                    },
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(bottom = 3.dp, start = 4.dp, end = 4.dp)
                            )
                            
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        isUserMsg -> Color(0xFFE2E8F0) // Premium soft gray-blue bubble for user text
                                        isPeerMsg -> Color(0xFFFEF9C3) // Light warm gold/yellow for peer replies
                                        log.type == "success" -> Color(0xFFDCFCE7) // Mint light green success
                                        log.type == "error" -> Color(0xFFFEE2E2) // Light red warning error
                                        log.type == "help" -> Color(0xFFF0FDF4) // Cosmic very light blue/green for AI
                                        else -> CosmicGray // Clean default white card
                                    }
                                ),
                                shape = RoundedCornerShape(
                                    topStart = 14.dp,
                                    topEnd = 14.dp,
                                    bottomStart = if (isUserMsg) 14.dp else 2.dp,
                                    bottomEnd = if (isUserMsg) 2.dp else 14.dp
                                ),
                                border = if (log.type == "help") BorderStroke(1.dp, AccentTeal.copy(0.35f)) else BorderStroke(0.5.dp, TextMuted.copy(0.12f)),
                                modifier = Modifier
                                    .shadow(1.dp, RoundedCornerShape(12.dp))
                                    .clickable {
                                        val cleanText = if (isUserMsg && log.text.startsWith("agent@p2p:~$ ")) {
                                            log.text.removePrefix("agent@p2p:~$ ")
                                        } else {
                                            log.text
                                        }
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(cleanText))
                                        viewModel.showToast("Текст сообщения скопирован!")
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    val cleanText = if (isUserMsg && log.text.startsWith("agent@p2p:~$ ")) {
                                        log.text.removePrefix("agent@p2p:~$ ")
                                    } else {
                                        log.text
                                    }
                                    
                                    Text(
                                        text = cleanText,
                                        color = when (log.type) {
                                            "success" -> CyberGreen
                                            "error" -> AlertRed
                                            else -> TextLight
                                        },
                                        style = if (log.type == "help") {
                                            MaterialTheme.typography.bodyMedium.copy(
                                                lineHeight = 20.sp,
                                                letterSpacing = 0.25.sp
                                            )
                                        } else {
                                            MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    )

                                    if (log.type == "error" || log.text.contains("Не удалось связаться") || log.text.contains("API-ключ") || log.text.contains("ERROR_GEMINI_API_FAIL")) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = {
                                                    val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                                                    val maskedKey = if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                                                        "ОТСУТСТВУЕТ (Заглушка)"
                                                    } else {
                                                        "АКТИВЕН (" + apiKey.take(6) + "..." + apiKey.takeLast(4) + ")"
                                                    }
                                                    val activeModel = com.example.data.GeminiService.getActiveModelName()
                                                    val report = com.example.data.DiagnosticsTracker.getDiagnosticsReport(maskedKey, activeModel)
                                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(report))
                                                    viewModel.showToast("Лог скопирован!")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Copy Error",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = Color.White
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Скопировать отчет", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                            
                                            Button(
                                                onClick = {
                                                    val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                                                    val maskedKey = if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                                                        "ОТСУТСТВУЕТ (Заглушка)"
                                                    } else {
                                                        "АКТИВЕН (" + apiKey.take(6) + "..." + apiKey.takeLast(4) + ")"
                                                    }
                                                    val activeModel = com.example.data.GeminiService.getActiveModelName()
                                                    val report = com.example.data.DiagnosticsTracker.getDiagnosticsReport(maskedKey, activeModel)
                                                    com.example.data.DiagnosticsTracker.shareDiagnostics(context, report)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = "Share Error",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = Color.White
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Отправить разработчику 🛠️", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
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

        // Suggestions Horizontal Carousel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val suggestions = listOf(
                "help" to "Справка CLI 📋",
                "status" to "Статус узла 📡",
                "diagnostics" to "Диагностика ошибок 🛠️",
                "clearlogs" to "Очистить логи 🧹",
                "sync" to "Синхронизация 🔄",
                "Как устроен Орден?" to "Про Орден 🌌",
                "Объясни квантовые компьютеры простыми словами" to "Квантовый ИИ ⚛️",
                "Напиши функцию QuickSort на Kotlin" to "Код Кода 💻",
                "proposal Новые правила | Описание предложений" to "Предложение 📜",
                "dispute agent3 | Клевета в чате | Статья 1" to "Иск ⚖️"
            )
            
            suggestions.forEach { (cmd, label) ->
                Box(
                    modifier = Modifier
                        .background(CosmicGray, shape = RoundedCornerShape(16.dp))
                        .border(1.dp, AccentTeal.copy(0.2f), RoundedCornerShape(16.dp))
                        .clickable {
                            if (cmd == "help" || cmd == "status" || cmd == "sync" || cmd == "diagnostics" || cmd == "clearlogs") {
                                viewModel.executeCliCommand(cmd)
                            } else {
                                cliInputText = cmd
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        color = SoftCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ChatGPT-style pill input text box inside a modern Cosmic card container background
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CosmicGray, shape = RoundedCornerShape(24.dp))
                .border(1.5.dp, AccentTeal.copy(0.4f), RoundedCornerShape(24.dp))
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = cliInputText,
                onValueChange = { cliInputText = it },
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = TextLight,
                    fontSize = 13.sp,
                    letterSpacing = 0.25.sp
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(AccentTeal),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (cliInputText.isEmpty()) {
                            Text(
                                "Спросите ИИ-Агента или введите команду...",
                                color = TextMuted.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp)
                    .testTag("cli_text_input"),
                keyboardOptions = KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (cliInputText.isNotBlank()) {
                            viewModel.executeCliCommand(cliInputText)
                            cliInputText = ""
                        }
                    }
                )
            )

            IconButton(
                onClick = {
                    if (cliInputText.isNotBlank()) {
                        viewModel.executeCliCommand(cliInputText)
                        cliInputText = ""
                    }
                },
                modifier = Modifier.size(32.dp),
                enabled = cliInputText.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Отправить",
                    tint = if (cliInputText.isNotBlank()) AccentTeal else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// INTERACTIVE SYSTEM MIND MAP (TRANSITION GRAPH)
// -------------------------------------------------------------
@Composable
fun MindMapTab(
    viewModel: SuiteViewModel,
    onNavigate: (Int) -> Unit
) {
    var hoveredNode by remember { mutableStateOf(-1) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Elegant Apple/Google style intro banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CosmicGray),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentTeal.copy(0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ИНТЕРАКТИВНЫЙ ГРАФ ПЕРЕХОДОВ",
                            color = AccentTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Карта Системы Social OS",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextLight
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Вся архитектура децентрализованного Ордена распределена по 4 фундаментальным модулям. Нажмите на любой узел графа ниже, чтобы мгновенно перейти в соответствующий рабочий кабинет.",
                        color = TextMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        item {
            // Interactive Graphical Canvas & Layout
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                // Background Bezier Connection Lines
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(start = 30.dp, top = 40.dp, bottom = 40.dp)
                ) {
                    val width = this.size.width
                    val height = this.size.height
                    
                    // Branch line from Center Hub to other items
                    // We draw smooth curves from left edge (x=0) to nodes on the right
                    val path1 = Path().apply {
                        moveTo(0f, 10f)
                        cubicTo(width * 0.25f, 10f, width * 0.2f, height * 0.22f, width * 0.35f, height * 0.22f)
                    }
                    val path2 = Path().apply {
                        moveTo(0f, 10f)
                        cubicTo(width * 0.25f, 10f, width * 0.2f, height * 0.44f, width * 0.35f, height * 0.44f)
                    }
                    val path3 = Path().apply {
                        moveTo(0f, 10f)
                        cubicTo(width * 0.25f, 10f, width * 0.2f, height * 0.68f, width * 0.35f, height * 0.68f)
                    }
                    val path4 = Path().apply {
                        moveTo(0f, 10f)
                        cubicTo(width * 0.25f, 10f, width * 0.2f, height * 0.9f, width * 0.35f, height * 0.9f)
                    }

                    // Base connecting main line down
                    drawLine(
                        color = Color(0xFFCBD5E1),
                        start = androidx.compose.ui.geometry.Offset(0f, 10f),
                        end = androidx.compose.ui.geometry.Offset(0f, height * 0.9f),
                        strokeWidth = 3.dp.toPx()
                    )

                    listOf(path1, path2, path3, path4).forEachIndexed { index, path ->
                        drawPath(
                            path = path,
                            color = if (hoveredNode == index + 1) AccentTeal else Color(0xFFE2E8F0),
                            style = Stroke(
                                width = if (hoveredNode == index + 1) 4.dp.toPx() else 2.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                    }
                    
                    // Draw central connection node point
                    drawCircle(
                        color = AccentTeal,
                        radius = 8.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(0f, 10f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(0f, 10f)
                    )
                }

                // Vertical list representing our interactive branches
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp), // Leaves room for the connection rail on the left
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    
                    // CENTRAL NODE: THE CORE SOCIAL OS
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, bottom = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = CosmicGray),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, AccentTeal)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.6.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AccentTeal.copy(0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Core Kernel",
                                    tint = AccentTeal,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Ядро Social OS P2P",
                                    color = TextLight,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Автономная операционная среда узел-к-узлу",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // BRANCH 1: ARCHITECTURE / IDEOLOGY
                    MindMapNodeCard(
                        title = "🏛️ Идеология & ООП Модель",
                        desc = "Манифест децентрализации, правила («Code is Law») и доменная модель исходного кода ядра.",
                        nodeIndex = 1,
                        buttonLabel = "Классовая Модель & Стек",
                        isHovered = hoveredNode == 1,
                        onHover = { hoveredNode = it },
                        onClick = { onNavigate(1) }
                    )

                    // BRANCH 2: COUNCIL & TASKBOARD
                    MindMapNodeCard(
                        title = "📋 Законотворчество & Управа",
                        desc = "Голосование по репутационным кворумам, создание законов и подтверждение вкладов.",
                        nodeIndex = 2,
                        buttonLabel = "Кабинет Голосования",
                        isHovered = hoveredNode == 2,
                        onHover = { hoveredNode = it },
                        onClick = { onNavigate(2) }
                    )

                    // BRANCH 3: JUDICIARY COURT
                    MindMapNodeCard(
                        title = "⚖️ Братский Суд Присяжных",
                        desc = "Разрешение технических конфликтов и спорных вкладов силами присяжных заседателей.",
                        nodeIndex = 3,
                        buttonLabel = "Зал Судебных Разборов",
                        isHovered = hoveredNode == 3,
                        onHover = { hoveredNode = it },
                        onClick = { onNavigate(3) }
                    )

                    // BRANCH 4: P2P REPOSITORY FORKS & MAIL NETWORK
                    MindMapNodeCard(
                        title = "📡 Карта Сети & Члены Ордена",
                        desc = "Список участников и экземпляров Ордена, защищенный SMTP/IMAP чат и настройки P2P оверлея.",
                        nodeIndex = 4,
                        buttonLabel = "Терминал Связи & Члены",
                        isHovered = hoveredNode == 4,
                        onHover = { hoveredNode = it },
                        onClick = { onNavigate(4) }
                    )
                }
            }
        }
    }
}

@Composable
fun MindMapNodeCard(
    title: String,
    desc: String,
    nodeIndex: Int,
    buttonLabel: String,
    isHovered: Boolean,
    onHover: (Int) -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isHovered) Color(0xFFF8FAFC) else CosmicGray
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (isHovered) 2.dp else 1.dp,
            color = if (isHovered) AccentTeal else Color(0xFFE2E8F0)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = AccentTeal,
                    fontSize = 14.sp
                )
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = AccentTeal.copy(0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                color = TextLight,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isHovered) AccentTeal else AccentTeal.copy(0.08f),
                    contentColor = if (isHovered) Color.White else AccentTeal
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = buttonLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 1: DESCRIPTION & OOP CLASS MODEL MODEL
// -------------------------------------------------------------
@Composable
fun ArchitectureTab(
    viewModel: SuiteViewModel,
    onSwitchTab: (Int) -> Unit
) {
    var subTab by remember { mutableStateOf(0) } // 0: Институты, 1: Классовая Модель, 2: P2P Стек

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onSwitchTab(0) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back to map",
                    tint = AccentTeal
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "← НАЗАД К КАРТЕ ПЕРЕХОДОВ СИСТЕМЫ",
                color = AccentTeal,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onSwitchTab(0) }
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicGray),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "🏛️ Институты",
                    "💻 Классы",
                    "📡 P2P Стек"
                ).forEachIndexed { index, label ->
                    val isSelected = subTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AccentTeal else Color.Transparent)
                            .clickable { subTab = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) SpaceDark else TextLight,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            when (subTab) {
                0 -> {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = CosmicGray),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "🌌 ДОБРО ПОЖАЛОВАТЬ В ОТКРЫТЫЙ ОРДЕН!",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = HighContrastGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Вы зашли в Social OS — децентрализованную цифровую республику со встроенным репутационным суверенитетом. Здесь нет ни одного централизованного сервера: все решения синхронизируются пир-ту-пир прямо поверх электронной почты.",
                                    color = TextLight,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Ниже описаны фундаментальные алгоритмические институты (Code is Law), управляющие нашей децентрализованной средой.",
                                    color = SoftCyan,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "АРХИТЕКТУРНЫЕ ИНСТИТУТЫ (Social OS)",
                            style = MaterialTheme.typography.titleMedium,
                            color = HighContrastGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Text(
                            "Система построена как ядро Linux: компактный набор прозрачных алгоритмических институтов (Code is Law), вокруг которых развертываются прикладные ветки-форки участников.",
                            color = TextLight,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(institutesData) { inst ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CosmicGray),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(inst.icon, contentDescription = inst.title, tint = AccentTeal, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(inst.title, fontWeight = FontWeight.Bold, color = AccentTeal, fontSize = 15.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(inst.desc, color = TextLight, fontSize = 13.sp, lineHeight = 18.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Логика Закона:", color = HighContrastGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text(inst.rule, color = SoftCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
                1 -> {
                    item {
                        Text(
                            "КЛАССОВАЯ МОДЕЛЬ ООП (Чистая Архитектура)",
                            style = MaterialTheme.typography.titleMedium,
                            color = HighContrastGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Text(
                            "Ниже приведен исходный код доменных сущностей и протоколов ядра, отражающих закон прямого репутационного народовластия.",
                            color = TextLight,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(oopModelData) { codeM ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CosmicGray),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(codeM.className, color = AccentTeal, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                                Text(codeM.meta, color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                                Text(
                                    codeM.methods,
                                    color = Color(0xFF0F172A),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .background(Color(0xFFF1F5F9))
                                        .padding(8.dp)
                                        .fillMaxWidth()
                                )
                            }
                        }
                    }
                }
                2 -> {
                    item {
                        Text(
                            "P2P ЯДРО СИНХРОНИЗАЦИИ SMTP / IMAP",
                            style = MaterialTheme.typography.titleMedium,
                            color = HighContrastGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Text(
                            "Для связи вне цензуры Открытый Орден использует зашифрованные SMTP/IMAP сообщения в качестве P2P транспорта. Каждый узел — это автономный почтовый клиент, а роль DNS-маршрутизаторов играет глобальная цепочка email-провайдеров.",
                            color = TextLight,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CosmicGray),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("💡 Стек Рекомендуемых Технологий Android:", fontWeight = FontWeight.Bold, color = AccentTeal, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                val techList = listOf(
                                    "СУБД SQLite/Room с поддержкой Git-лайк версионирования таблиц для безболезненных форков.",
                                    "Kamil (SMTP/IMAP Kotlin Multiplatform) для прямого коннекта к почтовым серверам.",
                                    "Sodium Oxide / Noise Protocol для сквозного шифрования (E2EE) писем.",
                                    "Automerge / Yoko CRDT для бесконфликтного слияния репутационных изменений.",
                                    "Ed25519 (ECDSA) для подписи каждого голоса и действия («Закон защищен подписью»)."
                                )
                                techList.forEach { tech ->
                                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                        Text("• ", color = AccentTeal, fontWeight = FontWeight.Bold)
                                        Text(tech, color = TextLight, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CosmicGray),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("🔄 Конфликтология Межсетевого Обмена:", fontWeight = FontWeight.Bold, color = AccentTeal, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Каждое решение (Proposal) пакуется как JSON, шифруется, подписывается асимметричным Ed25519-ключом узла и высылается как SMTP-конверт.\n\n" +
                                    "При получении (IMAP), узел верифицирует репутационный вес отправителя на момент отправки и накатывает дельту: если голоса перешли Рубикон, локальные триггеры генерируют задачи управления автоматически.",
                                    color = TextLight,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 1: COUNCIL (PROPOSALS) & EXECUTIVE (TASKS)
// -------------------------------------------------------------
@Composable
fun CouncilAndTasksTab(
    viewModel: SuiteViewModel,
    proposals: List<ProposalEntity>,
    tasks: List<TaskEntity>,
    currentAgentId: String,
    selectedForkId: String,
    onBackToMap: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackToMap,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to map",
                        tint = AccentTeal
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "← НАЗАД К КАРТЕ ПЕРЕХОДОВ СИСТЕМЫ",
                    color = AccentTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onBackToMap() }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "СОВЕТ (COUNCIL/PARLIAMENT)",
                    style = MaterialTheme.typography.titleMedium,
                    color = HighContrastGold,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = SpaceDark),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("create_proposal_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Создать", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                "Законодательная палата. Принятие повестки дня репутационным взвешенным голосованием.",
                color = TextLight,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        if (proposals.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Нет активных предложений в этом форке.", color = TextMuted, fontSize = 13.sp)
                }
            }
        }

        items(proposals) { proposal ->
            ProposalItemCard(proposal = proposal, viewModel = viewModel, currentAgentId = currentAgentId)
        }

        // --- SECTION EXECUTIVE / TASKS ---
        item {
            SeparatorTitle("УПРАВА (EXECUTIVE/ADMINISTRATION)")
            Text(
                text = "Событийная модель: если повестка принята Советом, узел автоматически разворачивает задачу (Executive Task). Завершив ее, участник получает меритократическую репу.",
                color = TextLight,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        if (tasks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Управа не содержит активных поручений.", color = TextMuted, fontSize = 12.sp)
                }
            }
        }

        items(tasks) { task ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicGray),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Награда: +${task.reputationReward.toInt()} REP",
                            color = HighContrastGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (task.status == "AUDITED") CyberGreen.copy(0.15f) else SoftCyan.copy(0.15f)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (task.status == "AUDITED") "ВЫПОЛНЕНО & АУДИРОВАНО" else "НА ЭТАПЕ ВЫПОЛНЕНИЯ",
                                color = if (task.status == "AUDITED") CyberGreen else SoftCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(task.title, fontWeight = FontWeight.Bold, color = TextLight, fontSize = 14.sp)
                    Text(task.description, color = TextLight, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))

                    if (task.status != "AUDITED") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.completeTask(task.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = SpaceDark),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.align(Alignment.End).testTag("complete_task_${task.id}")
                        ) {
                            Text("Сделать Вклад (Закрыть задачу)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Освоено агентом: ${task.assignedTo ?: "Неизвестно"}",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp)
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = CosmicGray,
            title = { Text("Новое предложение в Совет", color = AccentTeal) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Название (Закон/Хартия)", color = TextMuted) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight,
                            focusedContainerColor = SpaceDark,
                            unfocusedContainerColor = SpaceDark,
                            focusedIndicatorColor = AccentTeal
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("add_proposal_title")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("Этическое / Содержательное обоснование", color = TextMuted) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight,
                            focusedContainerColor = SpaceDark,
                            unfocusedContainerColor = SpaceDark,
                            focusedIndicatorColor = AccentTeal
                        ),
                        modifier = Modifier.fillMaxWidth().height(100.dp).testTag("add_proposal_desc")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createProposal(newTitle, newDesc)
                        newTitle = ""
                        newDesc = ""
                        showCreateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = SpaceDark),
                    modifier = Modifier.testTag("add_proposal_submit")
                ) {
                    Text("Опубликовать", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Отмена", color = TextMuted)
                }
            }
        )
    }
}

@Composable
fun ProposalItemCard(
    proposal: ProposalEntity,
    viewModel: SuiteViewModel,
    currentAgentId: String
) {
    val agents by viewModel.agents.collectAsState()
    val arguments by remember(proposal.id) { viewModel.getArgumentsForProposal(proposal.id) }.collectAsState(initial = emptyList())
    val votes by remember(proposal.id) { viewModel.getVotesForProposal(proposal.id) }.collectAsState(initial = emptyList())

    val currentAgent = agents.find { it.id == currentAgentId }
    val currentAgentRep = currentAgent?.reputationScore ?: 0.0

    var isExpanded by remember { mutableStateOf(false) }
    var isAddingArgument by remember { mutableStateOf(false) }
    var argText by remember { mutableStateOf("") }
    var argType by remember { mutableStateOf("PRO") } // "PRO", "CON", "SUGGESTION", "OBJECTION"

    var isEditingText by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf(proposal.title) }
    var editedDesc by remember { mutableStateOf(proposal.description) }

    var resolvingArgId by remember { mutableStateOf<String?>(null) }
    var resolutionText by remember { mutableStateOf("") }

    val activeObjections = arguments.filter { it.type == "OBJECTION" && !it.isResolved }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("proposal_card_${proposal.id}"),
        colors = CardDefaults.cardColors(containerColor = CosmicGray),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = when (proposal.status) {
                "ACCEPTED" -> CyberGreen.copy(0.4f)
                "REJECTED" -> AlertRed.copy(0.4f)
                else -> {
                    when (proposal.stage) {
                        "DRAFT" -> TextMuted.copy(0.2f)
                        "DEBATE" -> SoftCyan.copy(0.4f)
                        "VOTING" -> HighContrastGold.copy(0.5f)
                        else -> Color(0xFFE2E8F0)
                    }
                }
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: ID and Stage Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ID: ${proposal.id.uppercase(Locale.ROOT)}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextMuted
                )
                
                // Stage Badge
                val stageLabel = when (proposal.status) {
                    "ACCEPTED" -> "ПРИНЯТО (Law)"
                    "REJECTED" -> "ОТКЛОНЕНО"
                    else -> {
                        when (proposal.stage) {
                            "DRAFT" -> "ЧЕРНОВИК [DRAFT]"
                            "DEBATE" -> "ДЕБАТЫ [DEBATE]"
                            "VOTING" -> "ГОЛОСОВАНИЕ [VOTING]"
                            else -> "АКТИВНО"
                        }
                    }
                }
                val stageColor = when (proposal.status) {
                    "ACCEPTED" -> CyberGreen
                    "REJECTED" -> AlertRed
                    else -> {
                        when (proposal.stage) {
                            "DRAFT" -> TextMuted
                            "DEBATE" -> SoftCyan
                            "VOTING" -> HighContrastGold
                            else -> AccentTeal
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(stageColor.copy(0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = stageLabel,
                        color = stageColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Proposer Subtitle
            val proposerName = agents.find { it.id == proposal.proposerId }?.name ?: proposal.proposerId
            Text(
                text = "Инициатор: $proposerName (${proposal.proposerId})",
                fontSize = 11.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (isEditingText && proposal.status == "ACTIVE" && (proposal.stage == "DRAFT" || proposal.stage == "DEBATE")) {
                // Editing fields
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    label = { Text("Название предложения") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentTeal,
                        unfocusedBorderColor = TextMuted.copy(0.3f),
                        focusedLabelColor = AccentTeal
                    )
                )

                OutlinedTextField(
                    value = editedDesc,
                    onValueChange = { editedDesc = it },
                    label = { Text("Содержание / Свод правил") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentTeal,
                        unfocusedBorderColor = TextMuted.copy(0.3f),
                        focusedLabelColor = AccentTeal
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { isEditingText = false }) {
                        Text("Отмена", color = AlertRed)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.amendProposal(proposal.id, editedTitle, editedDesc)
                            isEditingText = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberGreen)
                    ) {
                        Text("Внести правки")
                    }
                }
            } else {
                // Regular Display
                Text(
                    text = proposal.title,
                    fontWeight = FontWeight.Bold,
                    color = TextLight,
                    fontSize = 15.sp
                )
                Text(
                    text = proposal.description,
                    color = TextLight,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // If proposer is the current user, show edit option
                if (proposal.status == "ACTIVE" && (proposal.stage == "DRAFT" || proposal.stage == "DEBATE") && proposal.proposerId == currentAgentId) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = {
                            editedTitle = proposal.title
                            editedDesc = proposal.description
                            isEditingText = true
                        },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp), tint = AccentTeal)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Эволюционировать текст (внести Акт правки)", fontSize = 11.sp, color = AccentTeal, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quorum Progress Indicator (mainly for VOTING or Completed)
            val totalWeight = proposal.voteYesRep + proposal.voteNoRep
            val yesProgress = if (totalWeight > 0) (proposal.voteYesRep / totalWeight).toFloat() else 0f
            val quorumText = "Накопленный вес: ${totalWeight.toInt()}/${proposal.quorumRequired.toInt()} REP (репутационный кворум)"

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ЗА: ${proposal.voteYesRep.toInt()} REP (${(yesProgress * 100).toInt()}%)",
                        color = CyberGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ПРОТИВ: ${proposal.voteNoRep.toInt()} REP (${((1f - yesProgress) * 100).toInt()}%)",
                        color = AlertRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { yesProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = CyberGreen,
                    trackColor = AlertRed
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = quorumText,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp)
                )
            }

            // Unresolved objections warning if stage is DEBATE or DRAFT
            if (proposal.status == "ACTIVE" && (proposal.stage == "DRAFT" || proposal.stage == "DEBATE")) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeObjections.isNotEmpty()) AlertRed.copy(0.12f) else CyberGreen.copy(0.08f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (activeObjections.isNotEmpty()) Icons.Default.Warning else Icons.Default.Check,
                        contentDescription = null,
                        tint = if (activeObjections.isNotEmpty()) AlertRed else CyberGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (activeObjections.isNotEmpty()) {
                            "Бирюзовое вето: ${activeObjections.size} нерешенных возражений. Перевод в голосование заблокирован."
                        } else {
                            "Консенсус: Блокирующих возражений нет. Предложение готово к вынесению на голосование."
                        },
                        fontSize = 11.sp,
                        color = if (activeObjections.isNotEmpty()) AlertRed else CyberGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Expandable section for debate arguments, list of votes, and stage advancement actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expanding Toggle Button
                Row(
                    modifier = Modifier
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpanded) "СВЕРНУТЬ ПОДРОБНОСТИ" else "ОБСУЖДЕНИЕ И СОВЕЩАНИЕ (${arguments.size} мнений)",
                        fontSize = 11.sp,
                        color = AccentTeal,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = AccentTeal,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // If stage is VOTING and proposal is ACTIVE, show immediate vote controls here too or when expanded
                if (proposal.status == "ACTIVE" && proposal.stage == "VOTING") {
                    Row {
                        Button(
                            onClick = { viewModel.castVote(proposal.id, true) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGreen.copy(0.15f), contentColor = CyberGreen),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.padding(end = 4.dp).height(28.dp).testTag("vote_yes_${proposal.id}")
                        ) {
                            Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("ЗА", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.castVote(proposal.id, false) },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed.copy(0.15f), contentColor = AlertRed),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp).testTag("vote_no_${proposal.id}")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("ПРОТИВ", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = TextMuted.copy(0.15f), modifier = Modifier.padding(bottom = 10.dp))

                    // 1. Stage Advancement & Admin Controls (Sybil validation: rep >= 30.0 for stage changes)
                    if (proposal.status == "ACTIVE") {
                        Text(
                            text = "ЭВОЛЮЦИЯ СТАТУСА (REP >= 30.0)",
                            style = MaterialTheme.typography.bodySmall.copy(color = HighContrastGold, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isModerator = currentAgentRep >= 30.0

                            if (proposal.stage == "DRAFT") {
                                Button(
                                    onClick = { viewModel.advanceProposalStage(proposal.id, "DEBATE") },
                                    enabled = isModerator,
                                    colors = ButtonDefaults.buttonColors(containerColor = SoftCyan),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).height(32.dp)
                                ) {
                                    Text("Начать дебаты [DEBATE]", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (proposal.stage == "DEBATE") {
                                val hasNoObjections = activeObjections.isEmpty()
                                Button(
                                    onClick = { viewModel.advanceProposalStage(proposal.id, "VOTING") },
                                    enabled = isModerator && hasNoObjections,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = HighContrastGold,
                                        disabledContainerColor = TextMuted.copy(0.2f)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).height(32.dp)
                                ) {
                                    Text("Запустить голосование [VOTING]", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        if (currentAgentRep < 30.0) {
                            Text(
                                "Ваш статус репутации (${currentAgentRep}) недостаточен для перевода фаз по регламенту (Требуется 30.0+ REP).",
                                color = TextMuted,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }

                    // 2. Arguments and Objections Feed
                    Text(
                        text = "СОВЕЩАТЕЛЬНЫЙ ФЛУД / ЖУРНАЛ КРИТИКИ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextLight,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    if (arguments.isEmpty()) {
                        Text(
                            text = "Замечаний, рекомендаций и вето пока не высказано. Поделитесь вашим советом согласно принципу Advice Process.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    } else {
                        arguments.forEach { arg ->
                            val isObjection = arg.type == "OBJECTION"
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isObjection) {
                                        if (arg.isResolved) CyberGreen.copy(0.04f) else AlertRed.copy(0.05f)
                                    } else CosmicGray
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isObjection) {
                                        if (arg.isResolved) CyberGreen.copy(0.2f) else AlertRed.copy(0.2f)
                                    } else TextMuted.copy(0.12f)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        when (arg.type) {
                                                            "PRO" -> CyberGreen.copy(0.15f)
                                                            "CON" -> AlertRed.copy(0.15f)
                                                            "SUGGESTION" -> SoftCyan.copy(0.15f)
                                                            else -> HighContrastGold.copy(0.15f)
                                                        }
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = when (arg.type) {
                                                        "PRO" -> "ЗА"
                                                        "CON" -> "ПРОТИВ"
                                                        "SUGGESTION" -> "СОВЕТ"
                                                        else -> "КОНСЕНСУС-ВЕТО"
                                                    },
                                                    color = when (arg.type) {
                                                        "PRO" -> CyberGreen
                                                        "CON" -> AlertRed
                                                        "SUGGESTION" -> SoftCyan
                                                        else -> HighContrastGold
                                                    },
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = arg.authorName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = TextLight
                                            )
                                        }

                                        Text(
                                            text = if (isObjection) {
                                                if (arg.isResolved) "ИНТЕГРИРОВАНО" else "АКТИВНОЕ ВЕТО"
                                            } else "",
                                            color = if (arg.isResolved) CyberGreen else AlertRed,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = arg.text, color = TextLight, fontSize = 12.sp)

                                    if (arg.isResolved) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(CyberGreen.copy(0.06f))
                                                .padding(6.dp)
                                        ) {
                                            Text(
                                                text = "Включение автора: ${arg.resolutionText}",
                                                color = CyberGreen,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    // If current user is proposer and objection is active, show Resolve Objection fields
                                    if (isObjection && !arg.isResolved && proposal.proposerId == currentAgentId && proposal.status == "ACTIVE") {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        if (resolvingArgId == arg.id) {
                                            OutlinedTextField(
                                                value = resolutionText,
                                                onValueChange = { resolutionText = it },
                                                label = { Text("Как вы изменили закон под это возражение?") },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = CyberGreen,
                                                    unfocusedBorderColor = TextMuted.copy(0.3f)
                                                )
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                TextButton(onClick = { resolvingArgId = null }) {
                                                    Text("Отмена", color = AlertRed, fontSize = 11.sp)
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Button(
                                                    onClick = {
                                                        viewModel.resolveObjection(proposal.id, arg.id, resolutionText)
                                                        resolvingArgId = null
                                                        resolutionText = ""
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = CyberGreen),
                                                    modifier = Modifier.height(28.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                                ) {
                                                    Text("Завершить интеграцию", fontSize = 10.sp)
                                                }
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    resolvingArgId = arg.id
                                                    resolutionText = ""
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = CyberGreen.copy(0.1f), contentColor = CyberGreen),
                                                modifier = Modifier.align(Alignment.End).height(26.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                Text("Разрешить возражение / Снять вето", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Form to Add Argument or Objection (Advice process)
                    if (proposal.status == "ACTIVE") {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isAddingArgument) {
                            Text(
                                "Добавить отзыв в совет (Требуется репутация >= 15.0)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextLight,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            // Argument Types Row selector
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("PRO", "CON", "SUGGESTION", "OBJECTION").forEach { type ->
                                    val isSelected = argType == type
                                    val label = when (type) {
                                        "PRO" -> "ЗА"
                                        "CON" -> "ПРОТИВ"
                                        "SUGGESTION" -> "СОВЕТ"
                                        else -> "ЗАМЕЧАНИЕ"
                                    }
                                    val activeColor = when (type) {
                                        "PRO" -> CyberGreen
                                        "CON" -> AlertRed
                                        "SUGGESTION" -> SoftCyan
                                        else -> HighContrastGold
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) activeColor else activeColor.copy(0.1f))
                                            .clickable { argType = type }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) CosmicGray else activeColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (argType == "OBJECTION") {
                                Text(
                                    "Внимание: Вы выставляете блокирующее вето. Данный закон не сможет уйти на голосование, пока автор не проведет медиацию и не интегрирует ваши правки.",
                                    color = HighContrastGold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }

                            OutlinedTextField(
                                value = argText,
                                onValueChange = { argText = it },
                                placeholder = { Text("Обоснуйте свое решение...", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentTeal,
                                    unfocusedBorderColor = TextMuted.copy(0.3f)
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { isAddingArgument = false }) {
                                    Text("Отмена", color = AlertRed)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        viewModel.addProposalArgument(proposal.id, argText, argType)
                                        argText = ""
                                        isAddingArgument = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                                    enabled = currentAgentRep >= 15.0
                                ) {
                                    Text("Опубликовать совет")
                                }
                            }

                            if (currentAgentRep < 15.0) {
                                Text(
                                    "Сибил-защита активна! Высказать суждение могут только участники ордена с рейтингом >= 15.0 REP. Ваша репа: ${currentAgentRep}",
                                    color = AlertRed,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        } else {
                            Button(
                                onClick = { isAddingArgument = true },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal.copy(0.12f), contentColor = AccentTeal),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Высказать суждение / Оформить возражение [TEAL ADVICE]", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 4. Stance Breakdown (Who voted what)
                    if (votes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "РАСПРЕДЕЛЕНИЕ ГОЛОСОВ УЧАСТНИКОВ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        votes.forEach { vote ->
                            val voterName = agents.find { it.id == vote.voterId }?.name ?: vote.voterId
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$voterName (${vote.voterId}) [${vote.voterReputation.toInt()} REP]",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (vote.votedYes) "ЗА" else "ПРОТИВ",
                                    color = if (vote.votedYes) CyberGreen else AlertRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: JUDICIARY / ARBITRATION (СУД & СПОРЫ)
// -------------------------------------------------------------
@Composable
fun JudiciaryTab(
    viewModel: SuiteViewModel,
    disputes: List<DisputeEntity>,
    agents: List<AgentEntity>,
    currentAgentId: String,
    onBackToMap: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedDefendant by remember { mutableStateOf("") }
    var lawsuitDesc by remember { mutableStateOf("") }
    var selectedArticle by remember { mutableStateOf("Раздел I. Отрез 1 (Национальный суверенитет)") }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackToMap,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to map",
                        tint = AccentTeal
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "← НАЗАД К КАРТЕ ПЕРЕХОДОВ СИСТЕМЫ",
                    color = AccentTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onBackToMap() }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "СУД / АРБИТРАЖ",
                    style = MaterialTheme.typography.titleMedium,
                    color = HighContrastGold,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { 
                        selectedDefendant = agents.firstOrNull { it.id != currentAgentId }?.id ?: ""
                        showCreateDialog = true 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = SpaceDark),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("raise_dispute_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Заявить иск", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                "Судебный надзор на основе этического кодекса. Присяжные созываются случайно из числа пиров с высокой репутацией.",
                color = TextLight,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        if (disputes.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    Text("Конституционная палата чиста от споров.", color = TextMuted)
                }
            }
        }

        items(disputes) { dispute ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicGray),
                border = BorderStroke(
                    1.dp,
                    if (dispute.status == "RESOLVED_YES") AlertRed.copy(0.4f) else Color(0xFFE2E8F0)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "КОДЕКС: ${dispute.constitutionalArticle}",
                            color = HighContrastGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when (dispute.status) {
                                        "RESOLVED_YES" -> AlertRed.copy(0.15f)
                                        "RESOLVED_NO" -> CyberGreen.copy(0.15f)
                                        else -> AccentTeal.copy(0.15f)
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = when (dispute.status) {
                                    "RESOLVED_YES" -> "ВЕРДИКТ: ВИНОВЕН"
                                    "RESOLVED_NO" -> "ВЕРДИКТ: ОПРАВДАН"
                                    else -> "НА РАССМОТРЕНИИ"
                                },
                                color = when (dispute.status) {
                                    "RESOLVED_YES" -> AlertRed
                                    "RESOLVED_NO" -> CyberGreen
                                    else -> AccentTeal
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Истец: ${dispute.plaintiffId}  ->  Обвиняемый: ${dispute.defendantId}",
                        color = SoftCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = dispute.description,
                        color = TextLight,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Судейская Коллегия (Jury IDs): [${dispute.juryIds.replace(",", ", ")}]",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Решение суда: ${dispute.verdict}",
                        color = if (dispute.status == "RESOLVED_YES") AlertRed else SoftCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    val juryList = dispute.juryIds.split(",")
                    val isJuror = juryList.contains(currentAgentId)

                    if (dispute.status == "REVIEW" && isJuror) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Вы назначены присяжным:",
                                color = HighContrastGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row {
                                Button(
                                    onClick = { viewModel.castJuryVote(dispute.id, true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed.copy(0.12f), contentColor = AlertRed),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.padding(end = 4.dp).testTag("jury_guilty_${dispute.id}")
                                ) {
                                    Text("ВИНОВЕН", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { viewModel.castJuryVote(dispute.id, false) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberGreen.copy(0.12f), contentColor = CyberGreen),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("jury_innocent_${dispute.id}")
                                ) {
                                    Text("ОПРАВДАН", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = CosmicGray,
            title = { Text("Заявление Конституционного Иска", color = AccentTeal) },
            text = {
                Column {
                    Text("Выберите обвиняемого пира:", color = TextMuted, fontSize = 12.sp)
                    var expandedDropdown by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(
                            text = "$selectedDefendant ▾",
                            color = AccentTeal,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable { expandedDropdown = true }
                                .background(SpaceDark)
                                .padding(10.dp)
                                .fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false },
                            modifier = Modifier.background(CosmicGray)
                        ) {
                            agents.filter { it.id != currentAgentId }.forEach { agent ->
                                DropdownMenuItem(
                                    text = { Text(agent.id, color = TextLight) },
                                    onClick = {
                                        selectedDefendant = agent.id
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("Категория (Нарушенная статья):", color = TextMuted, fontSize = 12.sp)
                    var expandedArticles by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(
                            text = "$selectedArticle ▾",
                            color = AccentTeal,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable { expandedArticles = true }
                                .background(SpaceDark)
                                .padding(10.dp)
                                .fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = expandedArticles,
                            onDismissRequest = { expandedArticles = false },
                            modifier = Modifier.background(CosmicGray)
                        ) {
                            listOf(
                                "Раздел I. Отрез 1 (Конституционная неприкосновенность)",
                                "Раздел II. Статья 4 (Добросовестный вклад)",
                                "Раздел III. Параграф 1 (Защита от Сибилл-вмешательства)",
                                "Раздел V. Закон 12 (Идеологическая солидарность)"
                            ).forEach { art ->
                                DropdownMenuItem(
                                    text = { Text(art, color = TextLight) },
                                    onClick = {
                                        selectedArticle = art
                                        expandedArticles = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = lawsuitDesc,
                        onValueChange = { lawsuitDesc = it },
                        label = { Text("Описание инцидента и улик", color = TextMuted) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight,
                            focusedContainerColor = SpaceDark,
                            unfocusedContainerColor = SpaceDark,
                            focusedIndicatorColor = AccentTeal
                        ),
                        modifier = Modifier.fillMaxWidth().height(80.dp).testTag("dispute_desc_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.raiseDispute(selectedDefendant, lawsuitDesc, selectedArticle)
                        lawsuitDesc = ""
                        showCreateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = SpaceDark),
                    modifier = Modifier.testTag("submit_dispute_btn")
                ) {
                    Text("Заявить Коллегии", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Отмена", color = TextMuted)
                }
            }
        )
    }
}

// -------------------------------------------------------------
// TAB 3: REPOSITORY / FORKS & DETAILED SMTP NETWORK SYNC LOGS
// -------------------------------------------------------------
@Composable
fun RepositoryForksTab(
    viewModel: SuiteViewModel,
    forks: List<ForkEntity>,
    syncLogs: List<SyncLogEntity>,
    selectedForkId: String,
    agents: List<AgentEntity>,
    onBackToMap: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var forkIdInput by remember { mutableStateOf("") }
    var forkTitleInput by remember { mutableStateOf("") }
    var forkDescInput by remember { mutableStateOf("") }
    var quorumMult by remember { mutableStateOf(1.0f) }
    var minRepThreshold by remember { mutableStateOf(10.0f) }

    // Participant invite / import state
    var peerShareInput by remember { mutableStateOf("") }
    var importedFeedback by remember { mutableStateOf("") }

    // Chat state hoisted up
    var chatInputText by remember { mutableStateOf("") }
    var selectedRecipientId by remember { mutableStateOf("broadcast") }
    var showRecipientDropdown by remember { mutableStateOf(false) }

    val activeSmtpHost by viewModel.smtpHost.collectAsState()
    val activeSmtpPort by viewModel.smtpPort.collectAsState()
    val activeImapHost by viewModel.imapHost.collectAsState()
    val activeImapPort by viewModel.imapPort.collectAsState()
    val activeMailUser by viewModel.mailUser.collectAsState()
    val activeMailPass by viewModel.mailPass.collectAsState()
    val activeUseSsl by viewModel.useSsl.collectAsState()

    val chatMessages by viewModel.chatMessages.collectAsState()
    val currentAgentId by viewModel.currentAgentId.collectAsState()
    val isSimulationEnabled by viewModel.isSimulationEnabled.collectAsState()

    var smtpHostInput by remember(activeSmtpHost) { mutableStateOf(activeSmtpHost) }
    var smtpPortInput by remember(activeSmtpPort) { mutableStateOf(activeSmtpPort.toString()) }
    var imapHostInput by remember(activeImapHost) { mutableStateOf(activeImapHost) }
    var imapPortInput by remember(activeImapPort) { mutableStateOf(activeImapPort.toString()) }
    var mailUserInput by remember(activeMailUser) { mutableStateOf(activeMailUser) }
    var mailPassInput by remember(activeMailPass) { mutableStateOf(activeMailPass) }
    var useSslInput by remember(activeUseSsl) { mutableStateOf(activeUseSsl) }

    val context = androidx.compose.ui.platform.LocalContext.current

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackToMap,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to map",
                        tint = AccentTeal
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "← НАЗАД К КАРТЕ ПЕРЕХОДОВ СИСТЕМЫ",
                    color = AccentTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onBackToMap() }
                )
            }
        }

        // --- SECTION 1: NETWORK OVERVIEW & STATS ---
        item {
            SeparatorTitle("СТАТУС И УЧАСТНИКИ P2P СЕТИ (NETWORK OVERVIEW)")
            
            val totalAgents = agents.size
            val onlineCount = agents.count {
                it.id == currentAgentId || (isSimulationEnabled && (it.id == "alex@orden.p2p" || it.id == "maria@orden.p2p"))
            }
            val currentAgentName = agents.find { it.id == currentAgentId }?.name ?: "Неизвестен"

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = CosmicGray),
                    border = BorderStroke(1.dp, TextMuted.copy(0.2f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ЧЛЕНОВ ОРДЕНА", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("$totalAgents", fontSize = 18.sp, color = AccentTeal, fontWeight = FontWeight.Bold)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = CosmicGray),
                    border = BorderStroke(1.dp, TextMuted.copy(0.2f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("В СЕТИ (АКТИВНЫХ)", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("$onlineCount", fontSize = 18.sp, color = CyberGreen, fontWeight = FontWeight.Bold)
                    }
                }
                Card(
                    modifier = Modifier.weight(1.2f),
                    colors = CardDefaults.cardColors(containerColor = CosmicGray),
                    border = BorderStroke(1.dp, TextMuted.copy(0.2f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ТЕКУЩИЙ УЗЕЛ (ВЫ)", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(currentAgentName, fontSize = 13.sp, color = HighContrastGold, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }

        // --- SECTION 2: NODES / INSTANCES DIRECTORY ---
        item {
            Text(
                text = "АКТИВНЫЕ ЭКЗЕМПЛЯРЫ ПРИЛОЖЕНИЯ В СЕТЕВОМ ОКРУЖЕНИИ:",
                color = HighContrastGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }

        items(agents) { agent ->
            val isMe = agent.id == currentAgentId
            val isOnline = isMe || (isSimulationEnabled && (agent.id == "alex@orden.p2p" || agent.id == "maria@orden.p2p"))
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicGray),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isMe) CyberGreen.copy(0.5f) else TextMuted.copy(0.2f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(if (isOnline) CyberGreen else TextMuted.copy(0.5f), shape = RoundedCornerShape(5.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = agent.name,
                                    fontWeight = FontWeight.Bold,
                                    color = TextLight,
                                    fontSize = 13.sp
                                )
                                if (isMe) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "(Вы / Хост)",
                                        fontSize = 10.sp,
                                        color = CyberGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Text(
                                text = "Узел ID (Node address): ${agent.id}",
                                fontSize = 11.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "Роль: ${agent.role}  •  Репутация: ",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                                Text(
                                    text = "${agent.reputationScore.toInt()} REP",
                                    fontSize = 10.sp,
                                    color = HighContrastGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "  •  Вкладов: ${agent.contributionsCount}",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                            Text(
                                text = "Sign Key: ${agent.publicKey.take(16)}...",
                                fontSize = 10.sp,
                                color = TextMuted.copy(0.7f),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    
                    Button(
                        onClick = {
                            selectedRecipientId = agent.id
                            chatInputText = "Привет, ${agent.name}! Пишу из своей ноды социальной ОС."
                            android.widget.Toast.makeText(context, "Получатель изменен: ${agent.name}", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOnline) CyberGreen.copy(0.12f) else AccentTeal.copy(0.1f),
                            contentColor = if (isOnline) CyberGreen else AccentTeal
                        ),
                        border = BorderStroke(1.dp, if (isOnline) CyberGreen.copy(0.5f) else AccentTeal.copy(0.3f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("contact_agent_${agent.id.replace("@", "_")}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "Message agent",
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Написать", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicGray),
                border = BorderStroke(1.dp, HighContrastGold.copy(0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "РЕКРУТИНГ И ПРИГЛАШЕНИЯ (P2P ONBOARDING)",
                        color = HighContrastGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Отправьте инвайт новому участнику или вставьте его инвайт ниже для занесения в защищенную СУБД узла.",
                        color = TextLight,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Import row
                    OutlinedTextField(
                        value = peerShareInput,
                        onValueChange = { peerShareInput = it },
                        label = { Text("Код приглашения участника", color = TextMuted) },
                        placeholder = { Text("Вставьте код формата RECRUIT_JOIN#...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight,
                            focusedBorderColor = AccentTeal,
                            unfocusedBorderColor = TextMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("invite_importer_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val text = peerShareInput.trim()
                                if (text.startsWith("RECRUIT_JOIN#")) {
                                    try {
                                        val parts = text.removePrefix("RECRUIT_JOIN#").split("|")
                                        if (parts.size >= 4) {
                                            val id = parts[0]
                                            val name = parts[1]
                                            val role = parts[2]
                                            val rep = parts[3].toDoubleOrNull() ?: 20.0
                                            val key = if (parts.size > 4) parts[4] else ""
                                            viewModel.registerAgent(id, name, role, rep, key, autoLogin = false)
                                            importedFeedback = "Успешно импортирован участник: $name ($id)!"
                                            peerShareInput = ""
                                        } else {
                                            importedFeedback = "Ошибка разбора: неверный формат полей"
                                        }
                                    } catch (e: Exception) {
                                        importedFeedback = "Ошибка импорта: ${e.message}"
                                    }
                                } else {
                                    importedFeedback = "Ошибка: код должен начинаться с RECRUIT_JOIN#"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = SpaceDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("import_peer_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Внести пира", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val currentAgent = agents.find { it.id == viewModel.currentAgentId.value }
                                if (currentAgent != null) {
                                    val inviteCode = "RECRUIT_JOIN#${currentAgent.id}|${currentAgent.name}|${currentAgent.role}|${currentAgent.reputationScore}|${currentAgent.publicKey}"
                                    try {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("OpenOrdenInvite", inviteCode)
                                        clipboard.setPrimaryClip(clip)
                                        importedFeedback = "Ваш инвайт-код скопирован в буфер! Передайте его новому участнику боевой системы."
                                    } catch (e: Exception) {
                                        importedFeedback = "Ошибка буфера: ${e.message}. Код: $inviteCode"
                                    }
                                } else {
                                    importedFeedback = "Сначала выберите или зарегистрируйте свой аккаунт!"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicGray, contentColor = HighContrastGold),
                            border = BorderStroke(1.dp, HighContrastGold.copy(0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("copy_my_invite_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = HighContrastGold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Поделиться собой", fontSize = 11.sp, color = HighContrastGold)
                        }
                    }

                    if (importedFeedback.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = importedFeedback,
                            color = CyberGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item {
            var showSettings by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicGray),
                border = BorderStroke(1.dp, AccentTeal.copy(0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "НАСТРОЙКИ РЕАЛЬНОГО P2P SMTP / IMAP КАНАЛА",
                                color = AccentTeal,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        TextButton(
                            onClick = { showSettings = !showSettings },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = if (showSettings) "Скрыть" else "Настроить",
                                color = HighContrastGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (showSettings) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSimulationEnabled) Color.Transparent else CyberGreen.copy(alpha = 0.08f))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Режим симуляции сети (Fallback Sandbox)",
                                    color = if (isSimulationEnabled) HighContrastGold else CyberGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Text(
                                    if (isSimulationEnabled) 
                                        "Включен автогенератор демо-событий при ошибках или полупустых полях."
                                    else 
                                        "Только РЕАЛЬНОЕ сетевое SMTP/IMAP взаимодействие без подмешивания демо-данных.",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                            }
                            Switch(
                                checked = isSimulationEnabled,
                                onCheckedChange = { viewModel.setSimulationEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = SpaceDark,
                                    checkedTrackColor = HighContrastGold,
                                    uncheckedThumbColor = SpaceDark,
                                    uncheckedTrackColor = CosmicGray
                                ),
                                modifier = Modifier.testTag("simulation_toggle_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Для связи вне цензуры на реальных SMTP/IMAP серверах, укажите параметры своего почтового ящика ниже. Рекомендуется использовать специальный пароль приложения (App Password).",
                            color = TextLight,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Host Row SMTP
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = smtpHostInput,
                                onValueChange = { smtpHostInput = it },
                                label = { Text("SMTP Хост", color = TextMuted) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextLight,
                                    unfocusedTextColor = TextLight,
                                    focusedBorderColor = AccentTeal,
                                    unfocusedBorderColor = TextMuted
                                ),
                                modifier = Modifier.weight(2f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = smtpPortInput,
                                onValueChange = { smtpPortInput = it },
                                label = { Text("Порт", color = TextMuted) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextLight,
                                    unfocusedTextColor = TextLight,
                                    focusedBorderColor = AccentTeal,
                                    unfocusedBorderColor = TextMuted
                                ),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Host Row IMAP
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = imapHostInput,
                                onValueChange = { imapHostInput = it },
                                label = { Text("IMAP Хост", color = TextMuted) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextLight,
                                    unfocusedTextColor = TextLight,
                                    focusedBorderColor = AccentTeal,
                                    unfocusedBorderColor = TextMuted
                                ),
                                modifier = Modifier.weight(2f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = imapPortInput,
                                onValueChange = { imapPortInput = it },
                                label = { Text("Порт", color = TextMuted) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextLight,
                                    unfocusedTextColor = TextLight,
                                    focusedBorderColor = AccentTeal,
                                    unfocusedBorderColor = TextMuted
                                ),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Credentials
                        OutlinedTextField(
                            value = mailUserInput,
                            onValueChange = { mailUserInput = it },
                            label = { Text("Email (Адрес ноды)", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                focusedBorderColor = AccentTeal,
                                unfocusedBorderColor = TextMuted
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = mailPassInput,
                            onValueChange = { mailPassInput = it },
                            label = { Text("Пароль / Пароль приложения", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                focusedBorderColor = AccentTeal,
                                unfocusedBorderColor = TextMuted
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // SSL use checkbox
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = useSslInput,
                                onCheckedChange = { useSslInput = it },
                                colors = CheckboxDefaults.colors(checkedColor = AccentTeal)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Использовать SSL/TLS шифрование транспортного уровня", color = TextLight, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    viewModel.updateMailSettings(
                                        smtpH = smtpHostInput.trim(),
                                        smtpP = smtpPortInput.trim().toIntOrNull() ?: 465,
                                        imapH = imapHostInput.trim(),
                                        imapP = imapPortInput.trim().toIntOrNull() ?: 993,
                                        usr = mailUserInput.trim(),
                                        pas = mailPassInput,
                                        ssl = useSslInput
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = SpaceDark),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Сохранить", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    viewModel.testMailConnection()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CosmicGray, contentColor = HighContrastGold),
                                border = BorderStroke(1.dp, HighContrastGold.copy(0.5f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Проверить линк", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighContrastGold)
                            }
                        }
                    }
                }
            }
        }

        item {
            var showForksPanel by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicGray),
                border = BorderStroke(1.dp, HighContrastGold.copy(0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = HighContrastGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "УПРАВЛЕНИЕ ФОРКАМИ ЯДРА (CORE DATABASE FORKS)",
                                color = HighContrastGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        TextButton(
                            onClick = { showForksPanel = !showForksPanel },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = if (showForksPanel) "Скрыть" else "Развернуть",
                                color = AccentTeal,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (showForksPanel) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Форк базы данных используется только если часть участников Ордена желает изменить базовую Конституцию Ордена и запустить свою параллельную повестку. Все остальные пиры продолжают мирное существование в исходной цепочке законов.",
                            color = TextLight,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "СПИСОК ЛОКАЛЬНЫХ ФОРКОВ И КОНСТИТУЦИЙ:",
                                color = HighContrastGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = SpaceDark),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp).testTag("create_fork_btn_inner")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Создать форк", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        forks.forEach { fork ->
                            val isSelected = fork.id == selectedForkId
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { viewModel.selectFork(fork.id) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) AccentTeal.copy(0.1f) else SpaceDark
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) AccentTeal else CosmicGray
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = fork.title,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) AccentTeal else TextLight,
                                            fontSize = 13.sp
                                        )
                                        if (fork.parentForkId != null) {
                                            Text(
                                                "Родитель: [${fork.parentForkId}]",
                                                color = SoftCyan,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        } else {
                                            Text("Базовый куратор", color = CyberGreen, fontSize = 9.sp)
                                        }
                                    }
                                    Text(fork.description, color = TextLight, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Множитель кворума: x${fork.votingQuorumMultiplier}",
                                            color = TextMuted,
                                            fontSize = 9.sp
                                        )
                                        Text(
                                            "Порог автора: ${fork.minReputationToPropose.toInt()} REP",
                                            color = TextMuted,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SeparatorTitle("ЗАШИФРОВАННЫЙ P2P ЧАТ (SECURE CHAT OVER SMTP)")
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicGray),
                border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Децентрализованный P2P чат. Каждое сообщение шифруется, подписывается вашей ЭЦП и отправляется через SMTP почтовый сервер получателям.",
                        color = TextLight,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Сетка сообщений
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = SpaceDark),
                        border = BorderStroke(1.dp, CosmicGray)
                    ) {
                        if (chatMessages.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Сообщений нет. Выберите собеседника ниже и отправьте первое подписанное P2P-сообщение!",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val latestMessages = chatMessages.takeLast(5)
                                latestMessages.forEach { msg ->
                                    val isMe = msg.senderId == currentAgentId
                                    val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                    val timeStr = format.format(Date(msg.timestamp))
                                    
                                    val senderInitials = msg.senderId.substringBefore("@").take(2).uppercase()

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        if (!isMe) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(HighContrastGold, shape = RoundedCornerShape(12.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    senderInitials,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SpaceDark
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }

                                        Column(
                                            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Text(
                                                text = if (isMe) "Вы" else msg.senderId,
                                                color = if (isMe) CyberGreen else HighContrastGold,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Card(
                                                shape = RoundedCornerShape(8.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isMe) CyberGreen.copy(0.12f) else CosmicGray
                                                ),
                                                border = BorderStroke(1.dp, if (isMe) CyberGreen.copy(0.4f) else CosmicGray.copy(0.4f)),
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Text(
                                                        text = msg.messageText,
                                                        color = TextLight,
                                                        fontSize = 12.sp
                                                    )
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Email,
                                                            contentDescription = null,
                                                            tint = TextMuted,
                                                            modifier = Modifier.size(8.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text(
                                                            "SMTP P2P • $timeStr",
                                                            color = TextMuted,
                                                            fontSize = 8.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (isMe) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(CyberGreen, shape = RoundedCornerShape(12.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    senderInitials,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SpaceDark
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Выбор получателя & Кнопка/Поле ввода
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Выбор получателя
                        Box {
                            Button(
                                onClick = { showRecipientDropdown = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CosmicGray, contentColor = TextLight),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.3f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (selectedRecipientId == "broadcast") "📢 Всем" else selectedRecipientId.substringBefore("@"),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showRecipientDropdown,
                                onDismissRequest = { showRecipientDropdown = false },
                                modifier = Modifier.background(CosmicGray)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("📢 В общий канал (broadcast)", color = TextLight, fontSize = 12.sp) },
                                    onClick = {
                                        selectedRecipientId = "broadcast"
                                        showRecipientDropdown = false
                                    }
                                )
                                agents.filter { it.id != currentAgentId }.forEach { agent ->
                                    DropdownMenuItem(
                                        text = { Text("👤 ${agent.name} (${agent.id.substringBefore("@")})", color = TextLight, fontSize = 12.sp) },
                                        onClick = {
                                            selectedRecipientId = agent.id
                                            showRecipientDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // Текстовое поле ввода
                        OutlinedTextField(
                            value = chatInputText,
                            onValueChange = { chatInputText = it },
                            placeholder = { Text("Текст зашифрованного P2P...", fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberGreen,
                                focusedLabelColor = CyberGreen,
                                unfocusedBorderColor = TextMuted.copy(0.4f),
                                unfocusedLabelColor = TextMuted,
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("chat_input_field")
                        )

                        // Кнопка отправить
                        Button(
                            onClick = {
                                if (chatInputText.isNotBlank()) {
                                    viewModel.sendChatMessage(selectedRecipientId, chatInputText)
                                    chatInputText = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = SpaceDark),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("send_chat_msg_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Отправить P2P сообщение",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            SeparatorTitle("СИНХРОНИЗАЦИЯ P2P (SMTP / IMAP ОВЕРЛЕЙ)")
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Каждое действие асинхронно упаковывается в email-конверты, шифруется, подписывается и вещается пирам по свободным SMTP реле.",
                    color = TextLight,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { viewModel.triggerSync() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = SpaceDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(start = 8.dp).testTag("simulate_sync_action")
                ) {
                    Text("Синхронизировать", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (syncLogs.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    Text("Сеть молчит. Выполните действия в Сандбоксе для генерации писем.", color = TextMuted, fontSize = 11.sp)
                }
            }
        }

        items(syncLogs) { log ->
            val isSmtp = log.direction == "SMTP_SEND"
            val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val timeStr = format.format(Date(log.timestamp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                border = BorderStroke(1.dp, CosmicGray)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isSmtp) Icons.Default.Send else Icons.Default.Email,
                                contentDescription = null,
                                tint = if (isSmtp) AccentTeal else CyberGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSmtp) "P2P TRANSMIT (SMTP SEND)" else "P2P CONSUME (IMAP RECEIVE)",
                                color = if (isSmtp) AccentTeal else CyberGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(timeStr, color = TextMuted, fontSize = 10.sp)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "HASH: ${log.messageHash}  •  STATUS: ${log.statusUrl}",
                        color = HighContrastGold,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = log.mailEnvelope,
                        color = TextLight,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .background(SpaceDark)
                            .padding(6.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = CosmicGray,
            title = { Text("Создать Форк Законов (Версию Системы)", color = AccentTeal) },
            text = {
                Column {
                    OutlinedTextField(
                        value = forkIdInput,
                        onValueChange = { forkIdInput = it },
                        label = { Text("Идентификатор (e.g. libertarian-v2)", color = TextMuted) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight,
                            focusedContainerColor = SpaceDark,
                            unfocusedContainerColor = SpaceDark,
                            focusedIndicatorColor = AccentTeal
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("fork_id_input")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = forkTitleInput,
                        onValueChange = { forkTitleInput = it },
                        label = { Text("Название Концепта (Плана)", color = TextMuted) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight,
                            focusedContainerColor = SpaceDark,
                            unfocusedContainerColor = SpaceDark,
                            focusedIndicatorColor = AccentTeal
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("fork_title_input")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = forkDescInput,
                        onValueChange = { forkDescInput = it },
                        label = { Text("Законодательное отличие от Ядра", color = TextMuted) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight,
                            focusedContainerColor = SpaceDark,
                            unfocusedContainerColor = SpaceDark,
                            focusedIndicatorColor = AccentTeal
                        ),
                        modifier = Modifier.fillMaxWidth().height(60.dp).testTag("fork_desc_input")
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Text("Множитель Кворума: x${String.format(java.util.Locale.US, "%.1f", quorumMult)}", color = TextLight, fontSize = 12.sp)
                    Slider(
                        value = quorumMult,
                        onValueChange = { quorumMult = it },
                        valueRange = 0.5f..2.5f,
                        colors = SliderDefaults.colors(thumbColor = AccentTeal, activeTrackColor = AccentTeal)
                    )

                    Text("Порог подачи предложения: ${minRepThreshold.toInt()} REP", color = TextLight, fontSize = 12.sp)
                    Slider(
                        value = minRepThreshold,
                        onValueChange = { minRepThreshold = it },
                        valueRange = 5.0f..50.0f,
                        colors = SliderDefaults.colors(thumbColor = AccentTeal, activeTrackColor = AccentTeal)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createFork(
                            forkIdInput,
                            forkTitleInput,
                            forkDescInput,
                            quorumMult.toDouble(),
                            minRepThreshold.toDouble()
                        )
                        forkIdInput = ""
                        forkTitleInput = ""
                        forkDescInput = ""
                        showCreateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = SpaceDark),
                    modifier = Modifier.testTag("submit_fork_btn")
                ) {
                    Text("Инициализировать Форк", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Отмена", color = TextMuted)
                }
            }
        )
    }
}

// -------------------------------------------------------------
// UI HELPER COMPONENTS
// -------------------------------------------------------------
@Composable
fun SeparatorTitle(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = HighContrastGold,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        HorizontalDivider(color = CosmicGray, thickness = 1.dp, modifier = Modifier.weight(1f))
    }
}

// -------------------------------------------------------------
// DESCRIPTIVE METADATA STRUCTS IN RUSSIAN
// -------------------------------------------------------------
data class InstituteDisplayInfo(
    val title: String,
    val icon: ImageVector,
    val desc: String,
    val rule: String
)

val institutesData = listOf(
    InstituteDisplayInfo(
        title = "1. Identity & Agent",
        icon = Icons.Default.AccountCircle,
        desc = "Учетная запись участника. Хранит историю зафиксированного вклада в Орден и репутационный вес (Reputation Score). Пресекает анонимный деструктив.",
        rule = "Agent(id, reputationScore) -> Weight = reputationScore"
    ),
    InstituteDisplayInfo(
        title = "2. Council & Parliament",
        icon = Icons.Default.List,
        desc = "Законодательный созываемый Совет пиров. Формирует повестку дня общества. Одобренные репутационным голосованием Хартии вступают в силу автоматически.",
        rule = "if (yesVotesRep > noVotesRep && totalRep >= quorumRequired) ACCEPT"
    ),
    InstituteDisplayInfo(
        title = "3. Judiciary & Arbitration",
        icon = Icons.Default.Warning,
        desc = "Судебная власть Ордена. Разрешает внутренние конфликты на базе неизменного Кодекса Конституции. Судьи присяжных отбираются случайно во избежание подкупа.",
        rule = "selectJury(candidates, 2) -> randomized eligible members >= 50 REP"
    ),
    InstituteDisplayInfo(
        title = "4. Executive & Administration",
        icon = Icons.Default.Build,
        desc = "Исполнительная Управа участников. Набирает роли под конкретные задачи. Задачи создаются автоматически как продолжение принятых Советом законов.",
        rule = "onProposalApproved() -> trigger autoCreateTask() -> awardRepOnComplete()"
    ),
    InstituteDisplayInfo(
        title = "5. Election & Voting",
        icon = Icons.Default.CheckCircle,
        desc = "Избирательная комиссия и Сибил-протокол. Предотвращает спам-голосования виртуалами. Игнорирует голоса ботов ниже определенного reputational ценза.",
        rule = "validateVote(voter) -> return voter.reputationScore >= SybilThreshold"
    ),
    InstituteDisplayInfo(
        title = "6. Repository & Fork",
        icon = Icons.Default.Share,
        desc = "Архивация правил и Древо ветвлений. Код Ордена может быть форкнут в любую секунду, дабы не дать системе заржаветь и законсервировать элиту.",
        rule = "forkRepository(parentId, paramAdjustments) -> spawn parallel instance"
    )
)

data class OopModelDisplayInfo(
    val className: String,
    val meta: String,
    val methods: String
)

val oopModelData = listOf(
    OopModelDisplayInfo(
        className = "class Agent",
        meta = "Хранилище репутации участника",
        methods = "fun calculateVoteWeight(): Double = reputationScore\n" +
                "fun registerContribution(c: Contribution): Agent"
    ),
    OopModelDisplayInfo(
        className = "class Proposal & Council",
        meta = "Управление голосованием Советом",
        methods = "fun castVote(voter: Agent, approve: Boolean)\n" +
                "fun checkQuorumReached(): Boolean\n" +
                "fun executeLaw(): Task  // Trigger automatic executive event"
    ),
    OopModelDisplayInfo(
        className = "class Dispute & JudiciaryStore",
        meta = "Разрешение нарушений Кодекса",
        methods = "fun selectJuryMembers(peers: List<Agent>): List<Agent>\n" +
                "fun evaluateVerdict(): DisputeStatus\n" +
                "fun enforcePenalty(): Agent  // Deduct bad reputations"
    ),
    OopModelDisplayInfo(
        className = "class ExecutiveEngine",
        meta = "Автореализация принятых законов",
        methods = "fun createTaskFromProposal(p: Proposal): Task\n" +
                "fun auditAndAwardReputation(t: Task, auditor: Agent): Agent"
    ),
    OopModelDisplayInfo(
        className = "class RepositoryForkManager",
        meta = "Контроль нелинейной эволюции системы",
        methods = "fun createVersionFork(p: SystemFork, newMultiplier: Double): SystemFork\n" +
                "fun mergeDeltas(crdtState1: Table, crdtState2: Table): Table"
    )
)
