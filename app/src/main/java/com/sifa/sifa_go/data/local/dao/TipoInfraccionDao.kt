package com.sifa.sifa_go.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.sifa.sifa_go.data.local.entity.TipoInfraccionEntity

@Dao
interface TipoInfraccionDao {

    /**
     * Inserta o actualiza cada registro según su clave primaria (id).
     * Sustituye a un diff manual: en cada refresco de red se realiza un upsert atómico.
     */
    @Upsert
    suspend fun upsertAll(tipos: List<TipoInfraccionEntity>)

    @Query("SELECT * FROM tipos_infraccion ORDER BY nombre ASC")
    suspend fun getAll(): List<TipoInfraccionEntity>
}
