package com.reverstabilizer.emailalarm

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Crear o editar una regla.
 *
 * Cada campo lleva su etiqueta arriba y una ayuda abajo en gris que explica
 * que escribir. La idea: nadie tiene que adivinar como funciona la app.
 */
@Composable
fun DialogoDeRegla(
    reglaInicial: Regla?,
    onCancelar: () -> Unit,
    onConfirmar: (Regla) -> Unit
) {
    var nombre by remember { mutableStateOf(reglaInicial?.nombre ?: "") }
    var remitente by remember { mutableStateOf(reglaInicial?.remitente ?: "") }
    var palabraClave by remember { mutableStateOf(reglaInicial?.palabraClave ?: "") }
    var sonido by remember { mutableStateOf(reglaInicial?.sonido) }

    val hayCondicion = remitente.isNotBlank() || palabraClave.isNotBlank()
    val tituloSonido = stringResource(R.string.field_sound)

    val selectorDeSonido = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val elegido: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            resultado.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            resultado.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
        // Elegir "predeterminado" es lo mismo que no elegir: se guarda null.
        val predeterminado = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        sonido = if (elegido == null || elegido == predeterminado) null else elegido.toString()
    }

    AlertDialog(
        onDismissRequest = onCancelar,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                stringResource(if (reglaInicial == null) R.string.dialog_new_rule else R.string.dialog_edit_rule),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            // Con toda la ayuda el dialogo es largo: en pantallas chicas tiene
            // que desplazarse para no esconder el boton Guardar.
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                CampoConAyuda(
                    etiqueta = stringResource(R.string.field_name),
                    opcional = false,
                    valor = nombre,
                    onCambio = { nombre = it },
                    ejemplo = stringResource(R.string.field_name_hint),
                    ayuda = stringResource(R.string.field_name_help)
                )
                CampoConAyuda(
                    etiqueta = stringResource(R.string.field_sender),
                    opcional = true,
                    valor = remitente,
                    onCambio = { remitente = it },
                    ejemplo = stringResource(R.string.field_sender_hint),
                    ayuda = stringResource(R.string.field_sender_help)
                )
                CampoConAyuda(
                    etiqueta = stringResource(R.string.field_keyword),
                    opcional = true,
                    valor = palabraClave,
                    onCambio = { palabraClave = it },
                    ejemplo = stringResource(R.string.field_keyword_hint),
                    ayuda = stringResource(R.string.field_keyword_help)
                )

                Ayuda(stringResource(R.string.dialog_combination))

                // Las reglas por palabra clave son para remitentes desconocidos
                // (un tramite, un organismo), que son justo los que mas caen en spam.
                if (palabraClave.isNotBlank()) ConsejoSpam(palabraClave.trim())

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(tituloSonido, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            nombreDelSonido(sonido),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            val predeterminado = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                            selectorDeSonido.launch(
                                Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                                    .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                    .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                    // Nunca "Silencio": una alarma muda no sirve.
                                    .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                    .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, predeterminado)
                                    .putExtra(
                                        RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                        sonido?.let(Uri::parse) ?: predeterminado
                                    )
                                    .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, tituloSonido)
                            )
                        }) { Text(stringResource(R.string.sound_change)) }
                    }
                }
            }
        },
        confirmButton = {
            // Un boton deshabilitado sin explicacion es un callejon sin salida:
            // abajo se dice que falta. Los dos botones van en esta misma fila
            // para que el motivo quede debajo de ambos sin desarmarla.
            Column(horizontalAlignment = Alignment.End) {
                Row {
                TextButton(onClick = onCancelar) { Text(stringResource(R.string.action_cancel)) }
                TextButton(
                    enabled = hayCondicion,
                    onClick = {
                        onConfirmar(
                            Regla(
                                id = reglaInicial?.id ?: 0,
                                nombre = nombre.trim()
                                    .ifBlank { remitente.trim().ifBlank { palabraClave.trim() } },
                                remitente = remitente.trim(),
                                palabraClave = palabraClave.trim(),
                                activa = reglaInicial?.activa ?: true,
                                sonido = sonido
                            )
                        )
                    }
                ) { Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold) }
                }
                if (!hayCondicion) Ayuda(stringResource(R.string.dialog_missing))
            }
        }
    )
}

@Composable
private fun CampoConAyuda(
    etiqueta: String,
    opcional: Boolean,
    valor: String,
    onCambio: (String) -> Unit,
    ejemplo: String,
    ayuda: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Text(etiqueta, fontWeight = FontWeight.SemiBold)
            if (opcional) {
                Text(
                    " · " + stringResource(R.string.optional),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        OutlinedTextField(
            value = valor,
            onValueChange = onCambio,
            placeholder = { Text(ejemplo) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Ayuda(ayuda)
    }
}

/**
 * Si el correo cae en spam, la app de correo no notifica y la alarma no se
 * entera: es un falso negativo silencioso. Un filtro de Gmail con
 * "Nunca enviar a spam" lo evita aunque no se sepa el remitente.
 */
@Composable
private fun ConsejoSpam(palabra: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.tip_spam_title),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = stringResource(R.string.tip_spam_body, palabra),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/** El nombre del tono para mostrar, o "tono del sistema" si no se eligio ninguno. */
@Composable
internal fun nombreDelSonido(sonido: String?): String {
    val contexto = LocalContext.current
    val predeterminado = stringResource(R.string.sound_default)
    val elegido = stringResource(R.string.sound_custom)
    return remember(sonido) {
        if (sonido == null) {
            predeterminado
        } else {
            // Leer el titulo de un tono de usuario puede requerir permisos que
            // la app no tiene: si falla, un nombre generico alcanza.
            runCatching { RingtoneManager.getRingtone(contexto, Uri.parse(sonido))?.getTitle(contexto) }
                .getOrNull() ?: elegido
        }
    }
}
