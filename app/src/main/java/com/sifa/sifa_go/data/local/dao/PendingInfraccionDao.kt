package com.sifa.sifa_go.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sifa.sifa_go.data.local.entity.PendingInfraccionEntity
import com.sifa.sifa_go.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingInfraccionDao {

    @Insert
    suspend fun insert(entity: PendingInfraccionEntity): Long

    @Query("SELECT * FROM pending_infracciones WHERE id = :id")
    suspend fun getById(id: Long): PendingInfraccionEntity?

    @Query("SELECT * FROM pending_infracciones WHERE status = :status ORDER BY createdAt ASC")
    suspend fun getByStatus(status: SyncStatus): List<PendingInfraccionEntity>

    @Query("SELECT * FROM pending_infracciones WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPending(): List<PendingInfraccionEntity>

    @Query("SELECT * FROM pending_infracciones WHERE status IN ('PENDING', 'FAILED') ORDER BY createdAt ASC")
    fun observeSyncableQueue(): Flow<List<PendingInfraccionEntity>>

    @Query("UPDATE pending_infracciones SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: SyncStatus)

    @Query("UPDATE pending_infracciones SET status = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("UPDATE pending_infracciones SET status = 'FAILED', failureReason = :reason WHERE id = :id")
    suspend fun markFailed(id: Long, reason: String? = null)

    @Query("SELECT * FROM pending_infracciones WHERE status = 'FAILED' ORDER BY createdAt DESC")
    suspend fun getFailed(): List<PendingInfraccionEntity>

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM pending_infracciones
            WHERE status IN ('PENDING', 'FAILED')
        )
    """)
    fun hasPending(): Flow<Boolean>

    @Query("DELETE FROM pending_infracciones WHERE id = :id")
    suspend fun deleteById(id: Long)
}
