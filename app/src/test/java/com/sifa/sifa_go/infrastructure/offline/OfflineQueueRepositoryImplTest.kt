package com.sifa.sifa_go.infrastructure.offline

import com.sifa.sifa_go.data.local.dao.PendingInfraccionDao
import com.sifa.sifa_go.data.local.entity.PendingInfraccionEntity
import com.sifa.sifa_go.data.local.entity.SyncStatus
import com.sifa.sifa_go.data.model.InfraccionCreateRequest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineQueueRepositoryImplTest {

    private val dao: PendingInfraccionDao = mockk(relaxed = true)

    private val request = InfraccionCreateRequest(
        lugar = "Av. Siempre Viva 742",
        fecha = "2024-06-01T10:30:00.000000",
        latitud = -33.4f,
        longitud = -70.6f,
        patenteVehiculo = "XYZ789",
        idTipoInfraccion = 2,
        observaciones = null,
        fechaCitacion = null
    )

    @Test
    fun `enqueue delegates to dao insert`() = runTest {
        val repo = OfflineQueueRepositoryImpl(dao)
        coEvery { dao.insert(any()) } returns 42L

        val id = repo.enqueue(request, listOf("/data/photo.jpg"))

        assertEquals(42L, id)
        coVerify { dao.insert(any()) }
    }

    @Test
    fun `getPending returns mapped domain objects`() = runTest {
        val entity = PendingInfraccionEntity(
            id = 1L,
            lugar = request.lugar,
            fecha = request.fecha,
            latitud = request.latitud,
            longitud = request.longitud,
            patenteVehiculo = request.patenteVehiculo,
            idTipoInfraccion = request.idTipoInfraccion,
            observaciones = null,
            fechaCitacion = null,
            imagePathsJson = "[\"/data/photo.jpg\"]",
            status = SyncStatus.PENDING,
            createdAt = 0L
        )
        coEvery { dao.getPending() } returns listOf(entity)

        val pending = repoUnderTest().getPending()

        assertEquals(1, pending.size)
        assertEquals("/data/photo.jpg", pending[0].imagePaths.single())
        assertEquals("XYZ789", pending[0].request.patenteVehiculo)
        assertTrue(pending[0].isPending)
    }

    @Test
    fun `deleteById delegates to dao`() = runTest {
        val repo = repoUnderTest()
        repo.deleteById(7L)
        coVerify { dao.deleteById(7L) }
    }

    @Test
    fun `markFailed delegates to dao`() = runTest {
        val repo = repoUnderTest()
        repo.markFailed(8L, "hay motivo")
        coVerify { dao.markFailed(8L, "hay motivo") }
    }

    @Test
    fun `observeSyncableQueue propagates flow`() = runTest {
        val entity = PendingInfraccionEntity(
            id = 1L,
            lugar = "lugar",
            fecha = "2024-01-01T00:00:00.000000",
            latitud = 0f,
            longitud = 0f,
            patenteVehiculo = "AA11",
            idTipoInfraccion = 1,
            observaciones = null,
            fechaCitacion = null,
            imagePathsJson = "[]",
            status = SyncStatus.PENDING,
            createdAt = 0L
        )
        coEvery { dao.observeSyncableQueue() } returns flowOf(listOf(entity))

        val repo = repoUnderTest()
        val result = repo.observeSyncableQueue().toList()

        assertEquals(1, result.size)
        assertEquals("AA11", result[0][0].request.patenteVehiculo)
    }

    private fun repoUnderTest(): OfflineQueueRepositoryImpl {
        return OfflineQueueRepositoryImpl(dao)
    }
}
