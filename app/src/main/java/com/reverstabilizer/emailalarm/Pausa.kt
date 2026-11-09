package com.reverstabilizer.emailalarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

private const val CANAL_PAUSA = "pausa"

/** No puede chocar con la alarma (1) ni con los avisos silenciosos (van con etiqueta). */
private const val ID_PAUSA = 2

private const val ACCION_REACTIVAR = "com.reverstabilizer.emailalarm.REACTIVAR"
private const val ACCION_VENCIO = "com.reverstabilizer.emailalarm.PAUSA_VENCIDA"

/** Codigos de PendingIntent propios de la pausa: el 0 y el 1 ya los usa la alarma. */
private const val PEDIDO_REACTIVAR = 2
private const val PEDIDO_VENCIMIENTO = 3

/** Una pausa sin hora de vuelta: dura hasta que la persona la reactive. */
const val PAUSA_SIN_VENCIMIENTO = Long.MAX_VALUE

/**
 * Si una pausa guardada todavia corre. Aparte y sin Context para poder probarla.
 *
 * 0 es "no hay pausa", y con [PAUSA_SIN_VENCIMIENTO] siempre devuelve true sin
 * necesitar un caso especial: ninguna hora real le llega.
 */
internal fun pausaVigente(hasta: Long, ahora: Long): Boolean = hasta > ahora

/**
 * Callar todas las alarmas de una vez, por un rato.
 *
 * Nace de un tester que entraba a una reunion importante: la unica forma de
 * callar la app era apagar alarma por alarma y despues acordarse de prenderlas
 * una por una. Con dos alarmas es molesto; con siete no lo hace nadie.
 *
 * Siempre con hora de vuelta, nunca un interruptor a secas. El peor error
 * posible en esta app es pausar para una reunion, olvidarse, y perderse tres
 * dias despues el correo por el que se pago: que suene en un momento incomodo
 * es un papelon, que no suene es que la app fallo en lo unico que hace. Contra
 * eso van tres cosas:
 *
 * 1. Se reactiva sola. La hora se mira en el momento en que llega el correo,
 *    asi que no depende de que ningun temporizador haya corrido: aunque Android
 *    mate el proceso, al vencer la pausa la alarma vuelve a sonar.
 * 2. Mientras dura queda un aviso fijo en la barra de notificaciones, con la
 *    hora de vuelta y un boton para reactivar. Se ve en la pantalla de bloqueo,
 *    que es donde la persona lo va a ver sin abrir la app.
 * 3. Los correos que coinciden mientras esta pausada dejan su aviso silencioso,
 *    igual que los que llegan fuera de horario. La pausa silencia el ruido, no
 *    la informacion.
 */
object Pausa {

    // El mismo archivo que [Ajustes], a proposito: es una preferencia mas, y
    // asi viaja con las demas cuando se cambia de telefono.
    private const val ARCHIVO = "ajustes"
    private const val CLAVE_HASTA = "pausa_hasta"

    /** Las duraciones que se ofrecen, en el orden en que se muestran. */
    enum class Duracion(val minutos: Long?) {
        MEDIA_HORA(30),
        UNA_HORA(60),
        DOS_HORAS(120),
        OCHO_HORAS(480),

        /** Sin hora de vuelta. Sigue avisando en la barra para que no se olvide. */
        INDEFINIDA(null);

        fun hasta(ahora: Long): Long =
            if (minutos == null) PAUSA_SIN_VENCIMIENTO else ahora + minutos * 60_000L
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    /**
     * Hasta cuando estan pausadas las alarmas, o 0 si no hay pausa.
     *
     * Una pausa vencida se limpia aca mismo: el temporizador que saca el aviso
     * de la barra puede no haber corrido (Android lo puede demorar o el
     * telefono puede haberse reiniciado), y entonces esta es la unica parte que
     * se entera. Preguntar por la pausa nunca puede devolver "si" de mas.
     */
    fun hasta(context: Context, ahora: Long = System.currentTimeMillis()): Long {
        val hasta = prefs(context).getLong(CLAVE_HASTA, 0L)
        if (hasta == 0L) return 0L
        if (pausaVigente(hasta, ahora)) return hasta
        Registro.d("La pausa ya vencio: las alarmas vuelven a sonar")
        reactivar(context)
        return 0L
    }

    fun activa(context: Context, ahora: Long = System.currentTimeMillis()): Boolean =
        hasta(context, ahora) != 0L

    fun pausar(context: Context, duracion: Duracion, ahora: Long = System.currentTimeMillis()) {
        val ctx = context.applicationContext
        val hasta = duracion.hasta(ahora)
        prefs(ctx).edit().putLong(CLAVE_HASTA, hasta).apply()
        Registro.d("Alarmas en pausa (${duracion.name})")
        AvisoDePausa.mostrar(ctx, hasta)
        programarVencimiento(ctx, hasta)
        PausaTile.repintar(ctx)
    }

    /** Idempotente: la puede llamar la persona, el temporizador o [hasta]. */
    fun reactivar(context: Context) {
        val ctx = context.applicationContext
        prefs(ctx).edit().remove(CLAVE_HASTA).apply()
        AvisoDePausa.ocultar(ctx)
        cancelarVencimiento(ctx)
        // Si no, el mosaico de la cortina queda diciendo "en pausa" hasta la
        // proxima vez que alguien la abra.
        PausaTile.repintar(ctx)
    }

