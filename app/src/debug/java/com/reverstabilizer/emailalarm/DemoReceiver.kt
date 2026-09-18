package com.reverstabilizer.emailalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * SOLO EN DEBUG: vive en src/debug, asi que no existe en el APK publicado.
 *
 * Carga reglas de ejemplo y abre la pantalla de alarma a pedido, para sacar
 * capturas prolijas de la app (sitio y ficha de Play) sin tipear a mano.
 *
 *   adb shell am broadcast -n com.reverstabilizer.emailalarm/.DemoReceiver \
 *       --es accion reglas --es idioma en
 *
 * accion: reglas | historial | alarma | detener | pro | sinpro      idioma: en | es
 *
 * "pro" simula una suscripcion activa (solo debug), para capturas y para usar
 * la app antes de que exista el producto en Play Console. "sinpro" la quita.
 */
class DemoReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val espanol = intent.getStringExtra("idioma") == "es"
        when (intent.getStringExtra("accion")) {
            "reglas" -> cargarReglas(context, espanol)
            "historial" -> cargarHistorial(context, espanol)
            "alarma" -> mostrarAlarma(context, espanol)
            "detener" -> Alarma.detener()
            // Dos alarmas en el mismo instante desde dos hilos, como cuando
            // Gmail publica la misma notificacion dos veces. Tiene que quedar
            // un solo reproductor, y DETENER tiene que callarla.
            "doble" -> repeat(2) {
                Thread { Alarma.disparar(context, "Prueba doble", "Dos a la vez") }.start()
            }
            "pro" -> Suscripcion.simularActiva(context, true)
            "sinpro" -> Suscripcion.simularActiva(context, false)
        }
    }

    private fun cargarReglas(context: Context, espanol: Boolean) {
        val reglas = if (espanol) {
            listOf(
                Regla(nombre = "Cliente: Acme", remitente = "Acme", palabraClave = "propuesta"),
                Regla(nombre = "Cita de migraciones", palabraClave = "USCIS"),
                Regla(nombre = "Turnos del restaurante", remitente = "turnos@bistro.com")
            )
        } else {
            listOf(
                Regla(nombre = "Client: Acme", remitente = "Acme", palabraClave = "proposal"),
                Regla(nombre = "Immigration appointment", palabraClave = "USCIS"),
                Regla(nombre = "Restaurant shifts", remitente = "shifts@bistro.com")
            )
        }

        val pendiente = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val dao = BaseDeDatos.obtener(context).reglaDao()
            dao.observarTodas().first().forEach { dao.borrar(it) }
            reglas.forEach { dao.guardar(it) }
            pendiente.finish()
        }
    }

    /** Correos de ejemplo para el historial: uno que sono, varios que no. */
    private fun cargarHistorial(context: Context, espanol: Boolean) {
        val ahora = System.currentTimeMillis()
        val min = 60_000L
        val ejemplos = if (espanol) {
            listOf(
                Deteccion(hora = ahora - 3 * min, app = "Gmail", remitente = "Turnos del restaurante",
                    asunto = "Ya están abiertos los turnos de la semana que viene",
                    resultado = Resultado.SONO.name, regla = "Turnos del restaurante"),
                Deteccion(hora = ahora - 41 * min, app = "Gmail", remitente = "Chase",
                    asunto = "Tu estado de cuenta está disponible", resultado = Resultado.SIN_COINCIDENCIA.name),
                Deteccion(hora = ahora - 95 * min, app = "Gmail", remitente = "USCIS Online Account",
                    asunto = "USCIS: se actualizó el estado de tu caso",
                    resultado = Resultado.SONO.name, regla = "Cita de migraciones"),
                Deteccion(hora = ahora - 160 * min, app = "Gmail", remitente = "Amazon",
                    asunto = "Tu pedido está en camino", resultado = Resultado.SIN_COINCIDENCIA.name)
            )
        } else {
            listOf(
                Deteccion(hora = ahora - 3 * min, app = "Gmail", remitente = "Shift Scheduler",
                    asunto = "Next week's shifts are open — sign up now",
                    resultado = Resultado.SONO.name, regla = "Restaurant shifts"),
                Deteccion(hora = ahora - 41 * min, app = "Gmail", remitente = "Chase",
                    asunto = "Your statement is ready", resultado = Resultado.SIN_COINCIDENCIA.name),
                Deteccion(hora = ahora - 95 * min, app = "Gmail", remitente = "USCIS Online Account",
                    asunto = "USCIS: your case status was updated",
                    resultado = Resultado.SONO.name, regla = "Immigration appointment"),
                Deteccion(hora = ahora - 160 * min, app = "Gmail", remitente = "Amazon",
                    asunto = "Your package is on the way", resultado = Resultado.SIN_COINCIDENCIA.name)
            )
        }
        val pendiente = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val dao = BaseDeDatos.obtener(context).deteccionDao()
            dao.borrarTodo()
            ejemplos.forEach { dao.guardar(it) }
            pendiente.finish()
        }
    }

    private fun mostrarAlarma(context: Context, espanol: Boolean) {
        val (remitente, asunto) = if (espanol) {
            "Turnos del restaurante" to "Ya están abiertos los turnos de la semana que viene"
        } else {
            "Shift Scheduler" to "Next week's shifts are open — sign up now"
        }
        context.startActivity(
            Intent(context, AlarmaActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_REMITENTE, remitente)
                .putExtra(EXTRA_ASUNTO, asunto)
        )
    }
}
