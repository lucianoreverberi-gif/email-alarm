package com.reverstabilizer.emailalarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

private const val CANAL_NO_SONO = "fuera_de_horario"

/**
 * Llego un correo de una alarma, pero no corresponde que suene: o esta fuera de
 * los dias y horas de esa alarma, o la persona puso todas las alarmas en pausa.
 *
 * No suena ni prende la pantalla, justamente porque eso es lo que se pidio.
 * Queda una notificacion silenciosa, para que al agarrar el telefono sepa que
 * llego sin tener que buscarlo entre todos los correos. Es lo que hace que una
 * pausa no sea quedarse sordo: silencia el ruido, no la informacion.
 *
 * Un solo canal para los dos casos. Desde la barra de notificaciones son lo
 * mismo ("coincidio y no sono"), dos canales serian dos renglones que dicen
 * casi igual en los ajustes de Android, y el id de un canal no se puede cambiar
 * una vez creado: el de este quedo con el nombre viejo y no se toca.
 */
object AvisoSilencioso {

    fun mostrar(
        context: Context,
        alarma: String,
        remitente: String,
        asunto: String,
        enPausa: Boolean = false
    ) {
        val ctx = context.applicationContext
        val gestor = NotificationManagerCompat.from(ctx)
        if (!gestor.areNotificationsEnabled()) return

        // Importancia baja: sin sonido, sin vibrar, sin aviso flotante. Solo
        // queda en la barra de notificaciones.
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CANAL_NO_SONO,
                ctx.getString(R.string.channel_off_hours),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = ctx.getString(R.string.channel_off_hours_description) }
        )

        val abrirApp = PendingIntent.getActivity(
            ctx,
            0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val texto = "$remitente — ${sinFirmasParaMostrar(asunto)}"
        val titulo = if (enPausa) R.string.paused_arrived_title else R.string.off_hours_title
        val notificacion = NotificationCompat.Builder(ctx, CANAL_NO_SONO)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(ctx.getString(titulo, alarma))
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .build()

        // Uno por correo: si llegan dos de noche, a la manana se ven los dos.
        try {
            // Con etiqueta propia, el id no puede pisar la notificacion de la alarma.
            gestor.notify(CANAL_NO_SONO, "$remitente|$asunto".hashCode(), notificacion)
        } catch (e: SecurityException) {
            Registro.w("Sin permiso para el aviso silencioso: ${e.message}")
        }
    }
}