    /**
     * Repone el aviso y el temporizador despues de reiniciar el telefono o de
     * actualizar la app.
     *
     * Android borra las notificaciones y los temporizadores al reiniciar, pero
     * la pausa guardada sigue ahi. Sin esto, una pausa "hasta que yo las
     * reactive" sobreviviria al reinicio sin nada a la vista que lo recuerde:
     * justo la trampa que esta pausa esta hecha para no ser. Las alarmas no
     * dependen de esto (la hora se mira al llegar el correo), pero la red de
     * seguridad si.
     *
     * Barato y sin efecto si no hay pausa, asi que se puede llamar siempre.
     */
    fun restaurar(context: Context) {
        val hasta = hasta(context)
        if (hasta == 0L) return
        val ctx = context.applicationContext
        Registro.d("Habia una pausa en curso: se repone el aviso")
        AvisoDePausa.mostrar(ctx, hasta)
        programarVencimiento(ctx, hasta)
    }

    /**
     * Temporizador para sacar el aviso de la barra cuando vence la pausa.
     *
     * Inexacto a proposito: lo unico que hace es limpiar la notificacion. Que
     * una alarma suene o no nunca depende de esto, se decide mirando la hora
     * guardada, asi que un temporizador demorado o perdido no deja la app
     * sorda. Por eso tampoco pide SCHEDULE_EXACT_ALARM, que Google Play mira
     * con lupa y que no haria falta para algo cosmetico.
     */
    private fun programarVencimiento(ctx: Context, hasta: Long) {
        cancelarVencimiento(ctx)
        if (hasta == PAUSA_SIN_VENCIMIENTO) return
        val gestor = ctx.getSystemService(AlarmManager::class.java) ?: return
        // allowWhileIdle para que no quede para despues si el telefono esta en reposo.
        gestor.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, hasta, intentDeVencimiento(ctx))
    }

    private fun cancelarVencimiento(ctx: Context) {
        ctx.getSystemService(AlarmManager::class.java)?.cancel(intentDeVencimiento(ctx))
    }

    private fun intentDeVencimiento(ctx: Context): PendingIntent =
        PendingIntent.getBroadcast(
            ctx,
            PEDIDO_VENCIMIENTO,
            Intent(ctx, PausaReceiver::class.java).setAction(ACCION_VENCIO),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}

/**
 * El aviso fijo mientras las alarmas estan en pausa.
 *
 * Es la pieza que hace que la pausa sea segura: sin esto, pausar y olvidarse
 * es cuestion de tiempo. Va con importancia baja (no suena ni vibra) pero
 * fijo, para que no se pueda descartar de un manotazo sin querer.
 */
private object AvisoDePausa {

    fun mostrar(ctx: Context, hasta: Long) {
        val gestor = NotificationManagerCompat.from(ctx)
        if (!gestor.areNotificationsEnabled()) return

        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CANAL_PAUSA,
                ctx.getString(R.string.channel_paused),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = ctx.getString(R.string.channel_paused_description) }
        )

        val abrirApp = PendingIntent.getActivity(
            ctx,
            PEDIDO_REACTIVAR,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val reactivar = PendingIntent.getBroadcast(
            ctx,
            PEDIDO_REACTIVAR,
            Intent(ctx, PausaReceiver::class.java).setAction(ACCION_REACTIVAR),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val texto = if (hasta == PAUSA_SIN_VENCIMIENTO) {
            ctx.getString(R.string.paused_until_manual)
        } else {
            ctx.getString(R.string.paused_until, textoDeVuelta(hasta))
        }

        val notificacion = NotificationCompat.Builder(ctx, CANAL_PAUSA)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(ctx.getString(R.string.paused_title))
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setSilent(true)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(abrirApp)
            .addAction(android.R.drawable.ic_media_play, ctx.getString(R.string.action_resume), reactivar)
            .build()

        try {
            gestor.notify(ID_PAUSA, notificacion)
        } catch (e: SecurityException) {
            // Sin el aviso la pausa sigue funcionando y venciendo sola, pero se
            // pierde la red de seguridad: queda anotado para poder diagnosticarlo.
            Registro.w("Sin permiso para el aviso de pausa: ${e.message}")
        }
    }

    fun ocultar(ctx: Context) = NotificationManagerCompat.from(ctx).cancel(ID_PAUSA)
}

/**
 * La hora en que vuelven las alarmas, como la escribe el telefono (12 o 24
 * horas segun lo que tenga puesto). Si cae otro dia se muestra tambien el dia:
 * una pausa de ocho horas a las once de la noche vuelve manana.
 */
internal fun textoDeVuelta(hasta: Long): String =
    DateUtils.formatSameDayTime(
        hasta,
        System.currentTimeMillis(),
        java.text.DateFormat.SHORT,
        java.text.DateFormat.SHORT
    ).toString()

/** Recibe el boton REACTIVAR del aviso y el vencimiento de la pausa. */
class PausaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACCION_REACTIVAR -> {
                Registro.d("Reactivada desde la notificacion")
                Pausa.reactivar(context)
            }
            ACCION_VENCIO -> {
                Registro.d("Vencio la pausa: se saca el aviso")
                Pausa.reactivar(context)
            }
        }
    }
}
