package org.archuser.mqttnotify.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RetentionPolicyDao {
    @Query("SELECT * FROM retention_policies WHERE broker_id = :brokerId AND topic_filter = :topic LIMIT 1")
    suspend fun getTopicPolicy(brokerId: Long, topic: String): RetentionPolicyEntity?

    @Query("SELECT * FROM retention_policies WHERE broker_id = :brokerId AND topic_filter IS NULL LIMIT 1")
    suspend fun getBrokerDefault(brokerId: Long): RetentionPolicyEntity?

    @Query("SELECT * FROM retention_policies WHERE broker_id IS NULL AND topic_filter IS NULL LIMIT 1")
    suspend fun getGlobalDefault(): RetentionPolicyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RetentionPolicyEntity): Long
}
