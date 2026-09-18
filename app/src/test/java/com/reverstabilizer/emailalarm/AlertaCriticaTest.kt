package com.reverstabilizer.emailalarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * La barra de arriba aparece solo cuando una alarma no va a sonar. Todo lo
 * demas vive abajo: la pantalla abre con las alarmas, no con un diagnostico.
 */
class AlertaCriticaTest {

    private val activa = Suscripcion.Estado.ACTIVA
    private val nunca = Suscripcion.Estado.NUNCA
    private val vencida = Suscripcion.Estado.VENCIDA

    @Test
    fun `sin un permiso imprescindible nada mas importa`() {
        assertEquals(Alerta.PERMISO, alertaCritica(true, vencida, true))
    }

    @Test
    fun `una suscripcion vencida siempre avisa`() {
        assertEquals(Alerta.VENCIDA, alertaCritica(false, vencida, false))
    }

    @Test
    fun `alguien nuevo sin alarmas no ve ninguna alerta`() {
        assertNull(alertaCritica(false, nunca, false))
    }

    @Test
    fun `alarmas activas sin suscripcion no van a sonar y se avisa`() {
        assertEquals(Alerta.SIN_SUSCRIPCION, alertaCritica(false, nunca, true))
    }

    @Test
    fun `con suscripcion y permisos no hay barra`() {
        assertNull(alertaCritica(false, activa, true))
    }
}
