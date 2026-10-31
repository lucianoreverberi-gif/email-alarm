package com.reverstabilizer.emailalarm

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings

data class AppCorreo(
    val paquete: String,
    val nombre: String,
    /** true si esa app publica la direccion del remitente, no solo el nombre. */
    val exponeDireccion: Boolean
)

/**
 * Apps de correo que sabemos leer. El resto del mundo no tiene una forma
 * estandar de publicar el remitente en la notificacion, asi que se van
 * agregando de a una, verificando que traen en los extras.
 */
// Si agregas una app aca, agregala tambien en <queries> del AndroidManifest.xml,
// o Android no la deja ver y la app cree que no esta instalada.
private val CONOCIDAS = listOf(
    AppCorreo("com.google.android.gm", "Gmail", exponeDireccion = true),
    AppCorreo("com.microsoft.office.outlook", "Outlook", exponeDireccion = false),
    AppCorreo("com.samsung.android.email.provider", "Samsung Email", exponeDireccion = false),
    AppCorreo("com.yahoo.mobile.client.android.mail", "Yahoo Mail", exponeDireccion = false),
    AppCorreo("ch.protonmail.android", "Proton Mail", exponeDireccion = false),
    AppCorreo("me.bluemail.mail", "BlueMail", exponeDireccion = false),
    AppCorreo("com.fsck.k9", "K-9 Mail", exponeDireccion = false)
)

object AppsDeCorreo {

    /** Las que estan realmente instaladas en este telefono. */
    fun instaladas(context: Context): List<AppCorreo> {
        val pm = context.packageManager
        return CONOCIDAS.filter { app ->
            runCatching { pm.getPackageInfo(app.paquete, 0) }.isSuccess
        }
    }

    fun porPaquete(paquete: String): AppCorreo? = CONOCIDAS.find { it.paquete == paquete }

    /** El historial guarda el nombre de la app; esto recupera el paquete para su icono. */
    fun porNombre(nombre: String): AppCorreo? = CONOCIDAS.find { it.nombre == nombre }

    /**
     * Lleva a los ajustes de notificaciones de la app de correo.
     *
     * Si esa app tiene las notificaciones apagadas, aca no llega nada y la
     * alarma nunca suena. Android no deja consultar los ajustes de un tercero,
     * asi que no hay forma de detectarlo ni de arreglarlo desde el codigo: lo
     * unico posible es abrirle la pantalla a la persona.
     *
     * Algunos fabricantes no atienden el intent directo; de ahi el respaldo a
     * la ficha de la aplicacion, que siempre existe.
     */
    fun abrirNotificaciones(context: Context, paquete: String) {
        val directo = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, paquete)
        val respaldo = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", paquete, null))
        runCatching { context.startActivity(directo) }
            .onFailure { runCatching { context.startActivity(respaldo) } }
    }
}

/**
 * Cuanto silencio hace falta para sospechar que la app de correo tiene las
 * notificaciones apagadas.
 *
 * Dos dias es a proposito generoso. Con menos, a cualquiera que se tome un fin
 * de semana sin correos le saltaria un aviso que no corresponde, y un aviso que
 * aparece cuando todo esta bien deja de significar algo.
 *
 * Si cambia, hay que cambiar tambien los textos no_signal_*, que dicen "dos dias".
 */
internal const val SILENCIO_SOSPECHOSO = 48L * 60 * 60 * 1000

/**
 * Si de una app de correo no llego nada en mucho tiempo.
 *
 * Aparte para poder probarla: la decision es lo unico que importa y no necesita
 * ni Android ni preferencias.
 *
 * [escuchaDesde] en 0 significa que todavia no sabemos desde cuando escuchamos
 * (recien actualizada, o el listener nunca se conecto): sin esa referencia dos
 * dias de silencio no prueban nada. Igual que recien empezado a escuchar: hace
 * falta la ventana completa antes de concluir algo.
 */
internal fun haySilencio(escuchaDesde: Long, ultimaSenal: Long, ahora: Long): Boolean {
    if (escuchaDesde == 0L) return false
    if (ahora - escuchaDesde < SILENCIO_SOSPECHOSO) return false
    return ahora - ultimaSenal >= SILENCIO_SOSPECHOSO
}

/** Que apps estamos escuchando. Es una preferencia simple, no va a la base. */
object Ajustes {

    private const val ARCHIVO = "ajustes"
    private const val CLAVE_APPS = "apps_escuchadas"
    private const val CLAVE_ESCUCHA_DESDE = "escucha_desde"
    private const val PREFIJO_SENAL = "senal_"

    private fun prefs(context: Context) =
        context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    /**
     * Desde cuando estamos escuchando. Se anota una sola vez, la primera que el
     * listener se conecta, y es la referencia para no avisar de un silencio que
     * en realidad es "la app es nueva".
     */
    fun anotarQueEscucha(context: Context) {
        val p = prefs(context)
        if (p.getLong(CLAVE_ESCUCHA_DESDE, 0L) != 0L) return
        p.edit().putLong(CLAVE_ESCUCHA_DESDE, System.currentTimeMillis()).apply()
    }

    /**
     * Llego algo de esta app de correo.
     *
     * Se anota antes de leer la notificacion, no despues: lo que interesa es si
     * la app de correo esta emitiendo, y una notificacion que nuestro lector no
     * entiende tambien lo prueba. Si solo contaramos las que entendemos, un
     * formato nuevo de Outlook se veria igual que Gmail apagado, y mandariamos a
     * la persona a revisar unos ajustes que estan bien.
     */
    fun anotarSenal(context: Context, paquete: String) {
        prefs(context).edit()
            .putLong(PREFIJO_SENAL + paquete, System.currentTimeMillis())
            .apply()
    }

    /**
     * Las apps que escuchamos y de las que hace dos dias que no llega nada.
     *
     * Es una sospecha, no un diagnostico: Android no deja consultar los ajustes
     * de notificaciones de otra app. Tambien da positivo con alguien que
     * simplemente no recibio correos, y por eso el aviso que lo muestra es
     * ambar y dice que se puede ignorar.
     */
    fun appsSinSenal(context: Context, ahora: Long = System.currentTimeMillis()): Set<String> {
        val p = prefs(context)
        val desde = p.getLong(CLAVE_ESCUCHA_DESDE, 0L)
        return appsEscuchadas(context)
            .filter { haySilencio(desde, p.getLong(PREFIJO_SENAL + it, 0L), ahora) }
            .toSet()
    }


    fun appsEscuchadas(context: Context): Set<String> {
        val guardadas = prefs(context).getStringSet(CLAVE_APPS, null)
        // Por defecto escuchamos todas las apps de correo que haya instaladas.
        return guardadas ?: AppsDeCorreo.instaladas(context).map { it.paquete }.toSet()
    }

    fun guardarAppsEscuchadas(context: Context, paquetes: Set<String>) {
        prefs(context).edit()
            .putStringSet(CLAVE_APPS, paquetes)
            .apply()
    }
}
