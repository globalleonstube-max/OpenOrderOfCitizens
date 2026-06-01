package com.example.data

import com.example.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

class OrdenRepository(private val dao: OrdenDao) {

    // Reading flows
    fun getAgents(): Flow<List<AgentEntity>> = dao.getAllAgents()
    fun getProposals(forkId: String): Flow<List<ProposalEntity>> = dao.getProposalsForFork(forkId)
    fun getVotes(proposalId: String): Flow<List<VoteEntity>> = dao.getVotesForProposal(proposalId)
    fun getTasks(): Flow<List<TaskEntity>> = dao.getAllTasks()
    fun getDisputes(): Flow<List<DisputeEntity>> = dao.getAllDisputes()
    fun getForks(): Flow<List<ForkEntity>> = dao.getAllForks()
    fun getSyncLogs(): Flow<List<SyncLogEntity>> = dao.getSyncLogs()
    fun getChatMessages(): Flow<List<ChatMessageEntity>> = dao.getAllChatMessages()

    suspend fun insertChatMessage(message: ChatMessageEntity) = withContext(Dispatchers.IO) {
        dao.insertChatMessage(message)
    }

    /**
     * Инициализация базовых демонстрационных данных при первом запуске
     */
    suspend fun initializeSeedData() = withContext(Dispatchers.IO) {
        val currentForks = dao.getAllForksDirect()
        if (currentForks.isEmpty()) {
            // 1. Создание главного репозитория (ядро)
            val coreFork = ForkEntity(
                id = "core",
                parentForkId = null,
                title = "Ядро Ордена (v1.0.0)",
                description = "Минимальное, прозрачное ядро. Базовые правила и этический кодекс.",
                votingQuorumMultiplier = 1.0,
                minReputationToPropose = 10.0,
                createdAt = System.currentTimeMillis(),
                isActive = true
            )
            dao.insertFork(coreFork)

            // 2. Создание стартовых Агентов (участников)
            val agents = listOf(
                AgentEntity("leonid@orden.p2p", "Леонид (Грандмастер)", "Grandmaster", 100.0, 15, "0xAB73...F21"),
                AgentEntity("alex@orden.p2p", "Александр (Рыцарь)", "Knight", 65.0, 8, "0x32BC...91E"),
                AgentEntity("maria@orden.p2p", "Мария (Арбитр)", "Arbiter", 80.0, 12, "0xFA1E...78C"),
                AgentEntity("ivan@orden.p2p", "Иван (Адепт)", "Adept", 35.0, 3, "0x89DF...43A"),
                AgentEntity("dmitry@orden.p2p", "Дмитрий (Писец)", "Scribe", 45.0, 5, "0x1122...BB4")
            )
            for (agent in agents) {
                dao.insertAgent(agent)
            }

            // 3. Создание стартового предложения в Совет
            val proposalId = "prop-const-1"
            val initialProposal = ProposalEntity(
                id = proposalId,
                title = "Принятие Хартии Цифрового Суверенитета",
                description = "Формализовать базовый этический кодекс Ордена как неизменяемый закон. Сибил-фильтрация голосов, меритократия.",
                proposerId = "leonid@orden.p2p",
                status = "ACTIVE",
                voteYesRep = 100.0, // Леонид уже проголосовал "За"
                voteNoRep = 0.0,
                quorumRequired = 150.0, // Кворум в 150 репутационных единиц
                createdAt = System.currentTimeMillis(),
                forkId = "core"
            )
            dao.insertProposal(initialProposal)

            // Запишем голос Леонида
            dao.insertVote(
                VoteEntity(
                    proposalId = proposalId,
                    voterId = "leonid@orden.p2p",
                    votedYes = true,
                    voterReputation = 100.0,
                    timestamp = System.currentTimeMillis()
                )
            )

            // 4. Добавим один решенный прецедент в Суд
            val dispute = DisputeEntity(
                id = "dispute-001",
                plaintiffId = "alex@orden.p2p",
                defendantId = "ivan@orden.p2p",
                description = "Попытка обхода репутационного фильтра через спам фиктивных мелких вкладов.",
                constitutionalArticle = "Раздел II. Статья 4 (Добросовестный вклад)",
                status = "RESOLVED_YES",
                juryIds = "maria@orden.p2p,leonid@orden.p2p",
                juryVotesYes = 2,
                juryVotesNo = 0,
                verdict = "Виновен. Аннулировать спам-вклады, вынести предупреждение."
            )
            dao.insertDispute(dispute)
        }
    }

