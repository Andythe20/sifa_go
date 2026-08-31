package com.sifa.sifa_go.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Estado de sincronización de una infracción encolada.
 * PENDING: falta enviarse al backend.
 * SYNCED:  enviada exitosamente.
 * FAILED:  no se pudo enviar (ej: las fotos ya no existen y no se puede rearmar el request).
 */
enum class SyncStatus {
    PENDING,
    SYNCED,
    FAILED
}

/**
 * Entidad Room que representa una infracción pendiente de envío en la cola offline.
 * Los campos replican [com.sifa.sifa_go.data.model.InfraccionCreateRequest].
 *
 * Las fotos de evidencia se guardan como rutas de archivo (String) y no como binario,
 * para mantener la base de datos liviana.
 */
@Entity(tableName = "pending_infracciones")
data class PendingInfraccionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val lugar: String,
    val fecha: String,
    val latitud: Float,
    val longitud: Float,
    val patenteVehiculo: String,
    val idTipoInfraccion: Int,
    val observaciones: String?,
    val fechaCitacion: String?,

    /** Rutas de archivo de las fotos de evidencia, serializadas como JSON. */
    val imagePathsJson: String,

    val status: SyncStatus = SyncStatus.PENDING,

    /** Motivo por el que falló el envío (solo si status == FAILED). Ayuda a diagnosticar. */
    val failureReason: String? = null,

    /** Timestamp (epoch millis) en que se encoló, para mantener orden FIFO. */
    val createdAt: Long = Instant.now().toEpochMilli()
)
