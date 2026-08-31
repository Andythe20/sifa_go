package com.sifa.sifa_go.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa un tipo de infracción almacenado localmente.
 * Sirve de caché offline: se puebla desde el backend cuando hay conexión y
 * se consulta para mostrar el catálogo en el formulario cuando no la hay.
 */
@Entity(tableName = "tipos_infraccion")
data class TipoInfraccionEntity(
    @PrimaryKey
    val id: Int,
    val nombre: String
)
