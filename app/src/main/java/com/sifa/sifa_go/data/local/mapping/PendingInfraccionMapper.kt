package com.sifa.sifa_go.data.local.mapping

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sifa.sifa_go.data.local.entity.PendingInfraccionEntity
import com.sifa.sifa_go.data.model.InfraccionCreateRequest
import com.sifa.sifa_go.domain.model.PendingInfraccion

/**
 * Convierte entre [PendingInfraccionEntity] (Room) y [PendingInfraccion] (dominio).
 * Las rutas de fotos se serializan a JSON para almacenarse en una sola columna.
 */
object PendingInfraccionMapper {

    private val gson = Gson()

    fun toEntity(request: InfraccionCreateRequest, imagePaths: List<String>): PendingInfraccionEntity {
        return PendingInfraccionEntity(
            lugar = request.lugar,
            fecha = request.fecha,
            latitud = request.latitud,
            longitud = request.longitud,
            patenteVehiculo = request.patenteVehiculo,
            idTipoInfraccion = request.idTipoInfraccion,
            observaciones = request.observaciones,
            fechaCitacion = request.fechaCitacion,
            imagePathsJson = gson.toJson(imagePaths)
        )
    }

    fun toDomain(entity: PendingInfraccionEntity): PendingInfraccion {
        val request = InfraccionCreateRequest(
            lugar = entity.lugar,
            fecha = entity.fecha,
            latitud = entity.latitud,
            longitud = entity.longitud,
            patenteVehiculo = entity.patenteVehiculo,
            idTipoInfraccion = entity.idTipoInfraccion,
            observaciones = entity.observaciones,
            fechaCitacion = entity.fechaCitacion
        )
        return PendingInfraccion(
            id = entity.id,
            request = request,
            imagePaths = decodeImagePaths(entity.imagePathsJson),
            status = entity.status,
            createdAt = entity.createdAt,
            failureReason = entity.failureReason
        )
    }

    fun toDomainList(entities: List<PendingInfraccionEntity>): List<PendingInfraccion> =
        entities.map(::toDomain)

    private fun decodeImagePaths(json: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
