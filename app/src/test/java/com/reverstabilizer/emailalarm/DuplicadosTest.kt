package com.reverstabilizer.emailalarm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Una notificacion repetida no puede disparar dos alarmas. */
class DuplicadosTest {

    @Test
    fun `la misma notificacion dos veces se procesa una sola vez`() {
        val d = Duplicados()
        assertTrue(d.esNuevo("gmail|Google Payments|Payment method updated", ahora = 1_000))
        assertFalse(d.esNuevo("gmail|Google Payments|Payment method updated", ahora = 1_000))
    }

    @Test
    fun `correos distintos no se confunden`() {
        val d = Duplicados()
        assertTrue(d.esNuevo("gmail|USCIS|Case updated", ahora = 1_000))
        assertTrue(d.esNuevo("gmail|USCIS|Appointment scheduled", ahora = 1_000))
    }

    @Test
    fun `pasada la ventana, el mismo correo vuelve a contar`() {
        val d = Duplicados(ventanaMs = 60_000)
        assertTrue(d.esNuevo("x", ahora = 0))
        assertTrue(d.esNuevo("x", ahora = 61_000))
    }
}
