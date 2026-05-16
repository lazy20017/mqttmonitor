package org.archuser.mqttnotify.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicSubscriptionDao {
    @Query("SELECT * FROM topic_subscriptions WHERE broker_id = :brokerId ORDER BY topic_filter")
    fun observeForBroker(brokerId: Long): Flow<List<TopicSubscriptionEntity>>

    @Query("SELECT * FROM topic_subscriptions WHERE broker_id = :brokerId AND enabled = 1")
    suspend fun getEnabledForBroker(brokerId: Long): List<TopicSubscriptionEntity>

    @Query("SELECT * FROM topic_subscriptions WHERE broker_id = :brokerId")
    suspend fun getAllForBroker(brokerId: Long): List<TopicSubscriptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TopicSubscriptionEntity): Long

    @Delete
    suspend fun delete(entity: TopicSubscriptionEntity)

    @Query("DELETE FROM topic_subscriptions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
