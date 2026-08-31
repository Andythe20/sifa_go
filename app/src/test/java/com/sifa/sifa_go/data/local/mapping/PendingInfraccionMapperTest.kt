package com.sifa.sifa_go.data.local.mapping

import com.sifa.sifa_go.data.local.entity.SyncStatus
import com.sifa.sifa_go.data.model.InfraccionCreateRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingInfraccionMapperTest {

    private val request = InfraccionCreateRequest(
        lugar = "Av. Los Leones 1234",
        fecha = "2024-06-01T10:30:00.000000",
        latitud = -33.4f,
        longitud = -70.6f,
        patenteVehiculo = "ABCD12",
        idTipoInfraccion = 3,
        observaciones = "Vehículo mal estacionado",
        fechaCitacion = "2024-06-20T09:00:00.000000"
    )

    private val imagePaths = listOf("/data/photo1.jpg", "/data/photo2.jpg")

    @Test
    fun `toEntity preserves all infraccion fields`() {
        val entity = PendingInfraccionMapper.toEntity(request, imagePaths)

        assertEquals("Av. Los Leones 1234", entity.lugar)
        assertEquals("2024-06-01T10:30:00.000000", entity.fecha)
        assertEquals(-33.4f, entity.latitud)
        assertEquals(-70.6f, entity.longitud)
        assertEquals("ABCD12", entity.patenteVehiculo)
        assertEquals(3, entity.idTipoInfraccion)
        assertEquals("Vehículo mal estacionado", entity.observaciones)
        assertEquals("2024-06-20T09:00:00.000000", entity.fechaCitacion)
        assertEquals(SyncStatus.PENDING, entity.status)
    }

    @Test
    fun `toEntity serializes image paths to JSON`() {
        val entity = PendingInfraccionMapper.toEntity(request, imagePaths)
        assertTrue(entity.imagePathsJson.contains("photo1.jpg"))
        assertTrue(entity.imagePathsJson.contains("photo2.jpg"))
    }

    @Test
    fun `toDomain round trips entity to domain`() {
        val entity = PendingInfraccionMapper.toEntity(request, imagePaths)
        val domain = PendingInfraccionMapper.toDomain(entity)

        assertEquals(entity.id, domain.id)
        assertEquals(request.lugar, domain.request.lugar)
        assertEquals(request.fecha, domain.request.fecha)
        assertEquals(request.patenteVehiculo, domain.request.patenteVehiculo)
        assertEquals(request.idTipoInfraccion, domain.request.idTipoInfraccion)
        assertEquals(request.fechaCitacion, domain.request.fechaCitacion)
        assertEquals(imagePaths, domain.imagePaths)
        assertEquals(SyncStatus.PENDING, domain.status)
        assertTrue(domain.isPending)
    }

    @Test
    fun `toDomain handles null observaciones and fechaCitacion`() {
        val entity = PendingInfraccionMapper.toEntity(
            request.copy(observaciones = null, fechaCitacion = null),
            emptyList()
        )
        val domain = PendingInfraccionMapper.toDomain(entity)

        assertEquals(null, domain.request.observaciones)
        assertEquals(null, domain.request.fechaCitacion)
        assertTrue(domain.imagePaths.isEmpty())
    }

    @Test
    fun `decode handles invalid JSON gracefully`() {
        // Creamos una entidad con JSON corrupto y verificamos que no explote.
        val entity = PendingInfraccionMapper.toEntity(request, imagePaths)
            .copy(imagePathsJson = "not-json{{")
        val domain = PendingInfraccionMapper.toDomain(entity)
        assertTrue(domain.imagePaths.isEmpty())
    }
}