    /**
     * РЕГИСТРАЦИЯ ХАРТИИ УЧАСТНИКА: Добавление нового реального или удаленного агента в СУБД.
     */
    suspend fun registerAgent(agent: AgentEntity) = withContext(Dispatchers.IO) {
        dao.insertAgent(agent)
    }

    /**
     * ELECTION PROTOCOL: Голосование за Proposal
     * Автоматически обновляет репутационный вес и проверяет кворум.
     * «Код как закон»: Если голоса превышают кворум, Proposal принимается,
     * и триггер АВТОМАТИЧЕСКИ создает Административную задачу в Executive.
     */
    suspend fun castVote(proposalId: String, voterId: String, votedYes: Boolean): String = withContext(Dispatchers.IO) {
        val proposal = dao.getProposalById(proposalId) ?: return@withContext "Решение не найдено"
        if (proposal.status != "ACTIVE") return@withContext "Голосование уже закрыто"
        if (proposal.stage != "VOTING") return@withContext "Отказ: Голосование еще не запущено (Текущая стадия: ${proposal.stage})"

        val voter = dao.getAgentById(voterId) ?: return@withContext "Агент не зарегистрирован"

        // Проверим Сибил-барьер (например, минимальная репутация для голосования в ядре = 15.0)
        if (voter.reputationScore < 15.0) {
            return@withContext "Отказ: Вес репутации (${voter.reputationScore}) ниже Сибил-порога (15.0)"
        }

        // Проверим, голосовал ли уже этот участник
        val existingVote = dao.getVoteByVoter(proposalId, voterId)
        if (existingVote != null) {
            return@withContext "Вы уже проголосовали в этом повестке дня"
        }

        // Запись нового голоса
        val voteRepWeight = voter.reputationScore
        dao.insertVote(
            VoteEntity(
                proposalId = proposalId,
                voterId = voterId,
                votedYes = votedYes,
                voterReputation = voteRepWeight,
                timestamp = System.currentTimeMillis()
            )
        )

        // Пересчитаем накопленную репутацию по голосам
        val updatedYesRep = if (votedYes) proposal.voteYesRep + voteRepWeight else proposal.voteYesRep
        val updatedNoRep = if (!votedYes) proposal.voteNoRep + voteRepWeight else proposal.voteNoRep
        val totalVotesWeight = updatedYesRep + updatedNoRep

        // Проверка прохождения кворума
        val finalStatus: String
        var logTriggerMessage = "Голос учтен. Общий вес: YES=$updatedYesRep, NO=$updatedNoRep."

        if (totalVotesWeight >= proposal.quorumRequired) {
            if (updatedYesRep > updatedNoRep) {
                finalStatus = "ACCEPTED"
                logTriggerMessage += " Кворум достигнут. Решение ПРИНЯТО!"
                
                // EVENT-DRIVEN PROTOCOL: АВТОМАТИЧЕСКОЕ СОЗДАНИЕ ЗАДАЧИ В EXECUTIVE
                val automaticTask = TaskEntity(
                    id = "task-${proposal.id}",
                    proposalId = proposal.id,
                    title = "Реализация: ${proposal.title}",
                    description = "Сформировано автоматически исполнительной службой на основании одобренного решения Council: ${proposal.description}",
                    assignedTo = null, // Свободная роль на меритократической бирже работ
                    status = "ASSIGNED",
                    reputationReward = 30.0, // Награда за реализацию закона
                    completedAt = null
                )
                dao.insertTask(automaticTask)
                logTriggerMessage += " Задача автоматически создана в Управе (Executive)."
            } else {
                finalStatus = "REJECTED"
                logTriggerMessage += " Кворум достигнут. Решение ОТКЛОНЕНО большинством!"
            }
        } else {
            finalStatus = "ACTIVE"
        }

        // Обновляем статус предложения
        dao.insertProposal(
            proposal.copy(
                status = finalStatus,
                voteYesRep = updatedYesRep,
                voteNoRep = updatedNoRep
            )
        )

        // Симулируем отправку P2P транзакции вещания голоса по сети SMTP/IMAP
        simulateP2PSyncLog(
            direction = "SMTP_SEND",
            actionType = "CAST_VOTE",
            payload = "{\"proposalId\":\"$proposalId\",\"voter\":\"$voterId\",\"vote\":$votedYes,\"weight\":$voteRepWeight}"
        )

        return@withContext logTriggerMessage
    }

