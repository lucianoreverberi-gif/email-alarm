package com.reverstabilizer.emailalarm

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * El orden de la tarjeta de estado es el recorrido de alguien nuevo:
 * permisos, primera regla, suscripcion. Y una suscripcion vencida es urgente.
 */
class EstadoGeneralTest {

    private val activa = Suscripcion.Estado.ACTIVA
    private val nunca = Suscripcion.Estado.NUNCA
    private val vencida = Suscripcion.Estado.VENCIDA

    @Test
    fun `sin un permiso imprescindible nada mas importa`() {
        assertEquals(EstadoGeneral.BLOQUEADA, estadoGeneral(true, vencida, false, true))
    }

    @Test
    fun `una suscripcion vencida va antes que crear reglas`() {
        assertEquals(EstadoGeneral.VENCIDA, estadoGeneral(false, vencida, false, false))
    }

    @Test
    fun `alguien nuevo crea su primera regla antes de que le pidan la tarjeta`() {
        assertEquals(EstadoGeneral.SIN_REGLAS, estadoGeneral(false, nunca, false, false))
        assertEquals(EstadoGeneral.SIN_SUSCRIPCION, estadoGeneral(false, nunca, true, false))
    }

    @Test
    fun `un permiso opcional faltante no bloquea`() {
        assertEquals(EstadoGeneral.LIMITADA, estadoGeneral(false, activa, true, true))
    }

    @Test
    fun `con todo en orden esta lista`() {
        assertEquals(EstadoGeneral.LISTA, estadoGeneral(false, activa, true, false))
    }
}
