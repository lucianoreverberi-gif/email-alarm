package com.reverstabilizer.emailalarm

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Lo que mas importa arreglar primero, en orden. */
enum class EstadoGeneral { BLOQUEADA, VENCIDA, SIN_REGLAS, SIN_SUSCRIPCION, LIMITADA, LISTA }

/**
 * El orden define el recorrido de alguien nuevo: permisos, primera regla,
 * probar la alarma, y recien ahi suscribirse. Se pide la tarjeta despues de
 * que la persona vio que funciona, no antes.
 */
internal fun estadoGeneral(
    faltaEsencial: Boolean,
    suscripcion: Suscripcion.Estado,
    hayReglasActivas: Boolean,
    faltaOpcional: Boolean
): EstadoGeneral = when {
    faltaEsencial -> EstadoGeneral.BLOQUEADA
    suscripcion == Suscripcion.Estado.VENCIDA -> EstadoGeneral.VENCIDA
    !hayReglasActivas -> EstadoGeneral.SIN_REGLAS
    suscripcion == Suscripcion.Estado.NUNCA -> EstadoGeneral.SIN_SUSCRIPCION
    faltaOpcional -> EstadoGeneral.LIMITADA
    else -> EstadoGeneral.LISTA
}

@Composable
fun PantallaPrincipal(
    permisos: List<EstadoDePermiso>,
    appsInstaladas: List<AppCorreo>,
    appsEscuchadas: Set<String>,
    reglas: List<Regla>,
    detecciones: List<Deteccion>,
    suscripcion: Suscripcion.Estado,
    onResolverPermiso: (EstadoDePermiso) -> Unit,
    onCambiarApp: (String, Boolean) -> Unit,
    onGuardarRegla: (Regla) -> Unit,
    onCambiarActiva: (Regla, Boolean) -> Unit,
    onBorrarRegla: (Regla) -> Unit,
    onProbarAhora: () -> Unit,
    onProbarDespues: () -> Unit,
    onSuscribirse: () -> Unit,
    onBorrarHistorial: () -> Unit,
    onAbrirAjustes: () -> Unit,
    modifier: Modifier = Modifier
) {
    var reglaEnEdicion by remember { mutableStateOf<Regla?>(null) }
    var mostrandoDialogo by remember { mutableStateOf(false) }
    var permisosAbiertos by rememberSaveable { mutableStateOf(false) }

    val nuevaRegla = {
        reglaEnEdicion = null
        mostrandoDialogo = true
    }

    val pendientes = permisos.filter { !it.concedido }
    val estado = estadoGeneral(
        faltaEsencial = pendientes.any { it.imprescindible },
        suscripcion = suscripcion,
        hayReglasActivas = reglas.any { it.activa },
        faltaOpcional = pendientes.any { !it.imprescindible }
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // a) Titulo
        item { Encabezado(onAbrirAjustes) }

        // b) Estado, con la accion que lo resuelve en la misma tarjeta
        item {
            TarjetaDeEstado(
                estado = estado,
                faltantes = pendientes,
                onResolverPermiso = onResolverPermiso,
                onSuscribirse = onSuscribirse,
                onCrearRegla = nuevaRegla,
                onProbarAhora = onProbarAhora,
                onProbarDespues = onProbarDespues
            )
        }

        // c) Reglas: el producto
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Titulo(stringResource(R.string.section_rules), Modifier.weight(1f))
                TextButton(onClick = nuevaRegla) { Text("+ " + stringResource(R.string.action_new_rule)) }
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

        // d) Historial: la prueba de que la app esta viva
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Titulo(stringResource(R.string.section_history))
                    Ayuda(stringResource(R.string.history_hint))
                }
                if (detecciones.isNotEmpty()) {
                    TextButton(onClick = onBorrarHistorial) { Text(stringResource(R.string.history_clear)) }
                }
            }
        }
        if (detecciones.isEmpty()) {
            item { Tarjeta { Ayuda(stringResource(R.string.history_empty)) } }
        } else {
            item {
                Tarjeta {
                    detecciones.forEachIndexed { i, d ->
                        if (i > 0) Separador()
                        FilaDeDeteccion(d)
                    }
                }
            }
        }

        // e) Permisos: una linea; suelto, solo lo pendiente
        item {
            val concedidos = permisos.count { it.concedido }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Titulo(
                    (if (pendientes.isEmpty()) "✓ " else "") +
                        stringResource(R.string.permissions_summary, concedidos, permisos.size),
                    Modifier.weight(1f)
                )
                TextButton(onClick = { permisosAbiertos = !permisosAbiertos }) {
                    Text(
                        stringResource(
                            if (permisosAbiertos) R.string.permissions_hide else R.string.permissions_show
                        )
                    )
                }
            }
        }
        val visibles = if (permisosAbiertos) permisos else pendientes
        items(visibles, key = { it.clave }) { permiso ->
            FilaDePermiso(permiso) { onResolverPermiso(permiso) }
        }

        // f) Apps que escucho
        item { Titulo(stringResource(R.string.section_apps), Modifier.padding(top = 12.dp)) }
        if (appsInstaladas.isEmpty()) {
            item { Ayuda(stringResource(R.string.apps_none)) }
        }
        items(appsInstaladas, key = { it.paquete }) { app ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(
                    checked = app.paquete in appsEscuchadas,
                    onCheckedChange = { onCambiarApp(app.paquete, it) }
                )
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(app.nombre, fontWeight = FontWeight.Medium)
                    if (!app.exponeDireccion) Ayuda(stringResource(R.string.app_no_address))
                }
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
private fun Encabezado(onAbrirAjustes: () -> Unit) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Ayuda(stringResource(R.string.tagline))
        }
        TextButton(onClick = onAbrirAjustes) { Text(stringResource(R.string.action_settings)) }
    }
}

