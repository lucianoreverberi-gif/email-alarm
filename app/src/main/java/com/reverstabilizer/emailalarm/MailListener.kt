package com.reverstabilizer.emailalarm

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


class MailListener : NotificationListenerService() {

    // Las consultas a Room no pueden correr en el hilo principal, y
    // onNotificationPosted llega justamente en el principal.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        Registro.d("Listener CONECTADO: ya estoy recibiendo notificaciones")
    }

    override fun onListenerDisconnected() {
        Registro.d("Listener DESCONECTADO")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in Ajustes.appsEscuchadas(this)) return

        MotorDeReglas.avisoDeSincronizacion(sbn)?.let { aviso ->
            Registro.w("CUENTA CON PROBLEMA DE SINCRONIZACION -> $aviso")
            return
        }

        val correo = MotorDeReglas.leerCorreo(sbn) ?: return
        val app = AppsDeCorreo.porPaquete(correo.paquete)?.nombre ?: correo.paquete

        scope.launch {
            val reglas = BaseDeDatos.obtener(this@MailListener).reglaDao().activas()
            val regla = MotorDeReglas.primeraQueCoincide(reglas, correo)

            if (regla == null) {
                Registro.d("[$app] IGNORADO -> DE: ${correo.remitente} | ASUNTO: ${correo.asunto}")
                Registro.d("   remitente: ${correo.textoDelRemitente()}")
                return@launch
            }

            Registro.d("[$app] COINCIDE regla '${regla.nombre}' -> DE: ${correo.remitente} | ASUNTO: ${correo.asunto}")
            Alarma.disparar(this@MailListener, correo.remitente, correo.asunto)
        }
    }
}
