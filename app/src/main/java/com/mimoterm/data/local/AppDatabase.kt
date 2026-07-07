package com.mimoterm.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "command_history")
data class CommandHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val command: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sessionId: String
)

@Entity(tableName = "chat_history")
data class ChatHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val conversationId: String
)

@Dao
interface CommandHistoryDao {
    @Query("SELECT * FROM command_history WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT :limit")
    fun getCommands(sessionId: String, limit: Int = 100): Flow<List<CommandHistory>>

    @Insert
    suspend fun insert(command: CommandHistory)

    @Query("DELETE FROM command_history WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)
}

@Dao
interface ChatHistoryDao {
    @Query("SELECT * FROM chat_history WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessages(conversationId: String): Flow<List<ChatHistory>>

    @Insert
    suspend fun insert(message: ChatHistory)

    @Query("DELETE FROM chat_history WHERE conversationId = :conversationId")
    suspend fun deleteByConversation(conversationId: String)
}

@Database(
    entities = [CommandHistory::class, ChatHistory::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun commandHistoryDao(): CommandHistoryDao
    abstract fun chatHistoryDao(): ChatHistoryDao
}