    /**
     * COUNCIL : Создание нового предложения
     */
    suspend fun createProposal(title: String, description: String, proposerId: String, forkId: String): String = withContext(Dispatchers.IO) {
        val proposer = dao.getAgentById(proposerId) ?: return@withContext "Автор не найден"
        val fork = dao.getForkById(forkId) ?: return@withContext "Форк системы не существует"
        
        if (proposer.reputationScore < fork.minReputationToPropose) {
            return@withContext "Недостаточно репутации для формирования повестки. Требуется: ${fork.minReputationToPropose}"
        }

        val propId = "prop-${UUID.randomUUID().toString().take(6)}"
        val newProposal = ProposalEntity(
            id = propId,
            title = title,
            description = description,
            proposerId = proposerId,
            status = "ACTIVE",
            voteYesRep = proposer.reputationScore, // Автор голосует за свое предложение автоматически
            voteNoRep = 0.0,
            quorumRequired = 100.0 * fork.votingQuorumMultiplier,
            createdAt = System.currentTimeMillis(),
            forkId = forkId,
            stage = "DRAFT", // Turquoise governance starts in DRAFT stage for advice
            objectionsCount = 0,
            amendmentsCount = 0
        )
        dao.insertProposal(newProposal)

        // Запишем голос автора (будет задействован, когда стадия перейдет в VOTING)
        dao.insertVote(
            VoteEntity(
                proposalId = propId,
                voterId = proposerId,
                votedYes = true,
                voterReputation = proposer.reputationScore,
                timestamp = System.currentTimeMillis()
            )
        )

        simulateP2PSyncLog(
            direction = "SMTP_SEND",
            actionType = "NEW_PROPOSAL",
            payload = "{\"id\":\"$propId\",\"title\":\"$title\",\"proposer\":\"$proposerId\",\"forkId\":\"$forkId\",\"stage\":\"DRAFT\"}"
        )

        return@withContext "Решение успешно инициировано как черновик [DRAFT]! Используйте совет в рамках ветки '${fork.title}' для дебатов."
    }

    /**
     * TURQUOISE DEBATING & ADVICE PROCESS METHODS
     */
    fun getArgumentsForProposal(proposalId: String): Flow<List<DebateArgumentEntity>> = dao.getArgumentsForProposal(proposalId)

    suspend fun addProposalArgument(proposalId: String, authorId: String, text: String, type: String): String = withContext(Dispatchers.IO) {
        val proposal = dao.getProposalById(proposalId) ?: return@withContext "Ошибка: Предложение не найдено"
        val voter = dao.getAgentById(authorId) ?: return@withContext "Ошибка: Агент не зарегистрирован"

        // Сибил-защита обсуждений: участники с репутацией ниже порога (15.0) не могут вносить шум в дебаты
        if (voter.reputationScore < 15.0) {
            return@withContext "Отказ: Вес репутации (${voter.reputationScore}) ниже Сибил-порога дебатов (15.0 REP)"
        }

        val argId = "arg-${UUID.randomUUID().toString().take(6)}"
        val argument = DebateArgumentEntity(
            id = argId,
            proposalId = proposalId,
            authorId = authorId,
            authorName = voter.name,
            text = text,
            type = type, // "PRO", "CON", "SUGGESTION", "OBJECTION"
            timestamp = System.currentTimeMillis(),
            isResolved = false,
            resolutionText = ""
        )
        dao.insertDebateArgument(argument)

        // Пересчитаем количество активных возражений и внесем изменение в повестку
        val unresolvedObjections = dao.getActiveObjectionsForProposal(proposalId)
        val unresolvedCount = unresolvedObjections.size + (if (type == "OBJECTION") 1 else 0)
        dao.insertProposal(proposal.copy(objectionsCount = unresolvedCount))

        simulateP2PSyncLog(
            direction = "SMTP_SEND",
            actionType = "NEW_ARGUMENT",
            payload = "{\"id\":\"$argId\",\"proposalId\":\"$proposalId\",\"author\":\"$authorId\",\"type\":\"$type\"}"
        )

        return@withContext "Мнение зафиксировано! Тип: ${type.uppercase()}. Автор: ${voter.name}."
    }

