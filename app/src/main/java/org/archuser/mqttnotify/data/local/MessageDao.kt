package org.archuser.mqttnotify.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MessageEntity): Long

    @Query("SELECT * FROM messages WHERE broker_id = :brokerId ORDER BY received_at DESC LIMIT 500")
    fun observeForBroker(brokerId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    suspend fun getById(messageId: Long): MessageEntity?

    @Query("UPDATE messages SET is_unread = 0 WHERE id = :messageId")
    suspend fun markReadById(messageId: Long)

    @Query("UPDATE messages SET is_unread = 1 WHERE id = :messageId")
    suspend fun markUnreadById(messageId: Long)

    @Query("UPDATE messages SET is_unread = 0 WHERE broker_id = :brokerId")
    suspend fun markAllReadForBroker(brokerId: Long)

    @Query("UPDATE messages SET is_unread = 0 WHERE broker_id = :brokerId AND topic_filter = :topic")
    suspend fun markAllReadForTopic(brokerId: Long, topic: String)

    @Query("DELETE FROM messages WHERE broker_id = :brokerId AND topic_filter = :topic AND received_at < :cutoff")
    suspend fun deleteOlderThan(brokerId: Long, topic: String, cutoff: Long)

    @Query("SELECT COUNT(*) FROM messages WHERE broker_id = :brokerId AND topic_filter = :topic")
    suspend fun countForTopic(brokerId: Long, topic: String): Int

    @Query(
        "DELETE FROM messages WHERE id IN (" +
            "SELECT id FROM messages WHERE broker_id = :brokerId AND topic_filter = :topic " +
            "ORDER BY received_at DESC LIMIT -1 OFFSET :keep)"
    )
    suspend fun deleteOverflowForTopic(brokerId: Long, topic: String, keep: Int)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteById(messageId: Long)
}
