package com.sifa.sifa_go.data.local.mapping

import com.sifa.sifa_go.data.local.entity.TipoInfraccionEntity
import com.sifa.sifa_go.data.model.TipoInfraccionResponse

/**
 * Convierte entre [TipoInfraccionResponse] (API/dominio) y [TipoInfraccionEntity] (Room).
 */
object TipoInfraccionMapper {

    fun toEntity(response: TipoInfraccionResponse): TipoInfraccionEntity =
        TipoInfraccionEntity(id = response.id, nombre = response.nombre)

    fun toEntityList(responses: List<TipoInfraccionResponse>): List<TipoInfraccionEntity> =
        responses.map(::toEntity)

    fun toDomain(entity: TipoInfraccionEntity): TipoInfraccionResponse =
        TipoInfraccionResponse(id = entity.id, nombre = entity.nombre)

    fun toDomainList(entities: List<TipoInfraccionEntity>): List<TipoInfraccionResponse> =
        entities.map(::toDomain)
}
