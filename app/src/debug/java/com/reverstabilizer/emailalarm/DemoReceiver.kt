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
 * accion: reglas | alarma | detener      idioma: en | es
 */
class DemoReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val espanol = intent.getStringExtra("idioma") == "es"
        when (intent.getStringExtra("accion")) {
            "reglas" -> cargarReglas(context, espanol)
            "alarma" -> mostrarAlarma(context, espanol)
            "detener" -> Alarma.detener()
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
