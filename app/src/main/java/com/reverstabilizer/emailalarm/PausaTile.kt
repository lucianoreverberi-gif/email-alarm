package com.reverstabilizer.emailalarm

import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import android.content.Context

/**
 * Cuanto se pausa desde el mosaico. Una hora cubre una reunion, que es el caso
 * que lo pidio, y es el menor dano si la persona se equivoca: queda pausada
 * menos de lo que queria y la alarma suena antes, no despues.
 */
private val DURACION_DEL_MOSAICO = Pausa.Duracion.UNA_HORA

/**
 * Pausar las alarmas desde la cortina del telefono, sin abrir la app.
 *
 * El escenario que lo pide es "estoy entrando a la reunion y tengo diez
 * segundos". Abrir la app, tocar Pausar y elegir una duracion son tres pasos;
 * esto es uno. Por eso el mosaico no pregunta cuanto: pausa una hora y listo,
 * y para cualquier otra duracion esta la app.
 *
 * Tocarlo de nuevo reactiva, como cualquier mosaico de la cortina. El subtitulo
 * dice la hora en que vuelven, asi que no hace falta recordar cuanto falta.
 */
class PausaTile : TileService() {

    /** Android avisa cuando la cortina se abre: es el momento de poner el estado al dia. */
    override fun onStartListening() {
        pintar()
    }

    override fun onClick() {
        if (Pausa.activa(this)) Pausa.reactivar(this) else Pausa.pausar(this, DURACION_DEL_MOSAICO)
        pintar()
    }

    /**
     * El mosaico muestra lo que esta pasando, no lo que va a pasar si lo tocas.
     * Prendido = hay una pausa en curso. Es como se comportan los mosaicos del
     * sistema (No molestar, linterna), y al reves se lee como una mentira.
     */
    private fun pintar() {
        val tile = qsTile ?: return
        val hasta = Pausa.hasta(this)
        tile.state = if (hasta == 0L) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        tile.label = getString(if (hasta == 0L) R.string.tile_label else R.string.paused_title)
        tile.subtitle = when {
            hasta == 0L -> null
            hasta == PAUSA_SIN_VENCIMIENTO -> getString(R.string.tile_until_manual)
            else -> textoDeVuelta(hasta)
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_pausa)
        tile.updateTile()
    }

    companion object {

        /**
         * Pide a Android que repinte el mosaico porque la pausa cambio desde
         * otro lado (la app, la notificacion, o el vencimiento).
         *
         * Sin esto el mosaico queda diciendo "en pausa" despues de reactivar
         * desde la notificacion, hasta la proxima vez que se abra la cortina.
         *
         * No falla si la persona nunca agrego el mosaico: Android lo ignora.
         */
        fun repintar(context: Context) {
            runCatching {
                requestListeningState(
                    context.applicationContext,
                    ComponentName(context.applicationContext, PausaTile::class.java)
                )
            }
        }

        /**
         * Ofrece agregar el mosaico a la cortina con el dialogo del sistema.
         *
         * Desde Android 13. En versiones anteriores el mosaico existe igual,
         * pero hay que agregarlo a mano editando la cortina, y eso no se puede
         * explicar en una linea: ahi la fila no se muestra.
         */
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun ofrecerAgregar(context: Context) {
            val gestor = context.getSystemService(StatusBarManager::class.java) ?: return
            runCatching {
                gestor.requestAddTileService(
                    ComponentName(context.applicationContext, PausaTile::class.java),
                    context.getString(R.string.tile_label),
                    Icon.createWithResource(context, R.drawable.ic_pausa),
                    {},
                    {}
                )
            }.onFailure { Registro.d("No se pudo ofrecer el mosaico: $it") }
        }
    }
}
