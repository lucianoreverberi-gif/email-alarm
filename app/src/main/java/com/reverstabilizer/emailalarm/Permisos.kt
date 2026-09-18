package com.reverstabilizer.emailalarm

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Un permiso que la app necesita, con su estado y como pedirlo.
 *
 * [titulo] y [explicacion] son ids de recurso, no texto: asi el idioma lo
 * resuelve la capa de UI y no queda castellano incrustado en la logica.
 *
 * [imprescindible] marca los que, si faltan, hacen que la alarma directamente
 * no suene. Los otros la degradan pero no la rompen.
 */
data class EstadoDePermiso(
    val clave: String,
    @StringRes val titulo: Int,
    @StringRes val explicacion: Int,
    val concedido: Boolean,
    val imprescindible: Boolean,
    val intent: Intent?
)

object Permisos {

    fun revisarTodos(context: Context): List<EstadoDePermiso> = listOf(
        accesoANotificaciones(context),
        notificacionesPropias(context),
        pantallaCompleta(context),
        noMolestar(context),
        bateria(context)
    )

    /** Sin esto la app no ve ningun correo. Es el unico verdaderamente obligatorio. */
    private fun accesoANotificaciones(context: Context) = EstadoDePermiso(
        clave = "listener",
        titulo = R.string.perm_listener_title,
        explicacion = R.string.perm_listener_body,
        concedido = NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName),
        imprescindible = true,
        intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    )

    /** Sin esto no se puede mostrar la alarma ni el boton de detener. */
    private fun notificacionesPropias(context: Context): EstadoDePermiso {
        val concedido = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return EstadoDePermiso(
            clave = "notificaciones",
            titulo = R.string.perm_post_title,
            explicacion = R.string.perm_post_body,
            concedido = concedido,
            imprescindible = true,
            intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        )
    }

    /**
     * Desde Android 14 este permiso dejo de darse solo: hay que pedirselo al
     * usuario. Sin el, la alarma no toma la pantalla con el telefono bloqueado.
     */
    private fun pantallaCompleta(context: Context): EstadoDePermiso {
        val gestor = context.getSystemService(NotificationManager::class.java)
        val concedido = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            gestor.canUseFullScreenIntent()
        return EstadoDePermiso(
            clave = "fullscreen",
            titulo = R.string.perm_fullscreen_title,
            explicacion = R.string.perm_fullscreen_body,
            concedido = concedido,
            imprescindible = false,
            intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                    .setData(Uri.parse("package:${context.packageName}"))
            } else {
                null
            }
        )
    }

    /** Sin esto, No Molestar silencia la alarma. */
    private fun noMolestar(context: Context) = EstadoDePermiso(
        clave = "dnd",
        titulo = R.string.perm_dnd_title,
        explicacion = R.string.perm_dnd_body,
        concedido = context.getSystemService(NotificationManager::class.java)
            .isNotificationPolicyAccessGranted,
        imprescindible = false,
        intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
    )

    /** Samsung y otros duermen las apps que no se usan. Una alarma dormida no suena. */
    private fun bateria(context: Context): EstadoDePermiso {
        val energia = context.getSystemService(PowerManager::class.java)
        return EstadoDePermiso(
            clave = "bateria",
            titulo = R.string.perm_battery_title,
            explicacion = R.string.perm_battery_body,
            concedido = energia.isIgnoringBatteryOptimizations(context.packageName),
            imprescindible = false,
            // Se abre la lista y el usuario elige la app: pedirlo con un dialogo
            // directo requiere un permiso que Google Play restringe.
            intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        )
    }
}
