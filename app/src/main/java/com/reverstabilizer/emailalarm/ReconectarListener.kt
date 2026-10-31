package com.reverstabilizer.emailalarm

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService

/**
 * Vuelve a enganchar el listener despues de actualizar la app.
 *
 * Al instalar una version nueva, Android mata el NotificationListenerService.
 * Deberia volver a conectarlo solo, pero en varias versiones y en varios
 * fabricantes no lo hace: el permiso sigue concedido, la app sigue diciendo
 * que escucha, y no llega una sola notificacion hasta que la persona apaga y
 * prende el permiso a mano. Nadie va a adivinar eso.
 *
 * requestRebind es justamente para esto. Es barato y no molesta: si el sistema
 * ya reconecto, no hace nada.
 */
class ReconectarListener : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        Registro.d("App actualizada: pido reconectar el listener")
        runCatching {
            NotificationListenerService.requestRebind(
                ComponentName(context, MailListener::class.java)
            )
        }.onFailure { Registro.d("No se pudo pedir el rebind: $it") }
    }
}
