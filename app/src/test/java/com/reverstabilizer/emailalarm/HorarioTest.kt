package com.reverstabilizer.emailalarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

class HorarioTest {

    // 2026-09-21 es lunes.
    private fun lunes(hora: Int, minuto: Int = 0) = LocalDateTime.of(2026, 9, 21, hora, minuto)
    private fun sabado(hora: Int, minuto: Int = 0) = LocalDateTime.of(2026, 9, 26, hora, minuto)

    private val oficina = Horario(Horario.LUNES_A_VIERNES, 9 * 60, 18 * 60)
    private val noche = Horario(Horario.LUNES_A_VIERNES, 22 * 60, 7 * 60)

    @Test
    fun `por defecto suena siempre`() {
        val h = Horario()
        assertTrue(h.esSiempre)
        assertTrue(h.permite(lunes(3)))
        assertTrue(h.permite(sabado(23, 59)))
    }

    @Test
    fun `el caso de las 6 de la manana no suena en horario de oficina`() {
        assertFalse(oficina.permite(lunes(6)))
        assertTrue(oficina.permite(lunes(9)))
        assertTrue(oficina.permite(lunes(17, 59)))
        // "Hasta las 18" no incluye las 18:00 en punto.
        assertFalse(oficina.permite(lunes(18)))
    }

    @Test
    fun `los dias apagados no suenan aunque sea en horario`() {
        assertFalse(oficina.permite(sabado(12)))
    }

    @Test
    fun `el horario que cruza la medianoche usa el dia en que llega`() {
        assertTrue(noche.permite(lunes(23)))
        assertTrue(noche.permite(lunes(3)))
        assertFalse(noche.permite(lunes(12)))
        // Sabado a las 3 no suena, aunque el viernes a la noche si.
        assertFalse(noche.permite(sabado(3)))
    }

    @Test
    fun `prender y apagar dias`() {
        val h = Horario().conDia(DayOfWeek.SATURDAY, false).conDia(DayOfWeek.SUNDAY, false)
        assertEquals(Horario.LUNES_A_VIERNES, h.dias)
        assertFalse(h.suenaEl(DayOfWeek.SUNDAY))
        assertTrue(h.conDia(DayOfWeek.SUNDAY, true).suenaEl(DayOfWeek.SUNDAY))
        assertEquals(Horario.FIN_DE_SEMANA, Horario.TODOS_LOS_DIAS and Horario.LUNES_A_VIERNES.inv())
    }

    @Test
    fun `si coinciden dos alarmas alcanza con que una este en horario`() {
        val correo = CorreoDetectado("gmail", "Cliente Acme", emptyList(), "Nueva compra de uscis", "")
        val clientes = Regla(id = 1, nombre = "Clientes", remitente = "acme", dias = oficina.dias, desde = oficina.desde, hasta = oficina.hasta)
        val migraciones = Regla(id = 2, nombre = "Migraciones", palabraClave = "uscis")

        val decision = MotorDeReglas.decidir(listOf(clientes, migraciones), correo, lunes(6))
        assertEquals(Decision.Suena(migraciones), decision)
    }

    @Test
    fun `si coincide solo fuera de horario no suena`() {
        val correo = CorreoDetectado("gmail", "Cliente Acme", emptyList(), "Nueva compra", "")
        val clientes = Regla(id = 1, nombre = "Clientes", remitente = "acme", dias = oficina.dias, desde = oficina.desde, hasta = oficina.hasta)

        assertEquals(Decision.FueraDeHorario(clientes), MotorDeReglas.decidir(listOf(clientes), correo, lunes(6)))
        assertEquals(Decision.Suena(clientes), MotorDeReglas.decidir(listOf(clientes), correo, lunes(10)))
        assertEquals(Decision.Ninguna, MotorDeReglas.decidir(emptyList(), correo, lunes(10)))
    }
}
