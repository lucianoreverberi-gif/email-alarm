package com.reverstabilizer.emailalarm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PantallaPrincipal(
    permisos: List<EstadoDePermiso>,
    appsInstaladas: List<AppCorreo>,
    appsEscuchadas: Set<String>,
    reglas: List<Regla>,
    onResolverPermiso: (EstadoDePermiso) -> Unit,
    onCambiarApp: (String, Boolean) -> Unit,
    onGuardarRegla: (Regla) -> Unit,
    onCambiarActiva: (Regla, Boolean) -> Unit,
    onBorrarRegla: (Regla) -> Unit,
    onDetenerAlarma: () -> Unit,
    modifier: Modifier = Modifier
) {
    var reglaEnEdicion by remember { mutableStateOf<Regla?>(null) }
    var mostrandoDialogo by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Encabezado() }

        item { Resumen(permisos, reglas) }

        item { Titulo(stringResource(R.string.section_permissions)) }

        items(permisos, key = { it.clave }) { permiso ->
            FilaDePermiso(permiso) { onResolverPermiso(permiso) }
        }

        item { Titulo(stringResource(R.string.section_apps)) }

        if (appsInstaladas.isEmpty()) {
            item { Ayuda(stringResource(R.string.apps_none)) }
        }

        items(appsInstaladas, key = { it.paquete }) { app ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = app.paquete in appsEscuchadas,
                    onCheckedChange = { onCambiarApp(app.paquete, it) }
                )
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(app.nombre, fontWeight = FontWeight.Medium)
                    if (!app.exponeDireccion) {
                        Text(
                            text = stringResource(R.string.app_no_address),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { Titulo(stringResource(R.string.section_rules)) }

        item {
            Button(
                onClick = {
                    reglaEnEdicion = null
                    mostrandoDialogo = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(stringResource(R.string.action_new_rule), fontWeight = FontWeight.Bold)
            }
        }

        if (reglas.isEmpty()) {
            item {
                Ayuda(stringResource(R.string.rules_empty))
            }
        }

        items(reglas, key = { it.id }) { regla ->
            FilaDeRegla(
                regla = regla,
                avisoDeDireccion = necesitaAvisoDeDireccion(regla, appsInstaladas, appsEscuchadas),
                onCambiarActiva = { onCambiarActiva(regla, it) },
                onEditar = {
                    reglaEnEdicion = regla
                    mostrandoDialogo = true
                },
                onBorrar = { onBorrarRegla(regla) }
            )
        }

        item {
            TextButton(
                onClick = onDetenerAlarma,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_stop_alarm),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (mostrandoDialogo) {
        DialogoDeRegla(
            reglaInicial = reglaEnEdicion,
            onCancelar = { mostrandoDialogo = false },
            onConfirmar = { regla ->
                onGuardarRegla(regla)
                mostrandoDialogo = false
            }
        )
    }
}

@Composable
private fun Encabezado() {
    Column(modifier = Modifier.padding(bottom = 6.dp)) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Lo primero que se ve: si la app esta en condiciones de sonar o no.
 * Un permiso imprescindible faltante la deja muda, y eso no puede estar
 * escondido abajo en una lista.
 */
@Composable
private fun Resumen(permisos: List<EstadoDePermiso>, reglas: List<Regla>) {
    val faltaEsencial = permisos.any { it.imprescindible && !it.concedido }
    val faltaOpcional = permisos.any { !it.imprescindible && !it.concedido }
    val sinReglas = reglas.none { it.activa }

    val (color, titulo, detalle) = when {
        faltaEsencial -> Triple(
            MaterialTheme.colorScheme.error,
            R.string.status_blocked_title,
            R.string.status_blocked_body
        )
        sinReglas -> Triple(
            MaterialTheme.colorScheme.error,
            R.string.status_no_rules_title,
            R.string.status_no_rules_body
        )
        faltaOpcional -> Triple(
            MaterialTheme.colorScheme.primary,
            R.string.status_limited_title,
            R.string.status_limited_body
        )
        else -> Triple(
            MaterialTheme.colorScheme.primary,
            R.string.status_ready_title,
            R.string.status_ready_body
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(titulo),
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 17.sp
            )
            Text(
                text = stringResource(detalle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun FilaDePermiso(permiso: EstadoDePermiso, onResolver: () -> Unit) {
    val color = when {
        permiso.concedido -> MaterialTheme.colorScheme.primary
        permiso.imprescindible -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(color.copy(alpha = if (permiso.concedido) 1f else 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (permiso.concedido) "✓" else "!",
                    color = if (permiso.concedido) Color.White else color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(stringResource(permiso.titulo), fontWeight = FontWeight.Medium)
                if (!permiso.concedido) {
                    Text(
                        text = stringResource(permiso.explicacion),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!permiso.concedido && permiso.intent != null) {
                TextButton(onClick = onResolver) { Text(stringResource(R.string.action_enable)) }
            }
        }
    }
}

@Composable
private fun FilaDeRegla(
    regla: Regla,
    avisoDeDireccion: Boolean,
    onCambiarActiva: (Boolean) -> Unit,
    onEditar: () -> Unit,
    onBorrar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = regla.nombre,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = regla.activa, onCheckedChange = onCambiarActiva)
            }

            // Frases completas por caso: armarlas concatenando se rompe al traducir.
            val descripcion = when {
                regla.remitente.isNotBlank() && regla.palabraClave.isNotBlank() ->
                    stringResource(R.string.rule_when_both, regla.remitente, regla.palabraClave)
                regla.remitente.isNotBlank() ->
                    stringResource(R.string.rule_when_sender, regla.remitente)
                else ->
                    stringResource(R.string.rule_when_keyword, regla.palabraClave)
            }
            Text(
                text = descripcion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (avisoDeDireccion) {
                Text(
                    text = stringResource(R.string.rule_address_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Row(modifier = Modifier.padding(top = 4.dp)) {
                TextButton(onClick = onEditar) { Text(stringResource(R.string.action_edit)) }
                TextButton(onClick = onBorrar) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun DialogoDeRegla(
    reglaInicial: Regla?,
    onCancelar: () -> Unit,
    onConfirmar: (Regla) -> Unit
) {
    var nombre by remember { mutableStateOf(reglaInicial?.nombre ?: "") }
    var remitente by remember { mutableStateOf(reglaInicial?.remitente ?: "") }
    var palabraClave by remember { mutableStateOf(reglaInicial?.palabraClave ?: "") }

    val hayCondicion = remitente.isNotBlank() || palabraClave.isNotBlank()

    AlertDialog(
        onDismissRequest = onCancelar,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                stringResource(
                    if (reglaInicial == null) R.string.dialog_new_rule
                    else R.string.dialog_edit_rule
                )
            )
        },
        text = {
            // Con el consejo de spam el dialogo se alarga: en pantallas chicas
            // tiene que poder desplazarse para no esconder el boton Guardar.
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text(stringResource(R.string.field_name)) },
                    placeholder = { Text(stringResource(R.string.field_name_hint)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = remitente,
                    onValueChange = { remitente = it },
                    label = { Text(stringResource(R.string.field_sender)) },
                    placeholder = { Text(stringResource(R.string.field_sender_hint)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = palabraClave,
                    onValueChange = { palabraClave = it },
                    label = { Text(stringResource(R.string.field_keyword)) },
                    placeholder = { Text(stringResource(R.string.field_keyword_hint)) },
                    singleLine = true
                )
                Text(
                    text = stringResource(
                        if (remitente.isNotBlank() && palabraClave.isNotBlank()) {
                            R.string.dialog_hint_both
                        } else {
                            R.string.dialog_hint_one
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hayCondicion) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                // Las reglas por palabra clave son para remitentes desconocidos
                // (un tramite, un organismo), que son justo los que mas caen en spam.
                if (palabraClave.isNotBlank()) {
                    ConsejoSpam(palabraClave.trim())
                }
            }
        },
        confirmButton = {
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
                            activa = reglaInicial?.activa ?: true
                        )
                    )
                }
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.action_cancel)) }
        }
    )
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
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
            .padding(12.dp),
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

@Composable
private fun Titulo(texto: String) {
    Text(
        text = texto.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 14.dp, bottom = 2.dp)
    )
}

@Composable
private fun Ayuda(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Una regla que busca una direccion solo puede funcionar en apps que la publiquen.
 * Si el usuario escucha alguna que no lo hace, conviene avisarle antes de que
 * se pierda un correo creyendo que estaba cubierto.
 */
private fun necesitaAvisoDeDireccion(
    regla: Regla,
    instaladas: List<AppCorreo>,
    escuchadas: Set<String>
): Boolean {
    if (!regla.remitente.contains("@")) return false
    return instaladas.any { it.paquete in escuchadas && !it.exponeDireccion }
}