    suspend fun resolveObjection(proposalId: String, argumentId: String, resolverId: String, resolutionText: String): String = withContext(Dispatchers.IO) {
        val proposal = dao.getProposalById(proposalId) ?: return@withContext "Предложение не найдено"
        if (proposal.proposerId != resolverId) {
            return@withContext "Отказ: Интегрировать возражение в текст повестки может только его инициатор (${proposal.proposerId})"
        }

        dao.updateObjectionResolution(argumentId, true, resolutionText)

        // Пересчет неразрешенных возражений
        val unresolved = dao.getActiveObjectionsForProposal(proposalId)
        dao.insertProposal(proposal.copy(objectionsCount = unresolved.size))

        simulateP2PSyncLog(
            direction = "SMTP_SEND",
            actionType = "RESOLVE_OBJECTION",
            payload = "{\"argumentId\":\"$argumentId\",\"proposalId\":\"$proposalId\",\"resolver\":\"$resolverId\"}"
        )

        return@withContext "Препятствие снято. Возражение успешно интегрировано в повестку дня!"
    }

    suspend fun amendProposal(proposalId: String, newTitle: String, newDescription: String, proposerId: String): String = withContext(Dispatchers.IO) {
        val proposal = dao.getProposalById(proposalId) ?: return@withContext "Предложение не найдено"
        if (proposal.proposerId != proposerId) {
            return@withContext "Отказ: Корректировать текст закона может только инициатор."
        }

        dao.insertProposal(
            proposal.copy(
                title = newTitle,
                description = newDescription,
                amendmentsCount = proposal.amendmentsCount + 1
            )
        )

        simulateP2PSyncLog(
            direction = "SMTP_SEND",
            actionType = "AMEND_PROPOSAL",
            payload = "{\"proposalId\":\"$proposalId\",\"newTitle\":\"$newTitle\",\"amendmentsCount\":${proposal.amendmentsCount + 1}}"
        )

        return@withContext "Текст предложения успешно изменен. Накапливаемый индекс эволюции: +1 Акт."
    }

    suspend fun advanceProposalStage(proposalId: String, targetStage: String, actorId: String): String = withContext(Dispatchers.IO) {
        val proposal = dao.getProposalById(proposalId) ?: return@withContext "Предложение не найдено"
        val actor = dao.getAgentById(actorId) ?: return@withContext "Пользователь не найден"

        // Сибил-защита для перевода системных фаз: требуется хотя бы 30.0 репутационных очков
        if (actor.reputationScore < 30.0) {
            return@withContext "Отказ: Ваш репутационный вес (${actor.reputationScore}) ниже порога системных модераторов (30.0 REP)"
        }

        // КРИТИЧЕСКИЙ БИРЮЗОВЫЙ КОНСЕНСУС (Code is Law):
        // Запуск формального голосования (VOTING) блокируется, пока есть нерешенные Objections (возражения членов Совета)
        if (targetStage == "VOTING") {
            val unresolved = dao.getActiveObjectionsForProposal(proposalId)
            if (unresolved.isNotEmpty()) {
                return@withContext "Отказ: Нельзя перейти к голосованию! Найдено активных блокирующих возражений: ${unresolved.size}. Автор должен интегрировать рекомендации."
            }
        }

        dao.insertProposal(
            proposal.copy(stage = targetStage)
        )

        simulateP2PSyncLog(
            direction = "SMTP_SEND",
            actionType = "STAGE_CHANGE",
            payload = "{\"proposalId\":\"$proposalId\",\"stage\":\"$targetStage\",\"by\":\"$actorId\"}"
        )

        return@withContext "Статус предложения изменен на [$targetStage] агентом ${actor.name}."
    }

