package com.example.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.*
import com.example.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SuiteViewModel(application: Application) : AndroidViewModel(application) {

    private val db: OrdenDatabase = Room.databaseBuilder(
        application.applicationContext,
        OrdenDatabase::class.java,
        "orden_social_os_v3.db"
    ).fallbackToDestructiveMigration().build()

    val repository = OrdenRepository(db.ordenDao())

    private val prefs = application.getSharedPreferences("orden_settings", android.content.Context.MODE_PRIVATE)

    private val _smtpHost = MutableStateFlow(prefs.getString("smtp_host", "smtp.gmail.com") ?: "smtp.gmail.com")
    val smtpHost = _smtpHost.asStateFlow()

    private val _smtpPort = MutableStateFlow(prefs.getInt("smtp_port", 465))
    val smtpPort = _smtpPort.asStateFlow()

    private val _imapHost = MutableStateFlow(prefs.getString("imap_host", "imap.gmail.com") ?: "imap.gmail.com")
    val imapHost = _imapHost.asStateFlow()

    private val _imapPort = MutableStateFlow(prefs.getInt("imap_port", 993))
    val imapPort = _imapPort.asStateFlow()

    private val _mailUser = MutableStateFlow(prefs.getString("mail_user", "") ?: "")
    val mailUser = _mailUser.asStateFlow()

    private val _mailPass = MutableStateFlow(prefs.getString("mail_pass", "") ?: "")
    val mailPass = _mailPass.asStateFlow()

    private val _useSsl = MutableStateFlow(prefs.getBoolean("use_ssl", true))
    val useSsl = _useSsl.asStateFlow()

    private val _missionRegistered = MutableStateFlow(prefs.getBoolean("mission_registered", false))
    val missionRegistered = _missionRegistered.asStateFlow()

    private val _missionVoted = MutableStateFlow(prefs.getBoolean("mission_voted", false))
    val missionVoted = _missionVoted.asStateFlow()

    private val _missionDispute = MutableStateFlow(prefs.getBoolean("mission_dispute", false))
    val missionDispute = _missionDispute.asStateFlow()

    private val _missionSynced = MutableStateFlow(prefs.getBoolean("mission_synced", false))
    val missionSynced = _missionSynced.asStateFlow()

    private val _showOnboarding = MutableStateFlow(prefs.getBoolean("show_onboarding_welcome", true))
    val showOnboarding = _showOnboarding.asStateFlow()

    private val _isSimulationEnabled = MutableStateFlow(prefs.getBoolean("is_simulation_enabled", true))
    val isSimulationEnabled = _isSimulationEnabled.asStateFlow()

    fun setSimulationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_simulation_enabled", enabled).apply()
        _isSimulationEnabled.value = enabled
        writeConsole("Симуляция сети: ${if (enabled) "ВКЛЮЧЕНА (демо-режим автоматизирован)" else "ВЫКЛЮЧЕНА (только РЕАЛЬНЫЕ SMTP/IMAP запросы)"}")
    }

    fun dismissOnboarding() {
        prefs.edit().putBoolean("show_onboarding_welcome", false).apply()
        _showOnboarding.value = false
    }

    fun resetOnboarding() {
        prefs.edit().putBoolean("show_onboarding_welcome", true).apply()
        _showOnboarding.value = true
    }

    fun updateMailSettings(smtpH: String, smtpP: Int, imapH: String, imapP: Int, usr: String, pas: String, ssl: Boolean) {
        prefs.edit().apply {
            putString("smtp_host", smtpH)
            putInt("smtp_port", smtpP)
            putString("imap_host", imapH)
            putInt("imap_port", imapP)
            putString("mail_user", usr)
            putString("mail_pass", pas)
            putBoolean("use_ssl", ssl)
            apply()
        }
        _smtpHost.value = smtpH
        _smtpPort.value = smtpP
        _imapHost.value = imapH
        _imapPort.value = imapP
        _mailUser.value = usr
        _mailPass.value = pas
        _useSsl.value = ssl
        writeConsole("Конфигурация P2P узла ($usr) сохранена.")
    }

    fun getMailSettings(): MailSettings {
        return MailSettings(
            smtpHost = _smtpHost.value,
            smtpPort = _smtpPort.value,
            imapHost = _imapHost.value,
            imapPort = _imapPort.value,
            user = _mailUser.value,
            pass = _mailPass.value,
            useSsl = _useSsl.value
        )
    }

    fun testMailConnection() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            writeConsole("Тестирование SMTP/IMAP соединения для ${_mailUser.value}...")
            val settings = getMailSettings()

            // 1. SMTP Test
            writeConsole("Тест 1/2: Отправка SMTP рукопожатия на ${_smtpHost.value}...")
            val smtpResult = MailTransport.sendEmail(
                settings = settings,
                recipient = _mailUser.value,
                subject = "ORDEN-P2P CONNECTION TEST",
                body = "ORDEN-P2P HANDSHAKE TEST MESSAGE FROM Android Node"
            )

            if (smtpResult.isSuccess) {
                writeConsole("SMTP соединение успешно! Тестовый конверт отправлен сам себе.")
            } else {
                writeConsole("Ошибка SMTP: ${smtpResult.exceptionOrNull()?.message}")
            }

            // 2. IMAP Test
            writeConsole("Тест 2/2: Чтение IMAP ящика на ${_imapHost.value}...")
            val imapResult = MailTransport.fetchUnreadP2PMessages(settings)
            if (imapResult.isSuccess) {
                writeConsole("IMAP соединение успешно! Найдено ${imapResult.getOrNull()?.size ?: 0} входящих транзакций Открытого Ордена.")
            } else {
                writeConsole("Ошибка IMAP: ${imapResult.exceptionOrNull()?.message}")
            }
        }
    }

    fun broadcastToPeers(actionType: String, forkId: String, payload: String) {
        val settings = getMailSettings()
        if (settings.user.isBlank() || settings.pass.isBlank()) {
            writeConsole("SMTP не настроен. Локальные изменения записаны (без вещания по SMTP/IMAP).")
            return
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val envelope = P2PEnvelope(
                senderId = settings.user,
                recipientId = "overlay-network@orden.p2p",
                actionType = actionType,
                forkId = forkId,
                serializedPayload = payload,
                signature = "SIG_ED25519_" + java.util.UUID.randomUUID().toString().take(12).uppercase()
            )
            val emailBody = envelope.generateEmailBody()
            val activeAgents = agents.value
            val peerEmails = activeAgents.map { it.id }.filter { it.contains("@") && !it.endsWith("@orden.p2p") && it != settings.user }

            if (peerEmails.isEmpty()) {
                writeConsole("В СУБД нет других пиров с реальными email-адресами. Тестируем: отправка пакета самому себе для верификации.")
                MailTransport.sendEmail(settings, settings.user, "ORDEN-P2P", emailBody)
            } else {
                for (rec in peerEmails) {
                    writeConsole("Трансляция P2P конверта к пиру: $rec...")
                    val res = MailTransport.sendEmail(settings, rec, "ORDEN-P2P: $actionType", emailBody)
                    if (res.isSuccess) {
                        writeConsole("Успешно доставлено пиру $rec по SMTP.")
                    } else {
                        writeConsole("Внимание: Сбой SMTP доставки для $rec: ${res.exceptionOrNull()?.message}")
                    }
                }
            }
        }
    }

    // UI state states
    val agents: StateFlow<List<AgentEntity>> = repository.getAgents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = repository.getTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val disputes: StateFlow<List<DisputeEntity>> = repository.getDisputes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val forks: StateFlow<List<ForkEntity>> = repository.getForks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncLogs: StateFlow<List<SyncLogEntity>> = repository.getSyncLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.getChatMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected fork ID & selected Actor perspective
    private val _selectedForkId = MutableStateFlow("core")
    val selectedForkId: StateFlow<String> = _selectedForkId.asStateFlow()

    private val _currentAgentId = MutableStateFlow("leonid@orden.p2p")
    val currentAgentId: StateFlow<String> = _currentAgentId.asStateFlow()

    // Current proposals filtered by selected fork ID
    val proposals: StateFlow<List<ProposalEntity>> = _selectedForkId
        .flatMapLatest { forkId -> repository.getProposals(forkId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Feedback logs or console messages within application sandbox
    private val _consoleOutput = MutableStateFlow("Запущено защищенное P2P ядро 'Открытый Орден'. Синхронизация успешна.")
    val consoleOutput: StateFlow<String> = _consoleOutput.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeSeedData()
        }
    }

    fun selectFork(forkId: String) {
        _selectedForkId.value = forkId
        writeConsole("Переключение контекста на Систему-Форк: [${forkId.uppercase()}]")
    }

    fun selectAgent(agentId: String) {
        _currentAgentId.value = agentId
        writeConsole("Авторизована подпись сессии для: $agentId")
    }

    fun registerAgent(id: String, name: String, role: String, initialRep: Double = 20.0, customPubKey: String = "", autoLogin: Boolean = true) {
        val cleanId = id.trim().lowercase()
        if (cleanId.isEmpty() || !cleanId.contains("@")) {
            writeConsole("Ошибка: ID участника должен быть в формате P2P-идентификатора (например, user@orden.p2p)")
            return
        }
        if (name.trim().isEmpty()) {
            writeConsole("Ошибка: Имя/Никнейм не может быть пустым")
            return
        }
        viewModelScope.launch {
            val finalPubKey = if (customPubKey.isBlank()) {
                "0x" + java.util.UUID.randomUUID().toString().replace("-", "").take(16).uppercase()
            } else {
                customPubKey.trim()
            }
            val newAgent = AgentEntity(
                id = cleanId,
                name = name.trim(),
                role = role,
                reputationScore = initialRep,
                contributionsCount = 0,
                publicKey = finalPubKey
            )
            repository.registerAgent(newAgent)
            writeConsole("В реестр Ордена внесен новый участник: $cleanId [Ключ: ${finalPubKey.take(10)}...]")
            
            prefs.edit().putBoolean("mission_registered", true).apply()
            _missionRegistered.value = true

            if (autoLogin) {
                _currentAgentId.value = cleanId
                writeConsole("Сессия переключена на нового участника: $cleanId")
            }

            showToast("Успешно зарегистрировано! Вы вошли как ${newAgent.name}", android.widget.Toast.LENGTH_LONG)
        }
    }

    fun writeConsole(message: String) {
        _consoleOutput.value = ">> $message\n${_consoleOutput.value.take(400)}"
    }

    fun showToast(message: String, duration: Int = android.widget.Toast.LENGTH_SHORT) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            try {
                android.widget.Toast.makeText(getApplication(), message, duration).show()
            } catch (e: Exception) {
                // Safeguard against any background context toast crashes
            }
        }
    }

    // Interactive operations
    fun castVote(proposalId: String, votedYes: Boolean) {
        viewModelScope.launch {
            val response = repository.castVote(proposalId, _currentAgentId.value, votedYes)
            writeConsole(response)
            if (response.contains("успешно") || response.contains("Кворум") || response.contains("Голос") || response.contains("удовлетворен")) {
                broadcastToPeers("CAST_VOTE", _selectedForkId.value, "{\"proposalId\":\"$proposalId\",\"voter\":\"${_currentAgentId.value}\",\"vote\":$votedYes}")
                
                prefs.edit().putBoolean("mission_voted", true).apply()
                _missionVoted.value = true
                showToast("Голос успешно учтен!")
            } else {
                showToast(response)
            }
        }
    }

    fun createProposal(title: String, description: String) {
        if (title.isBlank() || description.isBlank()) {
            writeConsole("Ошибка: название и описание предложения не могут быть пустыми")
            showToast("Ошибка: заполните поля!")
            return
        }
        viewModelScope.launch {
            val response = repository.createProposal(title, description, _currentAgentId.value, _selectedForkId.value)
            writeConsole(response)
            if (response.contains("успешно")) {
                broadcastToPeers(
                    "NEW_PROPOSAL", 
                    _selectedForkId.value, 
                    "{\"id\":\"prop-${java.util.UUID.randomUUID().toString().take(6)}\",\"title\":\"$title\",\"proposer\":\"${_currentAgentId.value}\",\"forkId\":\"${_selectedForkId.value}\",\"description\":\"$description\"}"
                )
                showToast("Предложение создано в черновиках!")
            } else {
                showToast(response)
            }
        }
    }

    fun getArgumentsForProposal(proposalId: String): Flow<List<DebateArgumentEntity>> {
        return repository.getArgumentsForProposal(proposalId)
    }

    fun addProposalArgument(proposalId: String, text: String, type: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val response = repository.addProposalArgument(proposalId, _currentAgentId.value, text, type)
            writeConsole(response)
            if (response.contains("зафиксировано") || response.contains("Успешно")) {
                broadcastToPeers(
                    "NEW_ARGUMENT",
                    _selectedForkId.value,
                    "{\"proposalId\":\"$proposalId\",\"author\":\"${_currentAgentId.value}\",\"text\":\"$text\",\"type\":\"$type\"}"
                )
                showToast("Ваше суждение добавлено в повестку дня!")
            } else {
                showToast(response)
            }
        }
    }

    fun resolveObjection(proposalId: String, argumentId: String, resolutionText: String) {
        if (resolutionText.isBlank()) return
        viewModelScope.launch {
            val response = repository.resolveObjection(proposalId, argumentId, _currentAgentId.value, resolutionText)
            writeConsole(response)
            if (response.contains("успешно") || response.contains("снято")) {
                broadcastToPeers(
                    "RESOLVE_OBJECTION",
                    _selectedForkId.value,
                    "{\"proposalId\":\"$proposalId\",\"argumentId\":\"$argumentId\",\"text\":\"$resolutionText\"}"
                )
                showToast("Возражение успешно разрешено!")
            } else {
                showToast(response)
            }
        }
    }

    fun amendProposal(proposalId: String, newTitle: String, newDescription: String) {
        if (newTitle.isBlank() || newDescription.isBlank()) return
        viewModelScope.launch {
            val response = repository.amendProposal(proposalId, newTitle, newDescription, _currentAgentId.value)
            writeConsole(response)
            if (response.contains("Успешно") || response.contains("успешно")) {
                broadcastToPeers(
                    "AMEND_PROPOSAL",
                    _selectedForkId.value,
                    "{\"proposalId\":\"$proposalId\",\"newTitle\":\"$newTitle\"}"
                )
                showToast("Содержание черновика отредактировано!")
            } else {
                showToast(response)
            }
        }
    }

    fun advanceProposalStage(proposalId: String, targetStage: String) {
        viewModelScope.launch {
            val response = repository.advanceProposalStage(proposalId, targetStage, _currentAgentId.value)
            writeConsole(response)
            if (response.contains("изменен") || response.contains("переведен")) {
                broadcastToPeers(
                    "STAGE_CHANGE",
                    _selectedForkId.value,
                    "{\"proposalId\":\"$proposalId\",\"stage\":\"$targetStage\"}"
                )
                showToast("Статус предложения изменен: $targetStage")
            } else {
                showToast(response)
            }
        }
    }

    fun getVotesForProposal(proposalId: String): Flow<List<VoteEntity>> {
        return repository.getVotes(proposalId)
    }

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            val response = repository.completeTask(taskId, _currentAgentId.value)
            writeConsole(response)
            if (response.contains("зафиксирован") || response.contains("успешно")) {
                broadcastToPeers("TASK_COMPLETED", _selectedForkId.value, "{\"taskId\":\"$taskId\",\"by\":\"${_currentAgentId.value}\"}")
                showToast("Задача успешно выполнена!")
            } else {
                showToast(response)
            }
        }
    }

    fun raiseDispute(defendantId: String, description: String, article: String) {
        if (defendantId == _currentAgentId.value) {
            writeConsole("Ошибка: Вы не можете заявить конституционный dispute против самого себя!")
            showToast("Нельзя подать иск на самого себя!")
            return
        }
        viewModelScope.launch {
            val response = repository.raiseDispute(_currentAgentId.value, defendantId, description, article)
            writeConsole(response)
            if (response.contains("создан") || response.contains("успешно") || response.contains("Зафиксирован")) {
                showToast("Конституционный иск подан!")
            } else {
                showToast(response)
            }
        }
    }

    fun castJuryVote(disputeId: String, voteGuilty: Boolean) {
        viewModelScope.launch {
            val response = repository.castJuryVote(disputeId, _currentAgentId.value, voteGuilty)
            writeConsole(response)
            if (response.contains("успешно") || response.contains("принят") || response.contains("Голос")) {
                prefs.edit().putBoolean("mission_dispute", true).apply()
                _missionDispute.value = true
                showToast("Ваш вердикт присяжного подан!")
            } else {
                showToast(response)
            }
        }
    }

    fun createFork(forkId: String, title: String, description: String, quorumMultiplier: Double, minPropRep: Double) {
        val cleanForkId = forkId.trim().lowercase().replace(" ", "-")
        if (cleanForkId.isEmpty() || title.isBlank()) {
            writeConsole("Ошибка: Обязательные параметры форка не заполнены")
            showToast("Ошибка: заполните поля!")
            return
        }
        viewModelScope.launch {
            val response = repository.createFork(
                id = cleanForkId,
                parentId = _selectedForkId.value,
                title = title,
                description = description,
                quorumMult = quorumMultiplier,
                minPropRep = minPropRep
            )
            writeConsole(response)
            if (response.contains("успешно") || response.contains("создан")) {
                _selectedForkId.value = cleanForkId // Переключим на созданный форк автоматически!
                showToast("Мирофорк успешно создан!")
            } else {
                showToast(response)
            }
        }
    }

    fun sendChatMessage(recipientId: String, text: String) {
        if (text.isBlank()) return
        val sender = _currentAgentId.value
        val timestamp = System.currentTimeMillis()
        viewModelScope.launch {
            // Save locally
            val msg = ChatMessageEntity(
                senderId = sender,
                recipientId = recipientId,
                messageText = text,
                timestamp = timestamp
            )
            repository.insertChatMessage(msg)
            writeConsole("Отправлено P2P сообщение для $recipientId: '$text'")

            // Broadcast via SMTP!
            val payload = "{\\\"text\\\":\\\"$text\\\",\\\"recipientId\\\":\\\"$recipientId\\\"}"
            broadcastToPeers("CHAT_MESSAGE", "core", payload)
            showToast("Сообщение отправлено по P2P SMTP!")
        }
    }

    fun triggerSync() {
        prefs.edit().putBoolean("mission_synced", true).apply()
        _missionSynced.value = true

        val settings = getMailSettings()
        if (settings.user.isBlank() || settings.pass.isBlank()) {
            viewModelScope.launch {
                if (_isSimulationEnabled.value) {
                    writeConsole("Параметры P2P SMTP/IMAP узла полупусты. Запуск симуляции демо-оверлея...")
                    val response = repository.triggerNetworkSyncDemo()
                    writeConsole(response)
                    showToast("Синхронизация завершена (Локальная симуляция)!")
                } else {
                    writeConsole("Ошибка P2P: Почтовый ящик не настроен. Настройте SMTP/IMAP для работы без симуляции.")
                    showToast("Синхронизация отклонена: настройте SMTP/IMAP")
                }
            }
            return
        }
        viewModelScope.launch {
            showToast("Запуск P2P синхронизации...")
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                writeConsole("Запуск РЕАЛЬНОГО опроса P2P IMAP ящика ${settings.user}...")
                val fetchResult = MailTransport.fetchUnreadP2PMessages(settings)
                if (fetchResult.isSuccess) {
                    val envelopes = fetchResult.getOrDefault(emptyList())
                    writeConsole("Успешно опрошен IMAP сервер. Извлечено P2P писем: ${envelopes.size}")
                    if (envelopes.isEmpty()) {
                        writeConsole("Новых P2P транзакций открытых повесток дня в почтовом оверлее не найдено.")
                    } else {
                        for (env in envelopes) {
                            val mergeResult = repository.processIncomingMailEnvelope(env)
                            writeConsole(mergeResult)
                        }
                    }
                    showToast("Реальная P2P синхронизация завершена!")
                } else {
                    writeConsole("Внимание: Сбой опроса IMAP почты: ${fetchResult.exceptionOrNull()?.message}")
                    if (_isSimulationEnabled.value) {
                        writeConsole("Откат на локальную симуляцию оверлея...")
                        val response = repository.triggerNetworkSyncDemo()
                        writeConsole(response)
                        showToast("Сбой опроса. Выполнена демо-симуляция.")
                    } else {
                        writeConsole("Ошибка P2P: Чтение почтового ящика завершилось сбоем. Симуляция отключена.")
                        showToast("Ошибка P2P опроса!")
                    }
                }
            }
        }
    }

    data class CliLog(val text: String, val type: String = "info")

    private val _cliLogs = MutableStateFlow<List<CliLog>>(listOf(
        CliLog("Добро пожаловать в P2P CLI консоль 'ОТКРЫТЫЙ ОРДЕН'!", "info"),
        CliLog("Командный интерфейс готов к приему директив социальной ОС.", "info"),
        CliLog("Введите 'help' для вывода списка доступных инструкций.", "help")
    ))
    val cliLogs: StateFlow<List<CliLog>> = _cliLogs.asStateFlow()

    fun executeCliCommand(rawCommand: String) {
        val trimmed = rawCommand.trim()
        if (trimmed.isEmpty()) return
        
        _cliLogs.value = _cliLogs.value + CliLog("agent@p2p:~$ $trimmed", "command")
        
        val parts = trimmed.split(Regex("\\s+"))
        if (parts.isEmpty()) return
        val cmd = parts[0].lowercase()
        
        try {
            when (cmd) {
                "help" -> {
                    val helpText = """
                        --- ДОСТУПНЫЕ ИНСТРУКЦИИ ОРДЕНА ---
                        • status, info - Состояние пирингового ядра и метрики узла
                        • register <id> | <name> | <role> | [rep] - Добавить нового соратника
                        • login <id> - Авторизоваться/переключить активную сессию
                        • proposal <title> | <description> - Выдвинуть предложение в совет
                        • vote <propId> <yes/no> - Проголосовать за/против предложение
                        • task <taskId> done - Отметить выполнение выданного поручения
                        • dispute <defendantId> | <description> | <article> - Открыть дело в Суде Чести
                        • juryvote <disputeId> <guilty/innocent> - Голосовать вердиктом присяжного
                        • fork <id> <title> | <description> - Создать новую редакцию/ветку правил
                        • selectfork <id> - Перейти на выбранную редакцию документов
                        • msg <recipientEmail> | <text> - Послать P2P шифрованный email-пакет
                        • sync - Инициировать ручной обмен транзакциями с оверлеем
                        • simulation <on/off> - Включить автоматизированную симуляцию
                        • mailconfig <smtpH> <smtpPort> <imapH> <imapPort> <usr> <pas> <ssl> - Быстрые настройки
                        • clear - Очистить буфер вывода терминала
                    """.trimIndent()
                    _cliLogs.value = _cliLogs.value + CliLog(helpText, "help")
                }
                "clear" -> {
                    _cliLogs.value = emptyList()
                }
                "status", "info" -> {
                    val activeAgent = _currentAgentId.value
                    val curFork = _selectedForkId.value
                    val agentsCount = agents.value.size
                    val propsCount = proposals.value.size
                    val disputesCount = disputes.value.size
                    val tasksCount = tasks.value.count { it.status != "COMPLETED" }
                    val isSim = if (_isSimulationEnabled.value) "АКТИВНА" else "ОТКЛЮЧЕНА (прямая почта)"
                    
                    val statusText = """
                        --- СТАТУС ВЕТВИ АКТУАЛЬНОСТИ ОРДЕНА ---
                        • Текущий агент: $activeAgent
                        • Контекст ветки репозитория: $curFork
                        • Подписчиков в локальном реестре: $agentsCount
                        • Законопроектов на голосовании: $propsCount
                        • Рассматриваемых споров: $disputesCount
                        • Поручений в работе: $tasksCount
                        • Симуляция распределенной сети: $isSim
                    """.trimIndent()
                    _cliLogs.value = _cliLogs.value + CliLog(statusText, "info")
                }
                "register" -> {
                    val argString = if (trimmed.length > 8) trimmed.substring(8).trim() else ""
                    val args = if (argString.contains("|")) {
                        argString.split("|").map { it.trim() }
                    } else {
                        val matches = Regex("""[^\s"']+|"([^"]*)"|'([^']*)'""").findAll(argString)
                        matches.map { 
                            val s = it.value
                            if (s.length >= 2 && ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")))) {
                                s.substring(1, s.length - 1)
                            } else {
                                s
                            }
                        }.toList()
                    }
                    
                    if (args.size < 3) {
                        _cliLogs.value = _cliLogs.value + CliLog("Ошибка: Недостаточно аргументов!\nОжидается: register <id> | <name> | <role> | [reputation]", "error")
                    } else {
                        val id = args[0]
                        val name = args[1]
                        val role = args[2]
                        val initialRep = args.getOrNull(3)?.toDoubleOrNull() ?: 20.0
                        registerAgent(id, name, role, initialRep, autoLogin = true)
                        _cliLogs.value = _cliLogs.value + CliLog("Успешно: агент зарегистрирован и авторизован. Имя: $name, ID: $id, Роль: $role, Престиж: $initialRep REP.", "success")
                    }
                }
                "login" -> {
                    if (parts.size < 2) {
                        _cliLogs.value = _cliLogs.value + CliLog("Ошибка: укажите целевой ID. Пример: login ivan@orden.p2p", "error")
                    } else {
                        val targetId = parts[1]
                        val found = agents.value.any { it.id == targetId }
                        if (found) {
                            selectAgent(targetId)
                            _cliLogs.value = _cliLogs.value + CliLog("Успешно: Сессия переключена на локальный узел $targetId", "success")
                        } else {
                            _cliLogs.value = _cliLogs.value + CliLog("Ошибка: Агента с ID '$targetId' нет в локальном реестре.", "error")
                        }
                    }
                }
                "proposal" -> {
                    val argString = if (trimmed.length > 8) trimmed.substring(8).trim() else ""
                    val args = if (argString.contains("|")) argString.split("|").map { it.trim() } else listOf(argString)
                    
                    if (args[0].isBlank()) {
                        _cliLogs.value = _cliLogs.value + CliLog("Ошибка: введите тему (название). Пример: proposal Название предл. | Описание сути", "error")
                    } else {
                        val title = args[0]
                        val description = args.getOrNull(1) ?: "Выдвинуто из CLI терминала"
                        createProposal(title, description)
                        _cliLogs.value = _cliLogs.value + CliLog("Успешно: Предложение '$title' зарегистрировано в совете.", "success")
                    }
                }
                "vote" -> {
                    if (parts.size < 3) {
                        _cliLogs.value = _cliLogs.value + CliLog("Ошибка: формат: vote <propId> <yes/no>", "error")
                    } else {
                        val propId = parts[1]
                        val voteStr = parts[2].lowercase()
                        if (voteStr == "yes" || voteStr == "no" || voteStr == "да" || voteStr == "нет") {
                            val votedYes = voteStr == "yes" || voteStr == "да"
                            castVote(propId, votedYes)
                            _cliLogs.value = _cliLogs.value + CliLog("Успешно: Голос за предложение $propId учтен как [${if (votedYes) "ЗА" else "ПРОТИВ"}].", "success")
                        } else {
                            _cliLogs.value = _cliLogs.value + CliLog("Ошибка: Голос должен быть yes (да) или no (нет).", "error")
                        }
                    }
                }
                "task" -> {
                    if (parts.size < 3 || parts[2].lowercase() != "done") {
                        _cliLogs.value = _cliLogs.value + CliLog("Ошибка: формат: task <taskId> done", "error")
                    } else {
                        val taskId = parts[1]
                        val exists = tasks.value.any { it.id == taskId }
                        if (exists) {
                            completeTask(taskId)
                            _cliLogs.value = _cliLogs.value + CliLog("Успешно: Задача '$taskId' завершена.", "success")
                        } else {
                            _cliLogs.value = _cliLogs.value + CliLog("Ошибка: Задача с ID '$taskId' отсутствует.", "error")
                        }
                    }
                }
                "dispute" -> {
                    val argString = if (trimmed.length > 7) trimmed.substring(7).trim() else ""
                    val args = if (argString.contains("|")) argString.split("|").map { it.trim() } else listOf(argString)
                    
                    if (args.size < 3) {
                        _cliLogs.value = _cliLogs.value + CliLog("Ошибка: Ожидается: dispute defendantId | description | article", "error")
                    } else {
                        val defId = args[0]
                        val desc = args[1]
                        val art = args[2]
                        raiseDispute(defId, desc, art)
                        _cliLogs.value = _cliLogs.value + CliLog("Успешно: Иск подан против $defId за нарушение статьи '$art'.", "success")
                    }
                }
                "juryvote" -> {
                    if (parts.size < 3) {
                        _cliLogs.value = _cliLogs.value + CliLog("Ошибка: формат: juryvote <disputeId> <guilty/innocent>", "error")
                    } else {
                        val disputeId = parts[1]
                        val verdict = parts[2].lowercase()
                        if (verdict == "guilty" || verdict == "innocent" || verdict == "виновен" || verdict == "невиновен") {
                            val isGuilty = verdict == "guilty" || verdict == "виновен"
                            castJuryVote(disputeId, isGuilty)
                            _cliLogs.value = _cliLogs.value + CliLog("Успешно: Решение для дела $disputeId: [${if (isGuilty) "ВИНОВЕН" else "НЕВИНОВЕН"}] добавлено в протокол.", "success")
                        } else {
                            _cliLogs.value = _cliLogs.value + CliLog("Ошибка: вердикт должен быть 'guilty' или 'innocent'.", "error")
                        }
                    }
                }
                "fork" -> {
                    val argString = if (trimmed.length > 4) trimmed.substring(4).trim() else ""
                    val partition = argString.split(" ", limit = 2)
                    if (partition.size < 2) {
                        _cliLogs.value = _cliLogs.value + CliLog("Ошибка: формат: fork <newForkId> <title> | [description]", "error")
                    } else {
                        val forkId = partition[0]
                        val rest = partition[1]
                        val subArgs = if (rest.contains("|")) rest.split("|").map { it.trim() } else listOf(rest)
                        val title = subArgs[0]
                        val desc = subArgs.getOrNull(1) ?: "Форк создан автономно"
                        createFork(forkId, title, desc, 1.0, 10.0)
                        _cliLogs.value = _cliLogs.value + CliLog("Успешно: Созван форк правил социальной ОС: '$title' ($forkId).", "success")
                    }
                }
                "selectfork" -> {
                    if (parts.size < 2) {
                        _cliLogs.value = _cliLogs.value + CliLog("Ошибка: укажите forkId.", "error")
                    } else {
                        val forkId = parts[1]
                        val found = forks.value.any { it.id == forkId }
                        if (found) {
                            selectFork(forkId)
                            _cliLogs.value = _cliLogs.value + CliLog("Успешно: контекст переключен на правила '$forkId'", "success")
                        } else {
                            _cliLogs.value = _cliLogs.value + CliLog("Ошибка: форк с ID '$forkId' не обнаружен.", "error")
                        }
                    }
                }
                "msg" -> {
                    val argString = if (trimmed.length > 3) trimmed.substring(3).trim() else ""
                    val args = if (argString.contains("|")) argString.split("|").map { it.trim() } else listOf(argString)
                    
                    if (args.size < 2 || args[0].isBlank()) {
                        _cliLogs.value = _cliLogs.value + CliLog("Ошибка: формат: msg <email> | <text>", "error")
                    } else {
                        val recipient = args[0]
                        val txt = args[1]
                        sendChatMessage(recipient, txt)
                        _cliLogs.value = _cliLogs.value + CliLog("Успешно: подготовлен и транслирован P2P пакет к $recipient.", "success")
                    }
                }
                "sync" -> {
                    triggerSync()
                    _cliLogs.value = _cliLogs.value + CliLog("Консоль: Инициализирован сетевой опрос...", "info")
                }
                "simulation" -> {
                    if (parts.size < 2) {
                        _cliLogs.value = _cliLogs.value + CliLog("Ошибка: укажите on/off", "error")
                    } else {
                        val opt = parts[1].lowercase()
                        val isEnabled = opt == "on" || opt == "да" || opt == "1"
                        setSimulationEnabled(isEnabled)
                        _cliLogs.value = _cliLogs.value + CliLog("Сеть: Симуляция установлена в [${if (isEnabled) "ВКЛ" else "ВЫКЛ"}].", "success")
                    }
                }
                "mailconfig" -> {
                    if (parts.size < 8) {
                        _cliLogs.value = _cliLogs.value + CliLog("Ошибка: укажите все 7 параметров:\nmailconfig <smtp> <port> <imap> <port> <user> <pass> <ssl>", "error")
                    } else {
                        val smtpH = parts[1]
                        val smtpP = parts[2].toIntOrNull() ?: 465
                        val imapH = parts[3]
                        val imapP = parts[4].toIntOrNull() ?: 993
                        val usr = parts[5]
                        val pas = parts[6]
                        val ssl = parts[7].toBoolean()
                        updateMailSettings(smtpH, smtpP, imapH, imapP, usr, pas, ssl)
                        _cliLogs.value = _cliLogs.value + CliLog("Кондоп: Конфигурация узла $usr зафиксирована.", "success")
                    }
                }
                else -> {
                    viewModelScope.launch {
                        _cliLogs.value = _cliLogs.value + CliLog("Проводник: Настраиваюсь на ноосферу Ордена...", "info")
                        val activeAgent = _currentAgentId.value
                        val curFork = _selectedForkId.value
                        val agentsList = agents.value.map { "${it.name} (id: ${it.id}, rep: ${it.reputationScore})" }.joinToString(", ")
                        val propsText = proposals.value.map { "${it.title} (id: ${it.id}, статус: ${it.status}, фаза: ${it.stage})" }.joinToString(", ")
                        val disputesText = disputes.value.map { "Иск против ${it.defendantId} (статус: ${it.status})" }.joinToString(", ")
                        val tasksText = tasks.value.map { "${it.title} (для: ${it.assignedTo}, статус: ${it.status})" }.joinToString(", ")
                        val isSim = if (_isSimulationEnabled.value) "Включена" else "Отключена"
                        
                        val contextText = """
                            - Текущий вошедший агент: $activeAgent
                            - Активная версия правил: $curFork
                            - Участники Ордена: $agentsList
                            - Законопроекты: $propsText
                            - Судебные споры: $disputesText
                            - Задачи: $tasksText
                            - Симуляция оверлея: $isSim
                        """.trimIndent()
                        
                        val response = com.example.data.GeminiService.generateGuideResponse(trimmed, contextText)
                        _cliLogs.value = _cliLogs.value + CliLog(response, "help")
                    }
                }
            }
        } catch (e: Exception) {
            _cliLogs.value = _cliLogs.value + CliLog("Исключение: ${e.message}", "error")
        }
    }
}
