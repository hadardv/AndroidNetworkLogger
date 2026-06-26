package com.example.network_logger_lib.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * Data-access object for network logs.
 * All reads are ordered newest-first by [NetworkLogEntity.timestamp].
 */
@Dao
interface NetworkLogDao {

    @Insert
    suspend fun insert(log: NetworkLogEntity): Long

    @Query("SELECT * FROM network_logs ORDER BY timestamp DESC")
    suspend fun getAllOrderedByTimestamp(): List<NetworkLogEntity>
}