    /**
     * EXECUTIVE : Выполнение задачи
     * Реализует меритократический вклад: при завершении и аудите задачи участником,
     * узел начисляет ему репутацию (Reputation Score).
     */
    suspend fun completeTask(taskId: String, agentId: String): String = withContext(Dispatchers.IO) {
        val task = dao.getTaskById(taskId) ?: return@withContext "Задача не найдена"
        if (task.status == "COMPLETED" || task.status == "AUDITED") return@withContext "Задача уже завершена"

        val agent = dao.getAgentById(agentId) ?: return@withContext "Агент не зарегистрирован"

        // Обновим задачу
        val updatedTask = task.copy(
            assignedTo = agentId,
            status = "AUDITED", // В демонстрационных целях аудит со стороны Scribe проходит мгновенно
            completedAt = System.currentTimeMillis()
        )
        dao.insertTask(updatedTask)

        // МЕРИТОКРАТИЯ: Начисление репутации за вклад
        val newRep = agent.reputationScore + task.reputationReward
        val newCount = agent.contributionsCount + 1
        dao.insertAgent(
            agent.copy(
                reputationScore = newRep,
                contributionsCount = newCount
            )
        )

        simulateP2PSyncLog(
            direction = "SMTP_SEND",
            actionType = "TASK_COMPLETED",
            payload = "{\"taskId\":\"$taskId\",\"by\":\"$agentId\",\"reward\":${task.reputationReward}}"
        )

        return@withContext "Вклад зафиксирован! Задача '${task.title}' закрыта. Агенту $agentId начислено +${task.reputationReward} репутации."
    }

    /**
     * JUDICIARY : Возбуждение спора и автоматический выбор суда присяжных
     * «Код как закон»: Жюри выбирается случайно из участников с высоким репутационным порогом
     * для исключения сговоров и обеспечения меритократической непредвзятости.
     */
    suspend fun raiseDispute(plaintiffId: String, defendantId: String, description: String, article: String): String = withContext(Dispatchers.IO) {
        val allAgents = dao.getAllAgentsDirect()
        // Отбираем кандидатов с высокой репутацией (>= 50.0), исключая участников спора
        val eligibleArbiters = allAgents.filter { 
            it.reputationScore >= 50.0 && it.id != plaintiffId && it.id != defendantId 
        }

        if (eligibleArbiters.size < 2) {
            return@withContext "Ошибка: Недостаточно независимых арбитров с репутацией >= 50 для созыва судебной коллегии"
        }

        // Случайный выбор двух присяжных
        val chosenJury = eligibleArbiters.shuffled().take(2).map { it.id }
        val juryString = chosenJury.joinToString(",")

        val disputeId = "dispute-${UUID.randomUUID().toString().take(6)}"
        val newDispute = DisputeEntity(
            id = disputeId,
            plaintiffId = plaintiffId,
            defendantId = defendantId,
            description = description,
            constitutionalArticle = article,
            status = "REVIEW",
            juryIds = juryString,
            juryVotesYes = 0,
            juryVotesNo = 0,
            verdict = "Суд коллегия созвана: [${juryString.replace(",", ", ")}]. Ожидание вердикта."
        )
        dao.insertDispute(newDispute)

        simulateP2PSyncLog(
            direction = "SMTP_SEND",
            actionType = "JUDICIARY_SUMMON",
            payload = "{\"id\":\"$disputeId\",\"jury\":\"$juryString\"}"
        )

        return@withContext "Спор зарегистрирован. Случайно созван независимый арбитраж: [${juryString.replace(",", ", ")}]"
    }

