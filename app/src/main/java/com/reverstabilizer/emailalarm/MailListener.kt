package com.reverstabilizer.emailalarm

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime

// El asunto de Outlook trae tambien la vista previa del cuerpo: se recorta
// para el historial, que solo necesita reconocer el correo.
private const val LARGO_ASUNTO_HISTORIAL = 140

class MailListener : NotificationListenerService() {

    // Las consultas a Room no pueden correr en el hilo principal, y
    // onNotificationPosted llega justamente en el principal.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val duplicados = Duplicados()

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
        if (!duplicados.esNuevo("${sbn.packageName}|${correo.remitente}|${correo.asunto}")) {
            Registro.d("[$app] repetida, se ignora -> ${correo.asunto}")
            return
        }

        scope.launch { ProcesarCorreo(this@MailListener, app, correo, LocalDateTime.now()) }
    }
}

/**
 * Lo que pasa con un correo ya leido: sonar, avisar o solo anotarlo.
 *
 * Aparte del listener para poder probar el camino real a cualquier hora
 * desde la version de desarrollo, sin esperar un correo verdadero.
 */
internal object ProcesarCorreo {

    suspend operator fun invoke(context: Context, app: String, correo: CorreoDetectado, momento: LocalDateTime) {
        val reglas = BaseDeDatos.obtener(context).reglaDao().activas()
        val regla = when (val decision = MotorDeReglas.decidir(reglas, correo, momento)) {
            Decision.Ninguna -> {
                Registro.d("[$app] IGNORADO -> DE: ${correo.remitente} | ASUNTO: ${correo.asunto}")
                Registro.d("   remitente: ${correo.textoDelRemitente()}")
                registrar(context, app, correo.remitente, correo.asunto, Resultado.SIN_COINCIDENCIA, null)
                return
            }
            // Antes que la suscripcion: fuera de horario no suena de todos
            // modos, y un aviso de suscripcion a las 6 de la manana seria
            // justo el ruido que la persona quiso evitar.
            is Decision.FueraDeHorario -> {
                Registro.d("[$app] FUERA DE HORARIO '${decision.regla.nombre}' -> ${correo.asunto}")
                registrar(context, app, correo.remitente, correo.asunto, Resultado.FUERA_DE_HORARIO, decision.regla.nombre)
                AvisoFueraDeHorario.mostrar(context, decision.regla.nombre, correo.remitente, correo.asunto)
                return
            }
            is Decision.Suena -> decision.regla
        }

        Registro.d("[$app] COINCIDE regla '${regla.nombre}' -> DE: ${correo.remitente} | ASUNTO: ${correo.asunto}")

        // Si lo guardado dice "sin suscripcion", se confirma con Google Play
        // antes de silenciar: puede haber pagado recien y no estar actualizado.
        val activa = Suscripcion.estaActiva(context) || Suscripcion.verificar(context)
        if (!activa) {
            Registro.d("   sin suscripcion activa: no suena, se avisa")
            registrar(context, app, correo.remitente, correo.asunto, Resultado.SIN_SUSCRIPCION, regla.nombre)
            AvisoSuscripcion.correoSinAlarma(context, correo.remitente)
            return
        }

        // Primero suena, despues se anota: el historial nunca demora la alarma.
        Alarma.disparar(context, correo.remitente, correo.asunto, regla.sonido)
        registrar(context, app, correo.remitente, correo.asunto, Resultado.SONO, regla.nombre)
    }

    private suspend fun registrar(
        context: Context,
        app: String,
        remitente: String,
        asunto: String,
        resultado: Resultado,
        regla: String?
    ) {
        val dao = BaseDeDatos.obtener(context).deteccionDao()
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
