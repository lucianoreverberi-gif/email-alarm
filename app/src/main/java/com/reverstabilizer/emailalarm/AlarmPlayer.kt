package com.reverstabilizer.emailalarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper

// Corte de seguridad: si nadie la para, la alarma no suena para siempre.
private const val DURACION_MAXIMA_MS = 2 * 60 * 1000L

/**
 * Solo el sonido. Quien decide cuando suena es [Alarma].
 *
 * Usa USAGE_ALARM, o sea el canal de volumen "Alarma", que es independiente del
 * volumen del timbre: suena aunque el telefono este en silencio. Ademas ese uso
 * esta exento de la restriccion de audio en segundo plano de Android 16.
 */
object AlarmPlayer {

    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val corteDeSeguridad = Runnable { detener() }
    private var alDetener: (() -> Unit)? = null

    fun estaSonando(): Boolean = player != null

    /** Idempotente: llamarla dos veces no arranca dos alarmas. */
    fun sonar(context: Context, alDetener: (() -> Unit)? = null) {
        this.alDetener = alDetener ?: this.alDetener
        if (player != null) return

        val tono = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(context.applicationContext, tono)
            isLooping = true
            prepare()
            start()
        }

        handler.postDelayed(corteDeSeguridad, DURACION_MAXIMA_MS)
        Registro.d(">>> ALARMA SONANDO <<<")
    }

    fun detener() {
        handler.removeCallbacks(corteDeSeguridad)

        player?.let { p ->
            if (p.isPlaying) p.stop()
            p.release()
        }
        player = null

        alDetener?.invoke()
        alDetener = null
        Registro.d("Alarma detenida")
    }
}