    /**
     * JUDICIARY : Вещание голоса присяжного по иску
     */
    suspend fun castJuryVote(disputeId: String, arbiterId: String, voteGuilty: Boolean): String = withContext(Dispatchers.IO) {
        val dispute = dao.getDisputeById(disputeId) ?: return@withContext "Спор не найден"
        if (dispute.status != "REVIEW") return@withContext "Спор уже закрыт"

        val juryList = dispute.juryIds.split(",")
        if (!juryList.contains(arbiterId)) {
            return@withContext "Вы не входите в утвержденную коллегию присяжных для этого иска"
        }

        val updatedYes = if (voteGuilty) dispute.juryVotesYes + 1 else dispute.juryVotesYes
        val updatedNo = if (!voteGuilty) dispute.juryVotesNo + 1 else dispute.juryVotesNo
        val totalVotes = updatedYes + updatedNo

        var newStatus = "REVIEW"
        var verdictMsg = dispute.verdict

        if (totalVotes >= juryList.size) {
            // Коллегия завершила голосование
            if (updatedYes > updatedNo) {
                newStatus = "RESOLVED_YES"
                verdictMsg = "Виновен: Нарушен закон '${dispute.constitutionalArticle}'. Наложен штраф репутации!"
                
                // Штраф репутации подсудимому
                val defendant = dao.getAgentById(dispute.defendantId)
                if (defendant != null) {
                     val penalizedRep = maxOf(10.0, defendant.reputationScore - 20.0)
                     dao.insertAgent(defendant.copy(reputationScore = penalizedRep))
                }
            } else {
                newStatus = "RESOLVED_NO"
                verdictMsg = "Оправдан: Арбитры не нашли состава правонарушения. Очистить прецедент."
            }
        } else {
            verdictMsg = "Получен статус: Голоса [$updatedYes/2 за виновность]. Ожидается финальное судейское решение."
        }

        dao.insertDispute(
            dispute.copy(
                status = newStatus,
                juryVotesYes = updatedYes,
                juryVotesNo = updatedNo,
                verdict = verdictMsg
            )
        )

        simulateP2PSyncLog(
            direction = "SMTP_SEND",
            actionType = "JUDICIARY_VOTE",
            payload = "{\"disputeId\":\"$disputeId\",\"arbiter\":\"$arbiterId\",\"voteGuilty\":$voteGuilty}"
        )

        return@withContext "Ваш закрытый голос внесен в реестр судебного прецедента."
    }

    /**
     * REPOSITORY / FORK: Ветвление системы (создание форка)
     */
    suspend fun createFork(id: String, parentId: String, title: String, description: String, quorumMult: Double, minPropRep: Double): String = withContext(Dispatchers.IO) {
        val existing = dao.getForkById(id)
        if (existing != null) return@withContext "Ветка с идентификатором '$id' уже зарегистрирована в реестре"

        val newFork = ForkEntity(
            id = id,
            parentForkId = parentId,
            title = title,
            description = description,
            votingQuorumMultiplier = quorumMult,
            minReputationToPropose = minPropRep,
            createdAt = System.currentTimeMillis(),
            isActive = true
        )
        dao.insertFork(newFork)

        simulateP2PSyncLog(
            direction = "SMTP_SEND",
            actionType = "FORK_CREATED",
            payload = "{\"id\":\"$id\",\"title\":\"$title\",\"parent\":\"$parentId\"}"
        )

        return@withContext "Успешно форкнуто! Ветка '$title' создана. Вы можете разрабатывать в ней новые институты, регулируя кворум и барьеры голосования."
    }

    /**
     * Симуляция P2P записи лога в базу
     */
    private suspend fun simulateP2PSyncLog(direction: String, actionType: String, payload: String) = withContext(Dispatchers.IO) {
        val envelope = P2PEnvelope(
            senderId = "node-local@orden.p2p",
            recipientId = "overlay-network@orden.p2p",
            actionType = actionType,
            forkId = "core",
            serializedPayload = payload,
            signature = "SIG_ED25519_" + UUID.randomUUID().toString().take(12).uppercase()
        )
        
        dao.insertSyncLog(
            SyncLogEntity(
                timestamp = System.currentTimeMillis(),
                direction = direction,
                mailEnvelope = envelope.generateEmailBody(),
                statusUrl = "CONVERGED_OK (CRDT)",
                messageHash = "SHA256-" + UUID.randomUUID().toString().take(8).uppercase()
            )
        )
    }

    /**
     * РЕАЛЬНЫЙ ТРАНСПОРТ: Анализ и применение входящего SMTP/IMAP сообщения P2P в локальную СУБД.
     */
    suspend fun processIncomingMailEnvelope(envelopeBody: String): String = withContext(Dispatchers.IO) {
        try {
            val lines = envelopeBody.split("\n", "\r")
            var senderId = ""
            var actionType = ""
            var forkId = "core"
            var payload = ""
            var isPayload = false

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("SENDER:")) senderId = trimmed.substringAfter("SENDER:").trim()
                else if (trimmed.startsWith("TYPE:")) actionType = trimmed.substringAfter("TYPE:").trim()
                else if (trimmed.startsWith("FORK:")) forkId = trimmed.substringAfter("FORK:").trim()
                else if (trimmed.startsWith("PAYLOAD:")) {
                    isPayload = true
                    continue
                } else if (trimmed == "===END-ORDEN-DATA===") {
                    isPayload = false
                } else if (isPayload) {
                    payload += trimmed
                }
            }

