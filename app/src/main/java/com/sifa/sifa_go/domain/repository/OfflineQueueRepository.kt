package com.sifa.sifa_go.domain.repository

import com.sifa.sifa_go.data.model.InfraccionCreateRequest
import com.sifa.sifa_go.domain.model.PendingInfraccion
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de la cola offline de infracciones.
 * La implementación concreta persiste en SQLite a través de Room.
 */
interface OfflineQueueRepository {

    /**
     * Guarda una infracción en la cola local con estado PENDING.
     * @return el id asignado en la base de datos.
     */
    suspend fun enqueue(request: InfraccionCreateRequest, imagePaths: List<String>): Long

    /** Devuelve las infracciones pendientes de envío, ordenadas FIFO. */
    suspend fun getPending(): List<PendingInfraccion>

    /**
     * Elimina físicamente una infracción ya sincronizada de la cola local.
     * Se usa tras un envío exitoso para no acumular registros que ocupen espacio.
     */
    suspend fun deleteById(id: Long)

    /** Marca una infracción como fallida (no se puede reconstruir el request o el backend rechazó). */
    suspend fun markFailed(id: Long, reason: String? = null)

    /** Devuelve las infracciones fallidas (con su [PendingInfraccion.status] FAILED) para diagnóstico. */
    suspend fun getFailed(): List<PendingInfraccion>

    /** Observa la cola sincronizable (pendientes + fallidas) para reflejarla en la UI. */
    fun observeSyncableQueue(): Flow<List<PendingInfraccion>>
}
