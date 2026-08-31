package com.sifa.sifa_go.infrastructure.tipos

import com.sifa.sifa_go.data.local.dao.TipoInfraccionDao
import com.sifa.sifa_go.data.local.entity.TipoInfraccionEntity
import com.sifa.sifa_go.data.model.TipoInfraccionResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TipoInfraccionRepositoryImplTest {

    private val dao: TipoInfraccionDao = mockk(relaxed = true)

    private val repo = TipoInfraccionRepositoryImpl(dao)

    @Test
    fun `getCached maps entities to domain`() = runTest {
        coEvery { dao.getAll() } returns listOf(
            TipoInfraccionEntity(id = 1, nombre = "Alta velocidad"),
            TipoInfraccionEntity(id = 2, nombre = "Estacionamiento")
        )

        val result = repo.getCached()

        assertEquals(
            listOf(
                TipoInfraccionResponse(id = 1, nombre = "Alta velocidad"),
                TipoInfraccionResponse(id = 2, nombre = "Estacionamiento")
            ),
            result
        )
    }

    @Test
    fun `saveAll upserts entities from domain`() = runTest {
        val tipos = listOf(
            TipoInfraccionResponse(id = 1, nombre = "Alta velocidad"),
            TipoInfraccionResponse(id = 2, nombre = "Estacionamiento")
        )

        repo.saveAll(tipos)

        coVerify { dao.upsertAll(
            listOf(
                TipoInfraccionEntity(id = 1, nombre = "Alta velocidad"),
                TipoInfraccionEntity(id = 2, nombre = "Estacionamiento")
            )
        ) }
    }

    @Test
    fun `getCached returns empty list when table is empty`() = runTest {
        coEvery { dao.getAll() } returns emptyList()

        assertEquals(emptyList<TipoInfraccionResponse>(), repo.getCached())
    }
}