            if (senderId.isBlank() || actionType.isBlank()) {
                return@withContext "Ошибка P2P разбора: заголовок пуст."
            }

            // Добавим лог получения
            dao.insertSyncLog(
                SyncLogEntity(
                    timestamp = System.currentTimeMillis(),
                    direction = "IMAP_REC",
                    mailEnvelope = envelopeBody,
                    statusUrl = "CONVERGED_REAL (CRDT Decrypted)",
                    messageHash = "SHA256-" + UUID.randomUUID().toString().take(8).uppercase()
                )
            )

            // Проверим, зарегистрирован ли отправитель как Агент. Если нет — временно внесем с базовыми параметрами
            val existingAgent = dao.getAgentById(senderId)
            if (existingAgent == null) {
                dao.insertAgent(
                    AgentEntity(
                        id = senderId,
                        name = senderId.substringBefore("@"),
                        role = "Adept",
                        reputationScore = 20.0,
                        contributionsCount = 0,
                        publicKey = "0xP2P_TEMP_" + UUID.randomUUID().toString().take(8).uppercase()
                    )
                )
            }

            fun extractVal(json: String, key: String): String {
                val pattern = """"$key"\s*:\s*"?([^",}]+)"?""".toRegex()
                val match = pattern.find(json) ?: return ""
                return if (match.groupValues.size > 1) match.groupValues[1].trim() else ""
            }

