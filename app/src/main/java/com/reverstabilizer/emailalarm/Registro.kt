package com.reverstabilizer.emailalarm

import android.util.Log

const val TAG = "MAILALARM"

/**
 * Logs de diagnostico, solo en debug.
 *
 * Los mensajes llevan remitente, asunto y cuerpo de los correos del usuario.
 * En una app publicada eso no puede quedar en el log del sistema: seria una
 * fuga de datos y contradiria la politica de privacidad, que dice que nada
 * sale del telefono.
 */
object Registro {
    fun d(mensaje: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, mensaje)
    }

    fun w(mensaje: String) {
        if (BuildConfig.DEBUG) Log.w(TAG, mensaje)
    }
}
