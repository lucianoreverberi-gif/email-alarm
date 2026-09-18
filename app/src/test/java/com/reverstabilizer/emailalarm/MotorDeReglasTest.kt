package com.reverstabilizer.emailalarm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El motor decide si suena o no. Un falso negativo es invisible en la app
 * (simplemente no suena), asi que conviene fijarlo con tests.
 */
class MotorDeReglasTest {

    private fun correo(
        remitente: String = "Luciano Reverberi",
        direcciones: List<String> = listOf("lucianoreverberi@hotmail.com"),
        asunto: String = "test4",
        cuerpo: String = ""
    ) = CorreoDetectado("com.google.android.gm", remitente, direcciones, asunto, cuerpo)

    @Test
    fun `coincide por direccion cuando la app la publica`() {
        val regla = Regla(nombre = "x", remitente = "lucianoreverberi@hotmail.com")
        assertTrue(MotorDeReglas.coincide(regla, correo()))
    }

    @Test
    fun `coincide por nombre visible cuando no hay direccion`() {
        val regla = Regla(nombre = "x", remitente = "Luciano")
        assertTrue(MotorDeReglas.coincide(regla, correo(direcciones = emptyList())))
    }

    @Test
    fun `una direccion coincide con el nombre visible usando lo que va antes de la arroba`() {
        // Outlook y a veces Gmail solo publican "Luciano Reverberi", sin direccion.
        val regla = Regla(nombre = "x", remitente = "lucianoreverberi@hotmail.com")
        assertTrue(MotorDeReglas.coincide(regla, correo(direcciones = emptyList())))
    }

    @Test
    fun `la parte antes de la arroba ignora puntos y tildes`() {
        val regla = Regla(nombre = "x", remitente = "juan.perez@empresa.com")
        assertTrue(MotorDeReglas.coincide(regla, correo(remitente = "Juan Pérez", direcciones = emptyList())))
    }

    @Test
    fun `una parte generica antes de la arroba no se usa`() {
        // Si "info" alcanzara, sonaria con cualquier remitente llamado "Info ...".
        val regla = Regla(nombre = "x", remitente = "info@miempresa.com")
        assertFalse(
            MotorDeReglas.coincide(regla, correo(remitente = "Info Banco Galicia", direcciones = emptyList()))
        )
    }

    @Test
    fun `las tildes no importan en ninguna direccion`() {
        val conTilde = Regla(nombre = "x", palabraClave = "migración")
        assertTrue(MotorDeReglas.coincide(conTilde, correo(asunto = "Su cita de migracion")))
        val sinTilde = Regla(nombre = "x", palabraClave = "migracion")
        assertTrue(MotorDeReglas.coincide(sinTilde, correo(asunto = "Su cita de MIGRACIÓN")))
    }

    @Test
    fun `el remitente coincide en parte`() {
        val regla = Regla(nombre = "x", remitente = "uscis")
        assertTrue(
            MotorDeReglas.coincide(regla, correo(remitente = "USCIS Online Account", direcciones = emptyList()))
        )
    }

    @Test
    fun `no distingue mayusculas`() {
        val regla = Regla(nombre = "x", remitente = "LUCIANOREVERBERI@HOTMAIL.COM")
        assertTrue(MotorDeReglas.coincide(regla, correo()))
    }

    @Test
    fun `palabra clave sola sirve cuando no se sabe el remitente`() {
        val regla = Regla(nombre = "migraciones", palabraClave = "cita")
        val mail = correo(remitente = "USCIS", direcciones = emptyList(), asunto = "Your cita is scheduled")
        assertTrue(MotorDeReglas.coincide(regla, mail))
    }

    @Test
    fun `la palabra clave tambien se busca en el cuerpo`() {
        val regla = Regla(nombre = "turnos", palabraClave = "shift")
        val mail = correo(asunto = "Weekly update", cuerpo = "Sign up for your shift here")
        assertTrue(MotorDeReglas.coincide(regla, mail))
    }

    @Test
    fun `con remitente y palabra clave alcanza con que se cumpla una`() {
        val regla = Regla(nombre = "x", remitente = "Luciano", palabraClave = "turno")

        // Solo el remitente.
        assertTrue(MotorDeReglas.coincide(regla, correo(asunto = "otra cosa")))
        // Solo la palabra clave.
        assertTrue(
            MotorDeReglas.coincide(
                regla,
                correo(remitente = "Otro", direcciones = emptyList(), asunto = "tu turno esta listo")
            )
        )
        // Ninguna de las dos.
        assertFalse(
            MotorDeReglas.coincide(
                regla,
                correo(remitente = "Otro", direcciones = emptyList(), asunto = "otra cosa")
            )
        )
    }

    @Test
    fun `una regla vacia nunca dispara`() {
        val regla = Regla(nombre = "vacia")
        assertFalse(MotorDeReglas.coincide(regla, correo()))
    }

    @Test
    fun `una regla que no coincide no dispara`() {
        val regla = Regla(nombre = "x", remitente = "otra@persona.com")
        assertFalse(MotorDeReglas.coincide(regla, correo()))
    }
}
