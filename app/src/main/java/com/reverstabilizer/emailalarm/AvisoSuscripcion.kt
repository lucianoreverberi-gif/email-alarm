package com.reverstabilizer.emailalarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

private const val CANAL_CUENTA = "cuenta"
private const val ID_VENCIDA = 10
private const val ID_CORREO_SIN_ALARMA = 11

/**
 * Avisos normales (no alarma) sobre la suscripcion.
 *
 * Existen por un solo motivo: sin suscripcion la alarma no suena, y eso no
 * puede pasar en silencio. Si la persona no se entera, se pierde justo el
 * correo para el que instalo la app.
 */
object AvisoSuscripcion {

    /** La suscripcion estaba activa y dejo de estarlo. */
    fun suscripcionVencida(context: Context) = mostrar(
        context,
        ID_VENCIDA,
        context.getString(R.string.aviso_vencida_titulo),
        context.getString(R.string.aviso_vencida_texto)
    )

    /**
     * Llego un correo que coincide con una regla, pero no hay suscripcion activa.
     * Es el momento exacto en que la persona tiene que saberlo.
     */
    fun correoSinAlarma(context: Context, remitente: String) {
        val texto = if (Suscripcion.estado(context) == Suscripcion.Estado.VENCIDA) {
            context.getString(R.string.aviso_correo_vencida, remitente)
        } else {
            context.getString(R.string.aviso_correo_nunca, remitente)
        }
        mostrar(context, ID_CORREO_SIN_ALARMA, context.getString(R.string.aviso_correo_titulo), texto)
    }

    private fun mostrar(context: Context, id: Int, titulo: String, texto: String) {
        val ctx = context.applicationContext
        val gestor = NotificationManagerCompat.from(ctx)
        if (!gestor.areNotificationsEnabled()) return

        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CANAL_CUENTA,
                ctx.getString(R.string.canal_cuenta),
                NotificationManager.IMPORTANCE_HIGH
            )
        )

        val abrirApp = PendingIntent.getActivity(
            ctx,
            id,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion = NotificationCompat.Builder(ctx, CANAL_CUENTA)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .build()

        // Si la persona quito el permiso de notificaciones, Android lo rechaza:
        // no hay a quien avisar, pero la app no puede caerse por eso.
        try {
            gestor.notify(id, notificacion)
        } catch (e: SecurityException) {
            Registro.w("Sin permiso para avisar de la suscripcion: ${e.message}")
        }
    }
}
