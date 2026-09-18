package com.reverstabilizer.emailalarm

/**
 * Recuerda los correos vistos hace poco, para procesar cada uno una sola vez.
 *
 * Gmail a veces publica la misma notificacion dos veces en el mismo instante
 * (y la vuelve a publicar cuando la actualiza). Sin esto, un correo sonaba
 * dos veces y quedaba dos veces en la actividad.
 *
 * Si llegan dos correos iguales de verdad dentro de la ventana, suena una sola
 * vez: no se pierde nada, porque la alarma ya esta sonando por el primero.
 */
class Duplicados(private val ventanaMs: Long = 2 * 60 * 1000L) {

    private val vistos = LinkedHashMap<String, Long>()

    /** true la primera vez que se ve [clave] dentro de la ventana. */
    @Synchronized
    fun esNuevo(clave: String, ahora: Long = System.currentTimeMillis()): Boolean {
        vistos.entries.removeAll { ahora - it.value > ventanaMs }
        if (clave in vistos) return false
        vistos[clave] = ahora
        return true
    }
}
