package com.reverstabilizer.emailalarm

import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * El idioma de la app, elegido desde Ajustes.
 *
 * Desde Android 13 el sistema ya ofrece "Idioma de la app" en sus ajustes
 * (lo habilita el locales_config.xml del manifest), pero casi nadie lo busca
 * ahi: la gente abre la app. Este selector hace lo mismo desde adentro y, en
 * Android 13 o superior, deja el sistema sincronizado, para que las dos
 * pantallas digan siempre lo mismo.
 *
 * Sin dependencias nuevas: en Android 13+ manda el LocaleManager del sistema;
 * mas abajo, [envolver] aplica el idioma al contexto de cada Activity.
 */
object Idioma {

    /** Los que la app tiene traducidos. Tiene que coincidir con locales_config.xml. */
    val DISPONIBLES = listOf("es", "en")

    private const val ARCHIVO = "ajustes"
    private const val CLAVE = "idioma"

    /** El codigo elegido ("es", "en") o null si manda el idioma del telefono. */
    fun elegido(context: Context): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val delSistema = context.getSystemService(LocaleManager::class.java)
                ?.applicationLocales
            if (delSistema != null && !delSistema.isEmpty) return delSistema[0]?.language
            return null
        }
        return prefs(context).getString(CLAVE, null)
    }

    /**
     * Guarda el idioma y lo aplica. null vuelve al del telefono.
     *
     * En Android 13+ el sistema recrea la Activity solo. Abajo hay que
     * recrearla a mano: por eso devuelve si hace falta.
     */
    fun elegir(context: Context, codigo: String?): Boolean {
        prefs(context).edit().apply {
            if (codigo == null) remove(CLAVE) else putString(CLAVE, codigo)
        }.apply()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)?.applicationLocales =
                if (codigo == null) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(codigo)
            return false
        }
        return true
    }

    /**
     * Devuelve el contexto con el idioma elegido aplicado. Se llama desde
     * attachBaseContext de cada Activity, antes de que se lea cualquier texto.
     */
    fun envolver(base: Context): Context {
        // En Android 13+ el sistema ya entrega el contexto en el idioma correcto.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base

        val codigo = prefs(base).getString(CLAVE, null) ?: return base
        val config = android.content.res.Configuration(base.resources.configuration)
        val locale = Locale.forLanguageTag(codigo)
        Locale.setDefault(locale)
        config.setLocales(LocaleList(locale))
        return ContextWrapper(base.createConfigurationContext(config))
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
}
