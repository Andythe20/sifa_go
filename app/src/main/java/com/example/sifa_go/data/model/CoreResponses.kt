package com.example.sifa_go.data.model

data class PlateInfoResponse(
    val plate: String,
    val marca: String,
    val modelo: String,
    val anio_fabricacion: Int,
    val color: String,
    val nro_motor: String,
    val nro_chasis: String,
    val rut_propietario: String,
    val nom_propietario: String
)