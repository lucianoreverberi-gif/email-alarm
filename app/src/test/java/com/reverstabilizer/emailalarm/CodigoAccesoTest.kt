package com.reverstabilizer.emailalarm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** El codigo de los revisores de Google: valido, sin importar como se tipea, y con vencimiento. */
class CodigoAccesoTest {

    private val huella = "b6f735b65a2fad48db08fbb827e3fe97ef1f07574208e8ecb261d01a07e9c80b" // SHA-256 de "EA-TEST-1"
    private val vence = LocalDate.of(2026, 12, 18)
    private val hoy = LocalDate.of(2026, 9, 19)

    @Test
    fun `el codigo correcto vale`() {
        assertTrue(codigoValido("EA-TEST-1", hoy, huella, vence))
    }

    @Test
    fun `no importan mayusculas ni espacios`() {
        assertTrue(codigoValido("  ea-test-1 ", hoy, huella, vence))
    }

    @Test
    fun `un codigo equivocado no vale`() {
        assertFalse(codigoValido("EA-TEST-2", hoy, huella, vence))
        assertFalse(codigoValido("", hoy, huella, vence))
    }

    @Test
    fun `vence despues de la fecha, no antes`() {
        assertTrue(codigoValido("EA-TEST-1", vence, huella, vence))
        assertFalse(codigoValido("EA-TEST-1", vence.plusDays(1), huella, vence))
    }
}
