package com.reverstabilizer.emailalarm

import java.time.DayOfWeek
import java.time.LocalDateTime

/**
 * Cuando puede sonar una alarma: que dias y entre que horas.
 *
 * Existe porque no todos los correos valen despertar a la familia: un pedido
 * de un cliente a las 6 de la manana puede esperar, una respuesta de
 * migraciones capaz que no. Por eso va por alarma y no para toda la app.
 *
 * @param dias un bit por dia, lunes = bit 0 ... domingo = bit 6.
 * @param desde minutos desde la medianoche; con [hasta], null = todo el dia.
 */
data class Horario(val dias: Int = TODOS_LOS_DIAS, val desde: Int? = null, val hasta: Int? = null) {

    val todoElDia: Boolean get() = desde == null || hasta == null

    /** El de siempre: todos los dias, a cualquier hora. */
    val esSiempre: Boolean get() = dias == TODOS_LOS_DIAS && todoElDia

    fun suenaEl(dia: DayOfWeek): Boolean = dias and bit(dia) != 0

    fun conDia(dia: DayOfWeek, suena: Boolean): Horario =
        copy(dias = if (suena) dias or bit(dia) else dias and bit(dia).inv())

    /**
     * Si un correo que llega en [momento] tiene que sonar.
     *
     * Un horario que cruza la medianoche (22:00 a 07:00) cuenta el dia en que
     * llega el correo, igual que la app de iPhone: "lunes a viernes, 22 a 7"
     * no suena el sabado a las 3.
     */
    fun permite(momento: LocalDateTime): Boolean {
        if (!suenaEl(momento.dayOfWeek)) return false
        if (desde == null || hasta == null) return true
        val minuto = momento.hour * 60 + momento.minute
        return if (desde < hasta) minuto in desde until hasta else minuto >= desde || minuto < hasta
    }

    companion object {
        const val TODOS_LOS_DIAS = 0b111_1111
        val LUNES_A_VIERNES = DayOfWeek.entries.take(5).fold(0) { acc, d -> acc or bit(d) }
        val FIN_DE_SEMANA = TODOS_LOS_DIAS and LUNES_A_VIERNES.inv()

        /** El horario que se propone al apagar "Todo el dia". */
        const val DESDE_SUGERIDO = 9 * 60
        const val HASTA_SUGERIDO = 18 * 60

        fun bit(dia: DayOfWeek): Int = 1 shl (dia.value - 1)
    }
}
