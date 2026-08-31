package com.sifa.sifa_go.domain.repository

import com.sifa.sifa_go.data.model.TipoInfraccionResponse

/**
 * Contrato de acceso a la caché local de tipologías de infracción.
 * Permite a la app mostrar el catálogo aunque no haya conexión, usando los
 * datos guardados la última vez que se sincronizó desde el backend.
 */
interface TipoInfraccionRepository {

    /** Devuelve las tipologías guardadas localmente (caché), ordenadas por nombre. */
    suspend fun getCached(): List<TipoInfraccionResponse>

    /** Guarda/actualiza el catálogo local. Upsert por id: reemplaza o agrega cada fila. */
    suspend fun saveAll(tipos: List<TipoInfraccionResponse>)
}
