package com.reverstabilizer.emailalarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Google Play informa la duracion de la prueba en formato ISO 8601. */
class SuscripcionTest {

    @Test
    fun `la prueba de 30 dias se lee como 30`() {
        assertEquals(30, diasDelPeriodo("P30D"))
    }

    @Test
    fun `un mes cuenta como 30 dias`() {
        assertEquals(30, diasDelPeriodo("P1M"))
    }

    @Test
    fun `semanas se pasan a dias`() {
        assertEquals(28, diasDelPeriodo("P4W"))
    }

    @Test
    fun `un periodo que no se entiende no inventa un numero`() {
        assertNull(diasDelPeriodo("30 dias"))
        assertNull(diasDelPeriodo(""))
    }
}
