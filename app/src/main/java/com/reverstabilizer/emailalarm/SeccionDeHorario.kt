package com.reverstabilizer.emailalarm

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Dias y horas en que suena la alarma, dentro del editor.
 *
 * Un tester puso una alarma para los registros de clientes nuevos y a las 4 de
 * la madrugada lo desperto uno. El horario ya existia y no le servia de nada:
 * estaba al final del editor, plegado, resumido en gris como "Todos los dias,
 * a cualquier hora". Decia la verdad y no comunicaba nada, porque describia un
 * ajuste en vez de su consecuencia: nadie lee "a cualquier hora" y entiende
 * "te va a despertar a las 4". Ahora es una decision a la vista, igual que el
 * modo de coincidencia, y el texto nombra la madrugada.
 *
 * El valor por defecto sigue siendo "a cualquier hora" a proposito: que una
 * alarma no suene por un correo urgente de noche es peor falla que despertar a
 * alguien. Lo que cambia es que ahora se elige con los ojos abiertos.
 */
@Composable
fun SeccionDeHorario(horario: Horario, onCambio: (Horario) -> Unit) {
    // Estado propio, no derivado de horario.esSiempre: si alguien elige "solo
    // ciertos dias y horas" y despues marca todos los dias con todo el dia
    // prendido, el horario vuelve a ser "siempre", pero la seccion no tiene
    // que cerrarse sola abajo de sus manos.
    var restringido by rememberSaveable { mutableStateOf(!horario.esSiempre) }
    var eligiendo by remember { mutableStateOf<Extremo?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.schedule_title), fontWeight = FontWeight.SemiBold)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OpcionDeModo(
                texto = stringResource(R.string.schedule_mode_always),
                seleccionado = !restringido,
                habilitado = true,
                modifier = Modifier.weight(1f)
            ) {
                restringido = false
                onCambio(Horario())
            }
            OpcionDeModo(
                texto = stringResource(R.string.schedule_mode_limited),
                seleccionado = restringido,
                habilitado = true,
                modifier = Modifier.weight(1f)
            ) {
                restringido = true
                // Si ya tenia horas puestas, se respetan; recien si no tiene
                // ninguna se proponen las sugeridas.
                onCambio(
                    if (horario.todoElDia) {
                        horario.copy(desde = Horario.DESDE_SUGERIDO, hasta = Horario.HASTA_SUGERIDO)
                    } else {
                        horario
                    }
                )
            }
        }

        // La advertencia va en la opcion elegida, no escondida adentro de la otra.
        if (!restringido) Ayuda(stringResource(R.string.schedule_help_always))

        AnimatedVisibility(restringido) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    diasDeLaSemana().forEach { dia ->
                        BotonDeDia(dia, horario.suenaEl(dia)) { onCambio(horario.conDia(dia, it)) }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.schedule_all_day), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Switch(
                        checked = horario.todoElDia,
                        onCheckedChange = { todoElDia ->
                            onCambio(
                                if (todoElDia) horario.copy(desde = null, hasta = null)
                                else horario.copy(desde = Horario.DESDE_SUGERIDO, hasta = Horario.HASTA_SUGERIDO)
                            )
                        }
                    )
                }

                if (!horario.todoElDia) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CampoDeHora(stringResource(R.string.schedule_from), horario.desde!!, Modifier.weight(1f)) {
                            eligiendo = Extremo.DESDE
                        }
                        CampoDeHora(stringResource(R.string.schedule_until), horario.hasta!!, Modifier.weight(1f)) {
                            eligiendo = Extremo.HASTA
                        }
                    }
                }

                Ayuda(stringResource(R.string.schedule_help))
            }
        }
    }

    eligiendo?.let { extremo ->
        val actual = (if (extremo == Extremo.DESDE) horario.desde else horario.hasta) ?: 0
        SelectorDeHora(
            minutos = actual,
            onCancelar = { eligiendo = null },
            onElegir = { minutos ->
                onCambio(if (extremo == Extremo.DESDE) horario.copy(desde = minutos) else horario.copy(hasta = minutos))
                eligiendo = null
            }
        )
    }
}

