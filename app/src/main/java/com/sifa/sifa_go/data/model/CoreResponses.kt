package com.sifa.sifa_go.data.model

import com.google.gson.annotations.SerializedName

data class PlateInfoResponse(
    val patente: String,
    val marca: String,
    val modelo: String,
    val anio_fabricacion: Int,
    val color: String,
    val nro_motor: String,
    val nro_serie: String,
    val propietario: String,
    val rut: String
)

data class TipoInfraccionResponse(
    val id: Int,
    val nombre: String,
)

/*
 Modelo para enviar una nueva infracción al Core Service.
 Debe coincidir exactamente con InfraccionCreateRequest.java del backend.
 */
data class InfraccionCreateRequest(
    val lugar: String,          // Dirección o descripción del lugar
    val fecha: String,          // Fecha en formato ISO 8601 (LocalDateTime en el backend)
    val latitud: Float,         // Capturada desde el GPS del móvil
    val longitud: Float,        // Capturada desde el GPS del móvil
    val patenteVehiculo: String,
    val idTipoInfraccion: Int,  // ID obtenido de la lista de tipos de infracción
    val observaciones: String?,
)

/*
  Respuesta del servidor tras crear exitosamente una infracción.
 */
data class InfraccionResponse(
    val id: String?,
    val status: String?,
    val timestamp: String?
)

data class InfractionLocation(
    val address: String?,
    val lat: Double?,
    val lng: Double?
)

data class InfractionVehicle(
    val brand: String?,
    val color: String?,
    val model: String?,
    val plate: String?,
    val type: String?
)

data class InfractionDenunciado(
    val comuna: String?,
    val direccion: String?,
    val edad: String?,
    val estadoCivil: String?,
    val nombre: String?,
    val profesion: String?,
    val rut: String?
)

data class InfractionTramitacion(
    val fechaCitacion: String?,
    val listadoCorte: Boolean?
)

data class InfraccionHistoryItem(
    val id: String,
    val status: String,
    val timestamp: String,
    val infractionDescription: String?,
    val numeroBoleta: String?,
    val numeroParte: String?,
    val agentId: String?,
    val infractionCode: String?,
    val disposicionInfringida: String?,
    val location: InfractionLocation?,
    val vehicle: InfractionVehicle?,
    @SerializedName("denunciado")
    val denunciado: InfractionDenunciado?,
    val tramitacion: InfractionTramitacion?,
    val photoUrl: String?,
    val evidenceUrls: List<String>?,
    val denunciante: String?,
    val amount: Double?
)
