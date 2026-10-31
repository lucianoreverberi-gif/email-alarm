package com.reverstabilizer.emailalarm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cuando sospechar que la app de correo tiene las notificaciones apagadas.
 *
 * Importa tanto cuando avisa como cuando no: un aviso que aparece con todo bien
 * deja de significar algo, y la persona aprende a ignorarlo.
 */
class SilencioTest {

    private val hora = 60L * 60 * 1000
    private val dia = 24 * hora
    private val ahora = 1_700_000_000_000

    @Test
    fun `avisa si escuchamos hace rato y nunca llego nada`() {
        assertTrue(haySilencio(escuchaDesde = ahora - 10 * dia, ultimaSenal = 0, ahora = ahora))
    }

    @Test
    fun `avisa si lo ultimo que llego fue hace mas de dos dias`() {
        assertTrue(
            haySilencio(
                escuchaDesde = ahora - 10 * dia,
                ultimaSenal = ahora - 3 * dia,
                ahora = ahora
            )
        )
    }

    @Test
    fun `no avisa si llego algo esta manana`() {
        assertFalse(
            haySilencio(
                escuchaDesde = ahora - 10 * dia,
                ultimaSenal = ahora - 5 * hora,
                ahora = ahora
            )
        )
    }

    @Test
    fun `no avisa a quien acaba de instalar la app`() {
        // Nunca llego nada, pero escuchamos hace dos horas: no prueba nada.
        assertFalse(haySilencio(escuchaDesde = ahora - 2 * hora, ultimaSenal = 0, ahora = ahora))
    }

    @Test
    fun `no avisa sin saber desde cuando escuchamos`() {
        // Recien actualizada desde una version que no lo anotaba, o el listener
        // nunca se conecto. Sin referencia no se puede concluir un silencio.
        assertFalse(haySilencio(escuchaDesde = 0, ultimaSenal = 0, ahora = ahora))
    }

    @Test
    fun `el limite son exactamente dos dias`() {
        val viejo = ahora - 10 * dia
        assertFalse(haySilencio(viejo, ahora - SILENCIO_SOSPECHOSO + 1, ahora))
        assertTrue(haySilencio(viejo, ahora - SILENCIO_SOSPECHOSO, ahora))
    }
}