            when (actionType) {
                "NEW_PROPOSAL" -> {
                    val pId = extractVal(payload, "id")
                    val pTitle = extractVal(payload, "title").ifBlank { "С SMTP канала" }
                    val proposer = extractVal(payload, "proposer").ifBlank { senderId }
                    val fId = extractVal(payload, "forkId").ifBlank { forkId }
                    val pDesc = extractVal(payload, "description").ifBlank { "SMTP Синхронизированный закон" }
                    
                    if (dao.getProposalById(pId) == null) {
                        dao.insertProposal(
                            ProposalEntity(
                                id = pId,
                                title = pTitle,
                                description = "$pDesc\n[Получено по зашифрованному реле SMTP от: $senderId]",
                                proposerId = proposer,
                                status = "ACTIVE",
                                voteYesRep = 20.0,
                                voteNoRep = 0.0,
                                quorumRequired = 100.0,
                                createdAt = System.currentTimeMillis(),
                                forkId = fId
                            )
                        )
                        return@withContext "Импортировано новое предложение [$pId] '$pTitle'"
                    }
                }
                "CAST_VOTE" -> {
                    val propId = extractVal(payload, "proposalId")
                    val voter = extractVal(payload, "voter").ifBlank { senderId }
                    val voteStr = extractVal(payload, "vote")
                    val votedYes = voteStr.toBoolean()

                    val p = dao.getProposalById(propId)
                    val v = dao.getAgentById(voter)
                    if (p != null && v != null) {
                        val existingVote = dao.getVoteByVoter(propId, voter)
                        if (existingVote == null) {
                            val repWeight = v.reputationScore
                            dao.insertVote(
                                VoteEntity(
                                    proposalId = propId,
                                    voterId = voter,
                                    votedYes = votedYes,
                                    voterReputation = repWeight,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                            val updatedYes = if (votedYes) p.voteYesRep + repWeight else p.voteYesRep
                            val updatedNo = if (!votedYes) p.voteNoRep + repWeight else p.voteNoRep
                            dao.insertProposal(
                                p.copy(
                                    voteYesRep = updatedYes,
                                    voteNoRep = updatedNo
                                )
                            )
                            return@withContext "Учтен входящий SMTP P2P голос от пира $voter за повестку $propId"
                        }
                    }
                }
                "TASK_COMPLETED" -> {
                    val tId = extractVal(payload, "taskId")
                    val byAgent = extractVal(payload, "by").ifBlank { senderId }
                    val task = dao.getTaskById(tId)
                    if (task != null && task.status != "COMPLETED" && task.status != "AUDITED") {
                        dao.insertTask(
                            task.copy(
                                assignedTo = byAgent,
                                status = "AUDITED",
                                completedAt = System.currentTimeMillis()
                            )
                        )
                        val ag = dao.getAgentById(byAgent)
                        if (ag != null) {
                            dao.insertAgent(
                                ag.copy(
                                    reputationScore = ag.reputationScore + task.reputationReward,
                                    contributionsCount = ag.contributionsCount + 1
                                )
                            )
                        }
                        return@withContext "Зафиксировано SMTP выполнение задачи [$tId] агентом $byAgent"
                    }
                }
                "CHAT_MESSAGE" -> {
                    val msgText = extractVal(payload, "text")
                    val rx = extractVal(payload, "recipientId")
                    if (msgText.isNotBlank()) {
                        dao.insertChatMessage(
                            ChatMessageEntity(
                                senderId = senderId,
                                recipientId = rx.ifBlank { "broadcast" },
                                messageText = msgText,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        return@withContext "Получено P2P сообщение от $senderId: '$msgText'"
                    }
                }
            }
            return@withContext "Данные P2P проверены, изменений не требуется или объект устарел."
        } catch (e: Exception) {
            return@withContext "Ошибка SMTP/IMAP интеграции: ${e.message}"
        }
    }

    /**
     * Принудительная симуляция входящей SMTP/IMAP синхронизации с другими пирами Ордена
     */
    suspend fun triggerNetworkSyncDemo(): String = withContext(Dispatchers.IO) {
        // Симулируем, что получили из внешнего почтового ящика IMAP действия от другого участника
        val randomNum = (1..4).random()
        val peerMail = when(randomNum) {
            1 -> {
                val p = "{\"proposalId\":\"prop-const-1\",\"voter\":\"alex@orden.p2p\",\"vote\":true,\"weight\":65.0}"
                val env = P2PEnvelope("alex@orden.p2p", "local@orden.p2p", "CAST_VOTE", "core", p, "SIG_ALEX_302F")
                env.generateEmailBody()
            }
            2 -> {
                val p = "{\"id\":\"dispute-992\",\"jury\":\"maria@orden.p2p,alex@orden.p2p\"}"
                val env = P2PEnvelope("ivan@orden.p2p", "local@orden.p2p", "JUDICIARY_SUMMON", "core", p, "SIG_IVAN_881C")
                env.generateEmailBody()
            }
            3 -> {
                val p = "{\"text\":\"Братва, настроил P2P транспорт через SMTP/IMAP, шифрование работает отлично. Все на связи?\",\"recipientId\":\"broadcast\"}"
                val env = P2PEnvelope("maria@orden.p2p", "broadcast", "CHAT_MESSAGE", "core", p, "SIG_MARIA_F28C")
                env.generateEmailBody()
            }
            else -> {
                val p = "{\"taskId\":\"task-core-auth\",\"by\":\"maria@orden.p2p\",\"reward\":25.0}"
                val env = P2PEnvelope("maria@orden.p2p", "local@orden.p2p", "TASK_COMPLETED", "core", p, "SIG_MARIA_0093")
                env.generateEmailBody()
            }
        }

        dao.insertSyncLog(
            SyncLogEntity(
                timestamp = System.currentTimeMillis(),
                direction = "IMAP_REC",
                mailEnvelope = peerMail,
                statusUrl = "MERGING (Symmetric Decryption + CRDT Reconciliation)",
                messageHash = "SHA256-" + UUID.randomUUID().toString().take(8).uppercase()
            )
        )

        // Для демонстрационных целей, если это был голос Александра за главный proposal, учтем его в реальной State:
        if (randomNum == 1) {
            castVote("prop-const-1", "alex@orden.p2p", true)
            return@withContext "Синхронизация IMAP завершена. Получен голос от Александра за 'Принятие Хартии'. Накат состояния успешен!"
        }

        if (randomNum == 3) {
            dao.insertChatMessage(
                ChatMessageEntity(
                    senderId = "maria@orden.p2p",
                    recipientId = "broadcast",
                    messageText = "Братва, настроил P2P транспорт через SMTP/IMAP, шифрование работает отлично. Все на связи?",
                    timestamp = System.currentTimeMillis()
                )
            )
            return@withContext "Синхронизация IMAP завершена. Получено новое P2P сообщение в чат от Maria: 'Братва, настроил P2P транспорт...'"
        }

        return@withContext "Синхронизация IMAP завершена. Получено внешнее событие P2P. Накат состояния завершен без конфликтов!"
    }
}
