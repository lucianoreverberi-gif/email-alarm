package com.reverstabilizer.emailalarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
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

    // Todos los reproductores que se arrancaron y no se pararon. Deberia haber
    // uno como mucho, pero si alguna vez hubiera dos, DETENER los para a todos:
    // una alarma que no se puede callar es el peor error que puede tener la app.
    private val activos = mutableListOf<MediaPlayer>()
    private val handler = Handler(Looper.getMainLooper())
    private val corteDeSeguridad = Runnable { detener() }
    private var alDetener: (() -> Unit)? = null

    @Synchronized
    fun estaSonando(): Boolean = activos.isNotEmpty()

    /**
     * Idempotente: llamarla dos veces no arranca dos alarmas, aunque las dos
     * llamadas lleguen a la vez desde hilos distintos (Gmail a veces publica
     * la misma notificacion dos veces en el mismo instante). Por eso es
     * @Synchronized: sin eso, las dos veian "no hay nada sonando" y cada una
     * arrancaba su reproductor.
     *
     * [sonido] es el tono elegido en la regla (una Uri en texto), o null para
     * el de alarma del sistema. Si el elegido falla, suena el del sistema:
     * un tono borrado o sin permiso de lectura nunca puede dejarla muda.
     */
    @Synchronized
    fun sonar(context: Context, sonido: String? = null, alDetener: (() -> Unit)? = null) {
        this.alDetener = alDetener ?: this.alDetener
        if (activos.isNotEmpty()) return

        val ctx = context.applicationContext
        val delSistema = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val elegido = sonido?.let { runCatching { Uri.parse(it) }.getOrNull() }

        val player = crear(ctx, elegido) ?: crear(ctx, delSistema)
        if (player == null) {
            Registro.w("No se pudo reproducir ningun tono")
            return
        }
        activos += player

        handler.postDelayed(corteDeSeguridad, DURACION_MAXIMA_MS)
        Registro.d(">>> ALARMA SONANDO <<<")
    }

    /** Un reproductor en bucle por el canal de alarma, o null si ese tono no anda. */
    private fun crear(ctx: Context, tono: Uri?): MediaPlayer? {
        if (tono == null) return null
        val mp = MediaPlayer()
        return try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.setDataSource(ctx, tono)
            mp.isLooping = true
            mp.prepare()
            mp.start()
            mp
        } catch (e: Exception) {
            Registro.w("El tono $tono no se pudo reproducir: ${e.message}")
            mp.release()
            null
        }
    }

    @Synchronized
    fun detener() {
        handler.removeCallbacks(corteDeSeguridad)

        activos.forEach { p ->
            // Uno que fallo al parar no puede impedir que se paren los demas.
            runCatching { if (p.isPlaying) p.stop() }
            runCatching { p.release() }
        }
        activos.clear()

        alDetener?.invoke()
        alDetener = null
        Registro.d("Alarma detenida")
    }
}
