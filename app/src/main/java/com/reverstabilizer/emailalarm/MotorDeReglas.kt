package com.reverstabilizer.emailalarm

import android.app.Notification
import android.app.Person
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification

/** Un correo tal como lo pudimos leer desde la notificacion. */
data class CorreoDetectado(
    val paquete: String,
    /** Nombre visible del remitente ("Luciano Reverberi"). Es lo unico que dan todas las apps. */
    val remitente: String,
    /** Direcciones del remitente, si la app las publica. Gmail si, Outlook no. */
    val direcciones: List<String>,
    val asunto: String,
    val cuerpo: String
) {
    /** Todo lo que identifica a quien mando: nombre y direccion. */
    fun textoDelRemitente(): String = (listOf(remitente) + direcciones).joinToString(" ")

    /** Donde buscamos las palabras clave: asunto y resumen del cuerpo. */
    fun textoDelContenido(): String = "$asunto $cuerpo"
}

object MotorDeReglas {

    /**
     * Si la notificacion es un aviso de la app de correo de que una cuenta no
     * puede sincronizar, devuelve su texto. Si no, null.
     *
     * Importa: una cuenta caida no genera correos, y una app que no suena
     * "porque no llego nada" es indistinguible de una que anda bien. Es el
     * falso negativo silencioso que esta app existe para evitar.
     */
    fun avisoDeSincronizacion(sbn: StatusBarNotification): String? {
        val extras = sbn.notification.extras
        val tipo = extras.getString("argNotificationType").orEmpty()
        if (!tipo.contains("WARNING")) return null

        val texto = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val ticker = sbn.notification.tickerText?.toString().orEmpty()
        return listOf(ticker, texto).firstOrNull { it.isNotBlank() } ?: tipo
    }

    /**
     * Devuelve el correo, o null si la notificacion no es un correo que podamos
     * evaluar (avisos de la propia app, resumenes de grupo, contenido oculto).
     */
    fun leerCorreo(sbn: StatusBarNotification): CorreoDetectado? {
        val notificacion = sbn.notification
        val extras = notificacion.extras

        // El resumen de grupo ("3 mensajes nuevos") no trae remitente ni asunto.
        if (notificacion.flags and Notification.FLAG_GROUP_SUMMARY != 0) return null

        // Gmail marca sus propios avisos ("reingresa la contrasena") con este tipo.
        val tipoGmail = extras.getString("argNotificationType").orEmpty()
        if (tipoGmail.contains("WARNING")) return null

        // Los campos vienen como CharSequence (texto con formato), no como String:
        // getString() devolveria null y parecerian vacios.
        val remitente = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val asunto = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val cuerpo = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()

        // Outlook oculta el contenido en algunas configuraciones. Sin remitente
        // ni asunto no hay nada contra que comparar.
        if (asunto == "Content hidden" || remitente.isBlank()) return null

        return CorreoDetectado(
            paquete = sbn.packageName,
            remitente = remitente,
            direcciones = direccionesDe(extras),
            asunto = asunto,
            cuerpo = cuerpo
        )
    }

    /** La primera regla activa que coincide, o null si no coincide ninguna. */
    fun primeraQueCoincide(reglas: List<Regla>, correo: CorreoDetectado): Regla? =
        reglas.firstOrNull { coincide(it, correo) }

    fun coincide(regla: Regla, correo: CorreoDetectado): Boolean {
        val buscaRemitente = regla.remitente.isNotBlank()
        val buscaPalabra = regla.palabraClave.isNotBlank()

        // Una regla sin nada cargado sonaria con todo. No dispara.
        if (!buscaRemitente && !buscaPalabra) return false

        // Alcanza con que se cumpla una. Perderse un correo es peor que sonar
        // de mas, asi que ante la duda la regla dispara.
        val remitenteCoincide = buscaRemitente &&
            coincideRemitente(regla.remitente, correo.textoDelRemitente())
        val palabraCoincide = buscaPalabra &&
            normalizar(correo.textoDelContenido()).contains(normalizar(regla.palabraClave.trim()))

        return remitenteCoincide || palabraCoincide
    }

    /**
     * Coincidencia parcial, sin mayusculas ni tildes. Si lo buscado es una
     * direccion y la app de correo solo publica el nombre (Gmail a veces,
     * Outlook siempre), tambien prueba con lo que va antes de la @:
     * "lucianoreverberi@hotmail.com" coincide con "Luciano Reverberi".
     */
    internal fun coincideRemitente(buscado: String, textoDelRemitente: String): Boolean {
        val texto = normalizar(textoDelRemitente)
        val b = normalizar(buscado.trim())
        if (texto.contains(b)) return true

        val arroba = b.indexOf('@')
        if (arroba <= 0) return false
        val local = compacto(b.substring(0, arroba))
        if (local.length < 4 || local in LOCALES_GENERICOS) return false
        return compacto(texto).contains(local)
    }

    /**
     * Partes de direccion tan comunes que usarlas haria sonar la alarma con
     * cualquier remitente. Van compactadas (sin puntos ni guiones).
     */
    private val LOCALES_GENERICOS = setOf(
        "info", "noreply", "donotreply", "contact", "contacto", "support", "soporte",
        "admin", "hello", "hola", "mail", "email", "notification", "notifications",
        "notificaciones", "team", "news", "newsletter", "alerts", "alertas",
        "service", "servicio", "billing", "ventas", "sales"
    )

    /** Gmail deja la direccion del remitente aca, como "mailto:alguien@dominio". */
    private fun direccionesDe(extras: Bundle): List<String> {
        val personas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelableArrayList(Notification.EXTRA_PEOPLE_LIST, Person::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelableArrayList<Person>(Notification.EXTRA_PEOPLE_LIST)
        }

        val desdePersonas = personas.orEmpty().flatMap { persona ->
            listOfNotNull(persona.name?.toString(), persona.uri?.removePrefix("mailto:"))
        }

        @Suppress("DEPRECATION")
        val desdeArray = extras.getStringArray(Notification.EXTRA_PEOPLE).orEmpty()
            .map { it.removePrefix("mailto:") }

        return (desdePersonas + desdeArray).filter { it.isNotBlank() }
    }
}


/** Minusculas y sin tildes: "Migración" y "migracion" son lo mismo. */
internal fun normalizar(texto: String): String =
    java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()

/** Normalizado y solo letras y numeros: "Juan Pérez" y "juan.perez" dan igual. */
internal fun compacto(texto: String): String = normalizar(texto).filter { it.isLetterOrDigit() }
