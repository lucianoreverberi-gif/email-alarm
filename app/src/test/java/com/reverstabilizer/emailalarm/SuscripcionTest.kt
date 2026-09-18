package com.reverstabilizer.emailalarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Google Play informa la duracion de la prueba en formato ISO 8601. */
class SuscripcionTest {

    @Test
    fun `la prueba de 7 dias se lee como 7`() {
        assertEquals(7, diasDelPeriodo("P7D"))
        assertEquals(7, diasDelPeriodo("P1W"))
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

    @Test
    fun `el periodo de renovacion dice si el plan es anual o mensual`() {
        assertEquals(Suscripcion.Periodo.ANUAL, periodoDe("P1Y"))
        assertEquals(Suscripcion.Periodo.MENSUAL, periodoDe("P1M"))
        assertNull(periodoDe("P1W"))
    }

    @Test
    fun `el ahorro del anual se redondea hacia abajo`() {
        // US$9.99 al ano contra 12 x US$1.99 = US$23.88: 58.16 %
        assertEquals(58, ahorroAnual(mensualMicros = 1_990_000, anualMicros = 9_990_000))
    }

    @Test
    fun `sin ahorro real no se muestra ninguno`() {
        assertNull(ahorroAnual(mensualMicros = 1_000_000, anualMicros = 12_000_000))
        assertNull(ahorroAnual(mensualMicros = 0, anualMicros = 9_990_000))
    }
}
