package com.example.domain

import java.util.UUID

/**
 * 1. IDENTITY / AGENT (Учетная запись участника)
 * Хранит репутационный вес и историю вклада. Защищен асимметричным ключом (P2P).
 */
data class Agent(
    val id: String, // leonid@orden.p2p
    val name: String,
    val role: String,
    val reputationScore: Double,
    val contributionsCount: Int,
    val publicKey: String
) {
    // В децентрализованных системах репутация - это вес голоса
    fun calculateVoteWeight(): Double {
        return reputationScore
    }
}

/**
 * 2. COUNCIL / PARLIAMENT (Совет)
 * Механизм формирования повестки и стратегического целеполагания.
 * Инкапсулирует правила голосования (Code is Law).
 */
data class Proposal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val proposerId: String,
    val status: ProposalStatus = ProposalStatus.ACTIVE,
    val voteYesRep: Double = 0.0,
    val voteNoRep: Double = 0.0,
    val quorumRequired: Double = 100.0, // Суммарная репутация для кворума
    val createdAt: Long = System.currentTimeMillis(),
    val forkId: String = "core"
) {
    fun isApproved(): Boolean {
        val totalVotesWeight = voteYesRep + voteNoRep
        if (totalVotesWeight < quorumRequired) return false
        return voteYesRep > voteNoRep
    }
}

enum class ProposalStatus {
    ACTIVE, ACCEPTED, REJECTED
}

/**
 * 3. ELECTION / VOTING (Избирательная комиссия)
 * Проверка легитимности голоса. Сибил-фильтр через репутационный ценз.
 */
class ElectionProtocol {
    companion object {
        /**
         * Сибил-защита: Голоса участников с репутацией ниже порога (Sybil Threshold) либо игнорируются,
         * либо их вес пренебрежимо мал.
         */
        fun validateVote(voter: Agent, minReputationRequired: Double): Boolean {
            return voter.reputationScore >= minReputationRequired
        }

        /**
         * Меритократический подсчет голосов: Вес голоса пропорционален репутации.
         */
        fun applyWeightedVote(
            proposal: Proposal,
            voter: Agent,
            voteYes: Boolean
        ): Proposal {
            val weight = voter.calculateVoteWeight()
            return if (voteYes) {
                proposal.copy(voteYesRep = proposal.voteYesRep + weight)
            } else {
                proposal.copy(voteNoRep = proposal.voteNoRep + weight)
            }
        }
    }
}

/**
 * 4. EXECUTIVE / ADMINISTRATION (Управа)
 * Выбираемый исполнительный орган. Берет задачи (роли),
 * которые автоматически создаются при утверждении решений Советом.
 */
data class ExecutiveTask(
    val id: String = UUID.randomUUID().toString(),
    val proposalId: String?,
    val title: String,
    val description: String,
    val assignedTo: String?,
    val status: TaskStatus = TaskStatus.ASSIGNED,
    val reputationReward: Double = 10.0,
    val completedAt: Long? = null
)

enum class TaskStatus {
    ASSIGNED, PROGRESS, COMPLETED, AUDITED
}

/**
 * 5. JUDICIARY / ARBITRATION (Суд/Арбитраж)
 * Разрешение споров на основе Конституции (этического кодекса) прецедентным методом.
 * Жюри присяжных выбирается случайно среди участников с высоким репутационным порогом.
 */
data class Dispute(
    val id: String = UUID.randomUUID().toString(),
    val plaintiffId: String,
    val defendantId: String,
    val description: String,
    val constitutionalArticle: String, // Индекс статьи Конституции
    val status: DisputeStatus = DisputeStatus.REVIEW,
    val juryIds: List<String> = emptyList(), // ID присяжных
    val juryVotesYes: Int = 0, // Виновен
    val juryVotesNo: Int = 0,  // Оправдан
    val verdict: String = "Ожидает голосования судей"
)

enum class DisputeStatus {
    REVIEW, RESOLVED_YES, RESOLVED_NO
}

class JudiciaryProtocol {
    companion object {
        /**
         * Выбор присяжных (Jury) случайным образом из списка квалифицированных агентов для нейтрализации сговора.
         */
        fun selectJury(candidates: List<Agent>, count: Int = 3): List<String> {
            return candidates
                .filter { it.reputationScore >= 50.0 } // Только доверенные участники с высоким весом
                .shuffled()
                .take(count)
                .map { it.id }
        }

        fun resolveDispute(dispute: Dispute): Dispute {
            val totalVotes = dispute.juryVotesYes + dispute.juryVotesNo
            if (totalVotes < dispute.juryIds.size) {
                return dispute // Рано закрывать
            }
            return if (dispute.juryVotesYes > dispute.juryVotesNo) {
                dispute.copy(
                    status = DisputeStatus.RESOLVED_YES,
                    verdict = "Виновен: Нарушена статья ${dispute.constitutionalArticle}. Применить штраф репутации!"
                )
            } else {
                dispute.copy(
                    status = DisputeStatus.RESOLVED_NO,
                    verdict = "Оправдан: Действия соответствуют нормативам Ордена."
                )
            }
        }
    }
}

/**
 * 6. REPOSITORY / FORK (Репозиторий)
 * Механизм форков конституции/правил сообщества во избежание окаменения и авторитаризма.
 */
data class SystemFork(
    val id: String, // "core-v2", "libertarian-orden"
    val parentForkId: String?,
    val title: String,
    val description: String,
    val votingQuorumMultiplier: Double = 1.0, // Форк меняет веса законов!
    val minReputationToPropose: Double = 10.0,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

/**
 * P2P SMTP/IMAP TRANSPORT PROTOCOLS (Синхронизация без серверов)
 * Описание протоколов обмена данными на базе почтовой сети:
 * Каждый узел использует существующую SMTP/IMAP инфраструктуру как транспортный распределенный уровень.
 * Данные пакуются в криптографически подписанные почтовые конверты.
 * Конфликты разрешаются локально получателем на базе правил Merge (как в Git или CRDT).
 */
data class P2PEnvelope(
    val senderId: String,
    val recipientId: String,
    val actionType: String, // "NEW_PROPOSAL", "CAST_VOTE", "REPUTATION_UPDATE"
    val forkId: String,
    val serializedPayload: String, // JSON закодированный объект
    val signature: String, // Локальная подпись отправителя к закрытым ключом
    val timestamp: Long = System.currentTimeMillis()
) {
    fun generateEmailBody(): String {
        return """
            ORDEN-P2P-PROTOCOL-V1
            SENDER: $senderId
            TYPE: $actionType
            FORK: $forkId
            TIMESTAMP: $timestamp
            SIGNATURE: $signature
            
            PAYLOAD:
            $serializedPayload
            ===END-ORDEN-DATA===
        """.trimIndent()
    }
}
