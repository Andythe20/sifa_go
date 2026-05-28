package com.sifa.sifa_go.data.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

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
    val fechaCitacion: String?
)

/*
  Respuesta del servidor tras crear exitosamente una infracción.
 */
data class InfraccionResponse(
    val id: String?,
    val status: String?,
    val timestamp: String?
)

/** -----------------------------------------------
 * DTOs para mapear la respuesta del backend con las infracciones
 * ------------------------------------------------
 * */
data class InfractionLocation(
    val address: String?,
    val lat: Double?,
    val lng: Double?
)

data class InfractionVehicle(
    val brand: String?,
    val color: String?,
    val model: String?,
    val nroMotor: String?,
    val nroSerie: String?,
    val plate: String?,
    val type: String?,
    val year: Int?
)

data class InfractionPropietario(
    val comuna: String?,
    val correo: String?,
    val direccion: String?,
    val edad: String?,
    val estadoCivil: String?,
    val nombreCompleto: String?,
    val profesion: String?,
    val rut: String?,
    val telefono: String?
)

data class TipoInfraccion(
    val id: Int,
    val nombre: String,
    val disposicionInfringida: String?
)

// wrapper para respuesta de las infracciones
data class SpringPageResponse<T>(
    @SerializedName("content")
    val content: List<T>,

    @SerializedName("totalPages")
    val totalPages: Int,

    @SerializedName("totalElements")
    val totalElements: Int,

    @SerializedName("last")
    val isLast: Boolean,

    @SerializedName("first")
    val isFirst: Boolean,

    @SerializedName("size")
    val size: Int,

    @SerializedName("number")
    val pageNumber: Int
)

data class InfraccionHistoryItem(
    @SerializedName("idInfraccion", alternate = ["id"])
    val id: String?,
    val idFiscalizador: String?,
    val idUsuarioJPL: String?,
    val fecha: String?,
    val status: String,
    val motivoRechazo: String?,
    val fechaResolucion: String?,
    val observaciones: String?,
    // nodo tipo infraccion
    val tipoInfraccion: TipoInfraccion?,
    // nodo location
    val location: InfractionLocation?,
    // nodo vehicle
    val vehicle: InfractionVehicle?,
    // nodo denunciado
    val propietario: InfractionPropietario?,
    // nodo evidencias urls
    val evidenceUrls: List<String>?
)


/**/
data class FiscalizadorHeartbeatRequest(
    @SerializedName("latitud")
    val latitud: Double,

    @SerializedName("longitud")
    val longitud: Double
)