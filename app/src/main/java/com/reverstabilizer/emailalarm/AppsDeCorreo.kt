package com.reverstabilizer.emailalarm

import android.content.Context
import android.content.pm.PackageManager

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
}

/** Que apps estamos escuchando. Es una preferencia simple, no va a la base. */
object Ajustes {

    private const val ARCHIVO = "ajustes"
    private const val CLAVE_APPS = "apps_escuchadas"


    fun appsEscuchadas(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
        val guardadas = prefs.getStringSet(CLAVE_APPS, null)
        // Por defecto escuchamos todas las apps de correo que haya instaladas.
        return guardadas ?: AppsDeCorreo.instaladas(context).map { it.paquete }.toSet()
    }

    fun guardarAppsEscuchadas(context: Context, paquetes: Set<String>) {
        context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(CLAVE_APPS, paquetes)
            .apply()
    }
}
