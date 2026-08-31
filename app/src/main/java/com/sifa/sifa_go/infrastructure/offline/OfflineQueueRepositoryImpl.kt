package com.sifa.sifa_go.infrastructure.offline

import com.sifa.sifa_go.data.local.dao.PendingInfraccionDao
import com.sifa.sifa_go.data.local.mapping.PendingInfraccionMapper
import com.sifa.sifa_go.data.model.InfraccionCreateRequest
import com.sifa.sifa_go.domain.model.PendingInfraccion
import com.sifa.sifa_go.domain.repository.OfflineQueueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementación de [OfflineQueueRepository] sobre SQLite (Room).
 * Es la fuente de verdad local para las infracciones pendientes de envío.
 */
class OfflineQueueRepositoryImpl(
    private val dao: PendingInfraccionDao
) : OfflineQueueRepository {

    override suspend fun enqueue(request: InfraccionCreateRequest, imagePaths: List<String>): Long {
        return dao.insert(PendingInfraccionMapper.toEntity(request, imagePaths))
    }

    override suspend fun getPending(): List<PendingInfraccion> {
        return PendingInfraccionMapper.toDomainList(dao.getPending())
    }

    override suspend fun markSynced(id: Long) {
        dao.markSynced(id)
    }

    override suspend fun markFailed(id: Long, reason: String?) {
        dao.markFailed(id, reason)
    }

    override suspend fun getFailed(): List<PendingInfraccion> {
        return PendingInfraccionMapper.toDomainList(dao.getFailed())
    }

    override fun observeSyncableQueue(): Flow<List<PendingInfraccion>> {
        return dao.observeSyncableQueue().map(PendingInfraccionMapper::toDomainList)
    }
}
