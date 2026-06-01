package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. Identity / Agent Entity
@Entity(tableName = "agents")
data class AgentEntity(
    @PrimaryKey val id: String, // "alex@orden.p2p", "leonid@orden.p2p", etc.
    val name: String,
    val role: String, // "Grandmaster", "Knight", "Adept", "Arbiter", "Scribe"
    val reputationScore: Double,
    val contributionsCount: Int,
    val publicKey: String
)

// 2. Council / Proposal Entity
@Entity(tableName = "proposals")
data class ProposalEntity(
    @PrimaryKey val id: String, // UUID
    val title: String,
    val description: String,
    val proposerId: String,
    val status: String, // "ACTIVE", "ACCEPTED", "REJECTED"
    val voteYesRep: Double,
    val voteNoRep: Double,
    val quorumRequired: Double,
    val createdAt: Long,
    val forkId: String, // Belongs to a specific Fork/Version of system!
    val stage: String = "VOTING", // "DRAFT", "DEBATE", "VOTING", "ACCEPTED", "REJECTED"
    val objectionsCount: Int = 0,
    val amendmentsCount: Int = 0
)

// 3. Election / Voting Activity Entity
@Entity(tableName = "votes")
data class VoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val proposalId: String,
    val voterId: String,
    val votedYes: Boolean,
    val voterReputation: Double,
    val timestamp: Long
)

// 4. Executive / Task Entity
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val proposalId: String?, // Spawned by this proposal if applicable
    val title: String,
    val description: String,
    val assignedTo: String?, // Agent ID
    val status: String, // "ASSIGNED", "PROGRESS", "COMPLETED", "AUDITED"
    val reputationReward: Double,
    val completedAt: Long?
)

// 5. Judiciary / Dispute Entity
@Entity(tableName = "disputes")
data class DisputeEntity(
    @PrimaryKey val id: String,
    val plaintiffId: String,
    val defendantId: String,
    val description: String,
    val constitutionalArticle: String,
    val status: String, // "REVIEW", "RESOLVED_YES" (guilty), "RESOLVED_NO" (acquitted)
    val juryIds: String, // Comma separated agent IDs
    val juryVotesYes: Int,
    val juryVotesNo: Int,
    val verdict: String
)

// 6. Repository / Fork Entity
@Entity(tableName = "forks")
data class ForkEntity(
    @PrimaryKey val id: String, // "core", "decentral-repu", etc.
    val parentForkId: String?,
    val title: String,
    val description: String,
    val votingQuorumMultiplier: Double,
    val minReputationToPropose: Double,
    val createdAt: Long,
    val isActive: Boolean
)

// 7. P2P Transport Sync Log
@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val direction: String, // "SMTP_SEND", "IMAP_REC"
    val mailEnvelope: String, // Simulated email envelope body
    val statusUrl: String, // CRDT Merging status
    val messageHash: String
)

@Dao
interface OrdenDao {
    // Agents
    @Query("SELECT * FROM agents ORDER BY reputationScore DESC")
    fun getAllAgents(): Flow<List<AgentEntity>>

    @Query("SELECT * FROM agents ORDER BY reputationScore DESC")
    suspend fun getAllAgentsDirect(): List<AgentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgent(agent: AgentEntity)

    @Query("SELECT * FROM agents WHERE id = :id")
    suspend fun getAgentById(id: String): AgentEntity?

    // Proposals
    @Query("SELECT * FROM proposals WHERE forkId = :forkId ORDER BY createdAt DESC")
    fun getProposalsForFork(forkId: String): Flow<List<ProposalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProposal(proposal: ProposalEntity)

    @Query("SELECT * FROM proposals WHERE id = :id")
    suspend fun getProposalById(id: String): ProposalEntity?

    // Votes
    @Query("SELECT * FROM votes WHERE proposalId = :proposalId")
    fun getVotesForProposal(proposalId: String): Flow<List<VoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVote(vote: VoteEntity)

    @Query("SELECT * FROM votes WHERE proposalId = :proposalId AND voterId = :voterId")
    suspend fun getVoteByVoter(proposalId: String, voterId: String): VoteEntity?

    // Tasks
    @Query("SELECT * FROM tasks ORDER BY completedAt ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): TaskEntity?

    // Disputes
    @Query("SELECT * FROM disputes ORDER BY id DESC")
    fun getAllDisputes(): Flow<List<DisputeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDispute(dispute: DisputeEntity)

    @Query("SELECT * FROM disputes WHERE id = :id")
    suspend fun getDisputeById(id: String): DisputeEntity?

    // Forks
    @Query("SELECT * FROM forks ORDER BY createdAt ASC")
    fun getAllForks(): Flow<List<ForkEntity>>

    @Query("SELECT * FROM forks ORDER BY createdAt ASC")
    suspend fun getAllForksDirect(): List<ForkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFork(fork: ForkEntity)

    @Query("SELECT * FROM forks WHERE id = :id")
    suspend fun getForkById(id: String): ForkEntity?

    // Sync Logs
    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT 30")
    fun getSyncLogs(): Flow<List<SyncLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncLog(log: SyncLogEntity)

    // Chat messages
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllChatMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    // Debate Arguments & Objections
    @Query("SELECT * FROM debate_arguments WHERE proposalId = :proposalId ORDER BY timestamp ASC")
    fun getArgumentsForProposal(proposalId: String): Flow<List<DebateArgumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebateArgument(argument: DebateArgumentEntity)

    @Query("UPDATE debate_arguments SET isResolved = :resolved, resolutionText = :resText WHERE id = :argId")
    suspend fun updateObjectionResolution(argId: String, resolved: Boolean, resText: String)

    @Query("SELECT * FROM debate_arguments WHERE proposalId = :proposalId AND type = 'OBJECTION' AND isResolved = 0")
    suspend fun getActiveObjectionsForProposal(proposalId: String): List<DebateArgumentEntity>
}

// 8. P2P Chat Message Entity
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderId: String,
    val recipientId: String,
    val messageText: String,
    val timestamp: Long,
    val isSystem: Boolean = false
)

// 9. Debate Argument Entity
@Entity(tableName = "debate_arguments")
data class DebateArgumentEntity(
    @PrimaryKey val id: String, // UUID
    val proposalId: String,
    val authorId: String,
    val authorName: String,
    val text: String,
    val type: String, // "PRO", "CON", "SUGGESTION", "OBJECTION"
    val timestamp: Long,
    val isResolved: Boolean = false, // True if proposer has addressed this objection
    val resolutionText: String = "" // Explanation of how objection was resolved
)

@Database(
    entities = [
        AgentEntity::class,
        ProposalEntity::class,
        VoteEntity::class,
        TaskEntity::class,
        DisputeEntity::class,
        ForkEntity::class,
        SyncLogEntity::class,
        ChatMessageEntity::class,
        DebateArgumentEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class OrdenDatabase : RoomDatabase() {
    abstract fun ordenDao(): OrdenDao
}