/**
 * Lo primero que se ve: si la alarma puede sonar, y el boton que arregla lo
 * que falte. El problema y su solucion van juntos, no a dos pantallas.
 */
@Composable
private fun TarjetaDeEstado(
    estado: EstadoGeneral,
    faltantes: List<EstadoDePermiso>,
    onResolverPermiso: (EstadoDePermiso) -> Unit,
    onSuscribirse: () -> Unit,
    onCrearRegla: () -> Unit,
    onProbarAhora: () -> Unit,
    onProbarDespues: () -> Unit
) {
    val error = MaterialTheme.colorScheme.error
    val verde = MaterialTheme.colorScheme.primary
    val (color, titulo, detalle) = when (estado) {
        EstadoGeneral.BLOQUEADA -> Triple(error, R.string.status_blocked_title, R.string.status_blocked_body)
        EstadoGeneral.VENCIDA -> Triple(error, R.string.status_expired_title, R.string.status_expired_body)
        EstadoGeneral.SIN_REGLAS -> Triple(error, R.string.status_no_rules_title, R.string.status_no_rules_body)
        EstadoGeneral.SIN_SUSCRIPCION -> Triple(verde, R.string.status_no_sub_title, R.string.status_no_sub_body)
        EstadoGeneral.LIMITADA -> Triple(verde, R.string.status_limited_title, R.string.status_limited_body)
        EstadoGeneral.LISTA -> Triple(verde, R.string.status_ready_title, R.string.status_ready_body)
    }

    // El permiso a resolver: primero los imprescindibles.
    val aResolver = faltantes.firstOrNull { it.imprescindible } ?: faltantes.firstOrNull()

    Tarjeta(color = color.copy(alpha = 0.09f)) {
        Text(stringResource(titulo), fontWeight = FontWeight.Bold, color = color, fontSize = 19.sp)
        Text(stringResource(detalle), color = MaterialTheme.colorScheme.onSurface)
        if (aResolver != null && (estado == EstadoGeneral.BLOQUEADA || estado == EstadoGeneral.LIMITADA)) {
            Ayuda(stringResource(aResolver.titulo) + " — " + stringResource(aResolver.explicacion))
        }

        val accion: Pair<Int, () -> Unit>? = when (estado) {
            EstadoGeneral.BLOQUEADA, EstadoGeneral.LIMITADA ->
                aResolver?.let { R.string.action_enable_permission to { onResolverPermiso(it) } }
            EstadoGeneral.VENCIDA -> R.string.sub_renew to onSuscribirse
            EstadoGeneral.SIN_REGLAS -> R.string.action_create_first_rule to onCrearRegla
            EstadoGeneral.SIN_SUSCRIPCION -> R.string.action_start_free_month to onSuscribirse
            EstadoGeneral.LISTA -> null
        }

        Column(
            modifier = Modifier.padding(top = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (accion != null) {
                Button(
                    onClick = accion.second,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = color)
                ) { Text(stringResource(accion.first), fontWeight = FontWeight.Bold) }
            }
            // Probar siempre esta a mano: sin probarla, nadie confia en una alarma.
            if (accion == null) {
                Button(
                    onClick = onProbarAhora,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) { Text(stringResource(R.string.action_test_alarm), fontWeight = FontWeight.Bold) }
            } else {
                OutlinedButton(
                    onClick = onProbarAhora,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) { Text(stringResource(R.string.action_test_alarm)) }
            }
            TextButton(onClick = onProbarDespues, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_test_alarm_later))
            }
            Ayuda(stringResource(R.string.test_alarm_later_hint))
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
    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = regla.nombre,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = regla.activa, onCheckedChange = onCambiarActiva)
        }

        // Frases completas por caso: armarlas concatenando se rompe al traducir.
        val descripcion = when {
            regla.remitente.isNotBlank() && regla.palabraClave.isNotBlank() ->
                stringResource(R.string.rule_when_both, regla.remitente, regla.palabraClave)
            regla.remitente.isNotBlank() -> stringResource(R.string.rule_when_sender, regla.remitente)
            else -> stringResource(R.string.rule_when_keyword, regla.palabraClave)
        }
        Text(descripcion, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (regla.sonido != null) {
            Ayuda(stringResource(R.string.rule_sound, nombreDelSonido(regla.sonido)))
        }

        if (avisoDeDireccion) {
            Text(
                text = stringResource(R.string.rule_address_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Row {
            TextButton(onClick = onEditar) { Text(stringResource(R.string.action_edit)) }
            TextButton(onClick = onBorrar) {
                Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** "14:32 · Gmail", el remitente, el asunto, y que decidio la app. */
@Composable
private fun FilaDeDeteccion(d: Deteccion) {
    val ahora = System.currentTimeMillis()
    val cuando = DateUtils.formatSameDayTime(
        d.hora, ahora, java.text.DateFormat.SHORT, java.text.DateFormat.SHORT
    ).toString()

    val resultado = runCatching { Resultado.valueOf(d.resultado) }.getOrDefault(Resultado.SIN_COINCIDENCIA)
    val (texto, color) = when (resultado) {
        Resultado.SONO ->
            stringResource(R.string.history_rang, d.regla.orEmpty()) to MaterialTheme.colorScheme.primary
        Resultado.SIN_COINCIDENCIA ->
            stringResource(R.string.history_no_match) to MaterialTheme.colorScheme.onSurfaceVariant
        Resultado.SIN_SUSCRIPCION ->
            stringResource(R.string.history_no_sub, d.regla.orEmpty()) to MaterialTheme.colorScheme.error
        Resultado.AVISO_CUENTA ->
            stringResource(R.string.history_account) to MaterialTheme.colorScheme.error
    }

    Column(Modifier.padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            "$cuando · ${d.app}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(d.remitente, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            d.asunto,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(texto, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun FilaDePermiso(permiso: EstadoDePermiso, onResolver: () -> Unit) {
    val color = when {
        permiso.concedido -> MaterialTheme.colorScheme.primary
        permiso.imprescindible -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Tarjeta {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(color.copy(alpha = if (permiso.concedido) 1f else 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (permiso.concedido) "✓" else "!",
                    color = if (permiso.concedido) Color.White else color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(stringResource(permiso.titulo), fontWeight = FontWeight.Medium)
                if (!permiso.concedido) Ayuda(stringResource(permiso.explicacion))
            }
            if (!permiso.concedido && permiso.intent != null) {
                TextButton(onClick = onResolver) { Text(stringResource(R.string.action_enable)) }
            }
        }
    }
}

// --- Piezas compartidas con el dialogo y los ajustes ---

/** Tarjeta con aire adentro y sin borde: el contenido separa, no las lineas. */
@Composable
internal fun Tarjeta(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = contenido
        )
    }
}

@Composable
internal fun Titulo(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
    )
}

/** Texto de ayuda: siempre gris. El rojo queda solo para errores. */
@Composable
internal fun Ayuda(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
private fun Separador() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .size(height = 1.dp, width = 0.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

/**
 * Una regla que busca una direccion puede fallar en apps que solo publican el
 * nombre (si el nombre no se parece a la parte antes de la @). Se avisa antes
 * de que la persona se pierda un correo creyendo que estaba cubierta.
 */
private fun necesitaAvisoDeDireccion(
    regla: Regla,
    instaladas: List<AppCorreo>,
    escuchadas: Set<String>
): Boolean {
    if (!regla.remitente.contains("@")) return false
    return instaladas.any { it.paquete in escuchadas && !it.exponeDireccion }
}
