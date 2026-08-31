package com.sifa.sifa_go.domain.model

import com.sifa.sifa_go.data.local.entity.SyncStatus
import com.sifa.sifa_go.data.model.InfraccionCreateRequest

/**
 * Modelo de dominio que representa una infracción encolada para envío offline.
 * Centraliza tanto el request tipado como las rutas de sus fotos de evidencia.
 */
data class PendingInfraccion(
    val id: Long,
    val request: InfraccionCreateRequest,
    val imagePaths: List<String>,
    val status: SyncStatus,
    val createdAt: Long,
    val failureReason: String? = null
) {
    val isPending: Boolean get() = status == SyncStatus.PENDING
}