private enum class Extremo { DESDE, HASTA }

/** Verde con tilde: suena ese dia. Gris sin tilde: no suena. */
@Composable
private fun BotonDeDia(dia: DayOfWeek, suena: Boolean, onCambio: (Boolean) -> Unit) {
    val colores = MaterialTheme.colorScheme
    FilterChip(
        selected = suena,
        onClick = { onCambio(!suena) },
        label = { Text(nombreCorto(dia), fontWeight = if (suena) FontWeight.SemiBold else FontWeight.Normal) },
        leadingIcon = if (suena) {
            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else null,
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colores.surfaceVariant,
            labelColor = colores.onSurfaceVariant,
            selectedContainerColor = colores.primaryContainer,
            selectedLabelColor = colores.primary,
            selectedLeadingIconColor = colores.primary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = suena,
            borderColor = colores.outlineVariant,
            selectedBorderColor = colores.primary,
            borderWidth = 1.dp,
            selectedBorderWidth = 1.5.dp
        )
    )
}

/** Una hora que se toca para cambiarla, con su etiqueta arriba. */
@Composable
private fun CampoDeHora(etiqueta: String, minutos: Int, modifier: Modifier = Modifier, onTocar: () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(12.dp))
                .clickable(onClick = onTocar)
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Icon(
                Icons.Outlined.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                formatoDeHora(minutos),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorDeHora(minutos: Int, onCancelar: () -> Unit, onElegir: (Int) -> Unit) {
    val estado = rememberTimePickerState(
        initialHour = minutos / 60,
        initialMinute = minutos % 60,
        is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    )
    AlertDialog(
        onDismissRequest = onCancelar,
        shape = RoundedCornerShape(24.dp),
        text = { TimePicker(state = estado) },
        confirmButton = {
            TextButton(onClick = { onElegir(estado.hour * 60 + estado.minute) }) {
                Text(stringResource(android.R.string.ok), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.action_cancel)) } }
    )
}

/** "Todos los dias, a cualquier hora", "Lunes a viernes, 09:00–18:00", "Lun, Mié, a cualquier hora". */
@Composable
fun resumenDelHorario(horario: Horario): String {
    if (horario.esSiempre) return stringResource(R.string.schedule_always)
    val dias = when (horario.dias) {
        Horario.TODOS_LOS_DIAS -> stringResource(R.string.schedule_every_day)
        Horario.LUNES_A_VIERNES -> stringResource(R.string.schedule_weekdays)
        Horario.FIN_DE_SEMANA -> stringResource(R.string.schedule_weekends)
        else -> diasDeLaSemana().filter(horario::suenaEl).joinToString(", ") { nombreCorto(it) }
    }
    val horas = if (horario.todoElDia) {
        stringResource(R.string.schedule_any_time)
    } else {
        "${formatoDeHora(horario.desde!!)}–${formatoDeHora(horario.hasta!!)}"
    }
    return stringResource(R.string.schedule_summary, dias, horas)
}

/** En el orden de la zona: domingo primero en Estados Unidos, lunes en casi todo el resto. */
private fun diasDeLaSemana(): List<DayOfWeek> {
    val primero = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    return (0L until 7L).map { primero.plus(it) }
}

/** "Lun", "Mié", "Mon": corto y con mayuscula, sin el punto de algunas abreviaturas. */
private fun nombreCorto(dia: DayOfWeek): String =
    dia.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        .trimEnd('.')
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }

/** Respeta si el telefono usa 24 horas o AM/PM. */
@Composable
private fun formatoDeHora(minutos: Int): String {
    val es24 = DateFormat.is24HourFormat(LocalContext.current)
    val patron = if (es24) "HH:mm" else "h:mm a"
    return LocalTime.of(minutos / 60, minutos % 60).format(DateTimeFormatter.ofPattern(patron, Locale.getDefault()))
}
