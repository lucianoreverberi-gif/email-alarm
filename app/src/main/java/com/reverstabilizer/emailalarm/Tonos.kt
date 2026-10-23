package com.reverstabilizer.emailalarm

import android.content.Context
import android.net.Uri

/**
 * Los seis tonos que viene con la app, los mismos que en la version de iPhone.
 *
 * Antes el unico camino era el selector de tonos del sistema: una lista
 * distinta en cada marca, con decenas de tonos genericos. Elegir un tono para
 * una alarma de trabajo no deberia ser una expedicion.
 *
 * [archivo] se guarda por nombre y no por id de recurso: los id cambian en
 * cada compilacion, asi que una regla guardada se quedaria sin tono despues
 * de una actualizacion.
 */
enum class Tono(val archivo: String, val etiqueta: Int) {
    ALARMA("tono_alarma", R.string.tone_alarm),
    AURORA("tono_aurora", R.string.tone_aurora),
    CAMPANAS("tono_campanas", R.string.tone_bells),
    PULSO("tono_pulso", R.string.tone_pulse),
    BRISA("tono_brisa", R.string.tone_breeze),
    DIGITAL("tono_digital", R.string.tone_digital);

    /** Lo que se guarda en la regla y lo que reproduce [AlarmPlayer]. */
    fun uri(context: Context): Uri =
        Uri.parse("android.resource://${context.packageName}/raw/$archivo")

    fun comoTexto(context: Context): String = uri(context).toString()

    companion object {
        /** Los seis duran lo mismo. Se muestra al lado del nombre. */
        const val SEGUNDOS = 24

        /** El tono de la app que corresponde a lo guardado, o null si es uno del telefono. */
        fun desde(sonido: String?): Tono? {
            if (sonido == null) return null
            return entries.firstOrNull { sonido.endsWith("/raw/${it.archivo}") }
        }
    }
}
