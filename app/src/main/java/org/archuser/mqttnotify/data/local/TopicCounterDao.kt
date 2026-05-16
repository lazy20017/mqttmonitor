package org.archuser.mqttnotify.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TopicCounterDao {
    @Query("SELECT * FROM topic_counters WHERE broker_id = :brokerId AND topic_filter = :topic")
    suspend fun getCounter(brokerId: Long, topic: String): TopicCounterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TopicCounterEntity)

    @Query("SELECT COALESCE(SUM(unread_count), 0) FROM topic_counters WHERE broker_id = :brokerId")
    suspend fun unreadCountForBroker(brokerId: Long): Int

    @Query("UPDATE topic_counters SET unread_count = 0 WHERE broker_id = :brokerId AND topic_filter = :topic")
    suspend fun resetUnreadForTopic(brokerId: Long, topic: String)

    @Query("UPDATE topic_counters SET unread_count = 0 WHERE broker_id = :brokerId")
    suspend fun resetUnreadForBroker(brokerId: Long)

    @Query("UPDATE topic_counters SET unread_count = unread_count + 1 WHERE broker_id = :brokerId AND topic_filter = :topic")
    suspend fun incrementUnreadForTopic(brokerId: Long, topic: String)

    @Query("UPDATE topic_counters SET unread_count = MAX(unread_count - 1, 0) WHERE broker_id = :brokerId AND topic_filter = :topic")
    suspend fun decrementUnreadForTopic(brokerId: Long, topic: String)

    @Query(
        "UPDATE topic_counters SET " +
            "unread_count = MAX(unread_count - :unreadDelta, 0), " +
            "total_count = MAX(total_count - 1, 0) " +
            "WHERE broker_id = :brokerId AND topic_filter = :topic"
    )
    suspend fun decrementCountsForDeletedMessage(brokerId: Long, topic: String, unreadDelta: Int)
}
