package com.reverstabilizer.emailalarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

private const val CANAL_FUERA_DE_HORARIO = "fuera_de_horario"

/**
 * Llego un correo de una alarma, pero fuera de sus dias u horas.
 *
 * No suena ni prende la pantalla: la persona eligio ese horario justamente
 * para no despertarse. Queda una notificacion silenciosa, para que al agarrar
 * el telefono sepa que llego sin tener que buscarlo entre todos los correos.
 */
object AvisoFueraDeHorario {

    fun mostrar(context: Context, alarma: String, remitente: String, asunto: String) {
        val ctx = context.applicationContext
        val gestor = NotificationManagerCompat.from(ctx)
        if (!gestor.areNotificationsEnabled()) return

        // Importancia baja: sin sonido, sin vibrar, sin aviso flotante. Solo
        // queda en la barra de notificaciones.
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CANAL_FUERA_DE_HORARIO,
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
        val notificacion = NotificationCompat.Builder(ctx, CANAL_FUERA_DE_HORARIO)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(ctx.getString(R.string.off_hours_title, alarma))
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
            gestor.notify(CANAL_FUERA_DE_HORARIO, "$remitente|$asunto".hashCode(), notificacion)
        } catch (e: SecurityException) {
            Registro.w("Sin permiso para el aviso de fuera de horario: ${e.message}")
        }
    }
}
