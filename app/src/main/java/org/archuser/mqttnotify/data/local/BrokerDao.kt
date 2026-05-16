package org.archuser.mqttnotify.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BrokerDao {
    @Query("SELECT * FROM brokers ORDER BY label COLLATE NOCASE")
    fun observeAll(): Flow<List<BrokerEntity>>

    @Query("SELECT * FROM brokers WHERE id = :id")
    suspend fun getById(id: Long): BrokerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BrokerEntity): Long

    @Update
    suspend fun update(entity: BrokerEntity)

    @Delete
    suspend fun delete(entity: BrokerEntity)
}
