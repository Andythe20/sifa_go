package com.sifa.sifa_go.data.model

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
