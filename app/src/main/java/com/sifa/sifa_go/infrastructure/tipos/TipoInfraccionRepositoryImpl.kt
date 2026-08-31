package com.sifa.sifa_go.infrastructure.tipos

import com.sifa.sifa_go.data.local.dao.TipoInfraccionDao
import com.sifa.sifa_go.data.local.mapping.TipoInfraccionMapper
import com.sifa.sifa_go.data.model.TipoInfraccionResponse
import com.sifa.sifa_go.domain.repository.TipoInfraccionRepository

/**
 * Implementación de [TipoInfraccionRepository] sobre SQLite (Room).
 * Es la fuente de verdad local del catálogo de tipologías para operar sin conexión.
 */
class TipoInfraccionRepositoryImpl(
    private val dao: TipoInfraccionDao
) : TipoInfraccionRepository {

    override suspend fun getCached(): List<TipoInfraccionResponse> =
        TipoInfraccionMapper.toDomainList(dao.getAll())

    override suspend fun saveAll(tipos: List<TipoInfraccionResponse>) {
        dao.upsertAll(TipoInfraccionMapper.toEntityList(tipos))
    }
}
