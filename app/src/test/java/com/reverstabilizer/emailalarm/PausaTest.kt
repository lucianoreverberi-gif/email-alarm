package com.reverstabilizer.emailalarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lo unico que decide si una alarma suena durante una pausa es esta cuenta.
 * Si se equivoca para el lado de "todavia vale", la app queda sorda sin que
 * nadie se entere, que es la falla que no nos podemos permitir.
 */
class PausaTest {

    private val ahora = 1_700_000_000_000L

    @Test
    fun `sin pausa guardada no hay pausa`() {
        assertFalse(pausaVigente(0L, ahora))
    }

    @Test
    fun `una pausa que todavia no vencio vale`() {
        assertTrue(pausaVigente(ahora + 60_000, ahora))
    }

    @Test
    fun `una pausa vencida no vale`() {
        assertFalse(pausaVigente(ahora - 1, ahora))
    }

    @Test
    fun `justo a la hora de vuelta ya suena`() {
        // El borde va para el lado de sonar: ante la duda, suena.
        assertFalse(pausaVigente(ahora, ahora))
    }

    @Test
    fun `la pausa sin vencimiento no vence sola`() {
        assertTrue(pausaVigente(PAUSA_SIN_VENCIMIENTO, ahora))
        assertTrue(pausaVigente(PAUSA_SIN_VENCIMIENTO, Long.MAX_VALUE - 1))
    }

    @Test
    fun `cada duracion cae donde dice su nombre`() {
        assertEquals(ahora + 30 * 60_000, Pausa.Duracion.MEDIA_HORA.hasta(ahora))
        assertEquals(ahora + 60 * 60_000, Pausa.Duracion.UNA_HORA.hasta(ahora))
        assertEquals(ahora + 2 * 60 * 60_000, Pausa.Duracion.DOS_HORAS.hasta(ahora))
        assertEquals(ahora + 8 * 60 * 60_000, Pausa.Duracion.OCHO_HORAS.hasta(ahora))
    }

    @Test
    fun `solo la indefinida no tiene hora de vuelta`() {
        val sinVencimiento = Pausa.Duracion.entries.filter { it.hasta(ahora) == PAUSA_SIN_VENCIMIENTO }
        assertEquals(listOf(Pausa.Duracion.INDEFINIDA), sinVencimiento)
    }

    @Test
    fun `toda duracion con minutos vence despues de ahora`() {
        // Un 0 o un negativo por error dejaria una pausa que nunca empieza.
        Pausa.Duracion.entries.filter { it.minutos != null }.forEach { duracion ->
            assertTrue(duracion.name, pausaVigente(duracion.hasta(ahora), ahora))
        }
    }
}
