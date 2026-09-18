package com.reverstabilizer.emailalarm

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// El asunto de Outlook trae tambien la vista previa del cuerpo: se recorta
// para el historial, que solo necesita reconocer el correo.
private const val LARGO_ASUNTO_HISTORIAL = 140

class MailListener : NotificationListenerService() {

    // Las consultas a Room no pueden correr en el hilo principal, y
    // onNotificationPosted llega justamente en el principal.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        Registro.d("Listener CONECTADO: ya estoy recibiendo notificaciones")
        // Se conecta al arrancar el telefono: buen momento para detectar una
        // suscripcion que vencio mientras la app no se abria, y avisar.
        scope.launch { Suscripcion.verificar(this@MailListener) }
    }

    override fun onListenerDisconnected() {
        Registro.d("Listener DESCONECTADO")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in Ajustes.appsEscuchadas(this)) return
        val app = AppsDeCorreo.porPaquete(sbn.packageName)?.nombre ?: sbn.packageName

        // Los avisos de cuenta de la app de correo ("no puedo entrar a x@hotmail.com")
        // no son correos. Suelen ser de una cuenta que la persona lee en otra app,
        // asi que mostrarlos seria ruido: se ignoran.
        MotorDeReglas.avisoDeSincronizacion(sbn)?.let { aviso ->
            Registro.d("[$app] aviso de cuenta ignorado -> $aviso")
            return
        }

        val correo = MotorDeReglas.leerCorreo(sbn) ?: return

        scope.launch {
            val reglas = BaseDeDatos.obtener(this@MailListener).reglaDao().activas()
            val regla = MotorDeReglas.primeraQueCoincide(reglas, correo)

            if (regla == null) {
                Registro.d("[$app] IGNORADO -> DE: ${correo.remitente} | ASUNTO: ${correo.asunto}")
                Registro.d("   remitente: ${correo.textoDelRemitente()}")
                registrar(app, correo.remitente, correo.asunto, Resultado.SIN_COINCIDENCIA, null)
                return@launch
            }

            Registro.d("[$app] COINCIDE regla '${regla.nombre}' -> DE: ${correo.remitente} | ASUNTO: ${correo.asunto}")

            // Si lo guardado dice "sin suscripcion", se confirma con Google Play
            // antes de silenciar: puede haber pagado recien y no estar actualizado.
            val activa = Suscripcion.estaActiva(this@MailListener) ||
                Suscripcion.verificar(this@MailListener)
            if (!activa) {
                Registro.d("   sin suscripcion activa: no suena, se avisa")
                registrar(app, correo.remitente, correo.asunto, Resultado.SIN_SUSCRIPCION, regla.nombre)
                AvisoSuscripcion.correoSinAlarma(this@MailListener, correo.remitente)
                return@launch
            }

            // Primero suena, despues se anota: el historial nunca demora la alarma.
            Alarma.disparar(this@MailListener, correo.remitente, correo.asunto, regla.sonido)
            registrar(app, correo.remitente, correo.asunto, Resultado.SONO, regla.nombre)
        }
    }

    private suspend fun registrar(
        app: String,
        remitente: String,
        asunto: String,
        resultado: Resultado,
        regla: String?
    ) {
        val dao = BaseDeDatos.obtener(this).deteccionDao()
        // La firma automatica no ayuda a reconocer el correo: se guarda sin ella.
        val texto = sinFirmasParaMostrar(asunto).take(LARGO_ASUNTO_HISTORIAL)
        dao.guardar(
            Deteccion(
                hora = System.currentTimeMillis(),
                app = app,
                remitente = remitente,
                asunto = texto,
                resultado = resultado.name,
                regla = regla
            )
        )
        dao.recortar()
    }
}
