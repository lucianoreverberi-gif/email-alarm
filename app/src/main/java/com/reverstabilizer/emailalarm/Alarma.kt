package com.reverstabilizer.emailalarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

// El id del canal lleva version a proposito: una vez creado, Android congela
// sus ajustes y los cambios del codigo se ignoran. Para estrenar ajustes
// (bypass de No Molestar, importancia) hay que estrenar canal.
private const val CANAL_ALARMA = "alarma_mail_v2"
private const val ID_NOTIFICACION = 1

const val EXTRA_REMITENTE = "remitente"
const val EXTRA_ASUNTO = "asunto"

/**
 * Decide como se manifiesta la alarma.
 *
 * Dispara dos cosas a la vez, y es a proposito:
 *
 * 1. El sonido, ya mismo. Nunca depende de que algo mas salga bien.
 * 2. Una notificacion con full-screen intent, que abre [AlarmaActivity] por
 *    encima de la pantalla de bloqueo.
 *
 * Android decide solo si el full-screen intent abre la pantalla (telefono
 * bloqueado o en reposo) o si la muestra como aviso flotante (telefono en uso).
 * Como el sonido no depende de eso, cualquiera de los dos caminos suena.
 */
object Alarma {

    fun disparar(context: Context, remitente: String, asunto: String, sonido: String? = null) {
        val ctx = context.applicationContext
        // Primero el sonido: si algo de la notificacion falla, la alarma ya suena.
        AlarmPlayer.sonar(ctx, sonido) { NotificationManagerCompat.from(ctx).cancel(ID_NOTIFICACION) }
        crearCanal(ctx)
        mostrarNotificacion(ctx, remitente, asunto)
    }

    /**
     * Alarma de prueba, ya: abre la pantalla completa y suena. Con la app
     * abierta Android no deja lanzar la pantalla por notificacion (la muestra
     * como aviso flotante), asi que se abre directo.
     *
     * No mira la suscripcion: sirve justamente para confiar antes de pagar.
     */
    fun probarAhora(context: Context) {
        val ctx = context.applicationContext
        AlarmPlayer.sonar(ctx)
        ctx.startActivity(
            Intent(ctx, AlarmaActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_REMITENTE, ctx.getString(R.string.test_alarm_sender))
                .putExtra(EXTRA_ASUNTO, ctx.getString(R.string.test_alarm_subject))
        )
    }

    /**
     * Alarma de prueba en [segundos], por el camino real (notificacion de
     * pantalla completa): da tiempo a bloquear el telefono y prueba lo que
     * de verdad importa, que suene con el telefono bloqueado.
     */
    fun probarDespues(context: Context, segundos: Long = 10) {
        val ctx = context.applicationContext
        Handler(Looper.getMainLooper()).postDelayed({
            disparar(
                ctx,
                ctx.getString(R.string.test_alarm_sender),
                ctx.getString(R.string.test_alarm_subject)
            )
        }, segundos * 1000)
    }

    fun detener() = AlarmPlayer.detener()

    private fun crearCanal(ctx: Context) {
        val gestor = ctx.getSystemService(NotificationManager::class.java)
        // El canal de la version anterior queda visible en los ajustes del
        // sistema y confunde. Como ya no se usa, se borra.
        gestor.deleteNotificationChannel("alarma_mail")

        val canal = NotificationChannel(
            CANAL_ALARMA,
            ctx.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = ctx.getString(R.string.channel_description)
            // El sonido lo pone AlarmPlayer, para que vaya en bucle y por el
            // canal de volumen de alarma.
            setSound(null, null)
            enableVibration(true)
            // Solo tiene efecto si el usuario dio acceso a la politica de
            // notificaciones. Si no lo dio, Android lo ignora sin fallar.
            setBypassDnd(true)
        }
        gestor.createNotificationChannel(canal)
    }

    private fun mostrarNotificacion(ctx: Context, remitente: String, asunto: String) {
        if (!puedeNotificar(ctx)) {
            Registro.w("Sin permiso para notificar: suena, pero sin pantalla de alarma")
            return
        }

        val pantallaDeAlarma = PendingIntent.getActivity(
            ctx,
            0,
            Intent(ctx, AlarmaActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra(EXTRA_REMITENTE, remitente)
                .putExtra(EXTRA_ASUNTO, asunto),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val detener = PendingIntent.getBroadcast(
            ctx,
            1,
            Intent(ctx, DetenerAlarmaReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion = NotificationCompat.Builder(ctx, CANAL_ALARMA)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(remitente)
            .setContentText(asunto)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(pantallaDeAlarma)
            .setFullScreenIntent(pantallaDeAlarma, true)
            .addAction(android.R.drawable.ic_lock_idle_alarm, ctx.getString(R.string.action_stop), detener)
            .build()

        // El permiso se puede quitar entre la revision de arriba y este momento.
        // Sin notificacion no hay pantalla de alarma, pero el sonido ya esta andando.
        try {
            NotificationManagerCompat.from(ctx).notify(ID_NOTIFICACION, notificacion)
        } catch (e: SecurityException) {
            Registro.w("Android rechazo la notificacion de alarma: ${e.message}")
        }
    }

    private fun puedeNotificar(ctx: Context): Boolean =
        NotificationManagerCompat.from(ctx).areNotificationsEnabled()
}

/** Recibe el toque del boton DETENER de la notificacion. */
class DetenerAlarmaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Alarma.detener()
    }
}
