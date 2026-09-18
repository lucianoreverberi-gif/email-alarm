package com.reverstabilizer.emailalarm

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Algo que hace que las alarmas no suenen. Es lo unico que va arriba de todo. */
enum class Alerta { PERMISO, VENCIDA, SIN_SUSCRIPCION }

/**
 * La pantalla abre con las alarmas, no con un diagnostico. La barra de arriba
 * aparece solo si una alarma que la persona cree activa no va a sonar: ese
 * falso negativo silencioso es justo lo que la app existe para evitar.
 *
 * Sin suscripcion y sin alarmas no hay alerta: alguien nuevo primero crea su
 * alarma y la prueba; recien despues se le pide la tarjeta.
 */
internal fun alertaCritica(
    faltaImprescindible: Boolean,
    suscripcion: Suscripcion.Estado,
    hayAlarmasActivas: Boolean
): Alerta? = when {
    faltaImprescindible -> Alerta.PERMISO
    suscripcion == Suscripcion.Estado.VENCIDA -> Alerta.VENCIDA
    suscripcion == Suscripcion.Estado.NUNCA && hayAlarmasActivas -> Alerta.SIN_SUSCRIPCION
    else -> null
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

    val nuevaAlarma = {
        reglaEnEdicion = null
        mostrandoDialogo = true
    }

    val pendientes = permisos.filter { !it.concedido }
    val alerta = alertaCritica(
        faltaImprescindible = pendientes.any { it.imprescindible },
        suscripcion = suscripcion,
        hayAlarmasActivas = reglas.any { it.activa }
    )

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            // Abajo queda lugar para que el boton flotante no tape lo ultimo.
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Titulo
            item { Encabezado(onAbrirAjustes) }

            // Excepcion: si una alarma no va a sonar, se dice antes que nada.
            if (alerta != null) {
                item {
                    BarraDeAlerta(
                        alerta = alerta,
                        permiso = pendientes.firstOrNull { it.imprescindible },
                        onResolverPermiso = onResolverPermiso,
                        onSuscribirse = onSuscribirse
                    )
                }
            }

            // 2. Mis alarmas: lo primero y lo mas grande
            item {
                Text(
                    text = stringResource(R.string.section_rules),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (reglas.isEmpty()) {
                item { PrimeraAlarma(onCrear = nuevaAlarma) }
            }
            items(reglas, key = { it.id }) { regla ->
                TarjetaDeAlarma(
                    regla = regla,
                    avisoDeDireccion = necesitaAvisoDeDireccion(regla, appsInstaladas, appsEscuchadas),
                    onCambiarActiva = { onCambiarActiva(regla, it) },
                    onEditar = {
                        reglaEnEdicion = regla
                        mostrandoDialogo = true
                    }
                )
            }

            // 3. Probar, discreto
            item { Prueba(onProbarAhora, onProbarDespues) }

            // 4. Actividad reciente
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

            // 5. Permisos: una linea; suelto, solo lo pendiente
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

            // 6. Apps que escucho
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

        // Como en el reloj: el boton para agregar siempre a mano, sin scrollear.
        // Sin alarmas no hace falta: la tarjeta de la primera ya es el boton.
        if (reglas.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = nuevaAlarma,
                shape = RoundedCornerShape(18.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            ) {
                Text("+  " + stringResource(R.string.action_new_rule), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }

    if (mostrandoDialogo) {
        val editada = reglaEnEdicion
        DialogoDeRegla(
            reglaInicial = editada,
            onCancelar = { mostrandoDialogo = false },
            onConfirmar = { regla ->
                onGuardarRegla(regla)
                mostrandoDialogo = false
            },
            onBorrar = editada?.let {
                {
                    onBorrarRegla(it)
                    mostrandoDialogo = false
                }
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

/** Barra roja: el problema y el boton que lo arregla, juntos. */
@Composable
private fun BarraDeAlerta(
    alerta: Alerta,
    permiso: EstadoDePermiso?,
    onResolverPermiso: (EstadoDePermiso) -> Unit,
    onSuscribirse: () -> Unit
) {
    val color = MaterialTheme.colorScheme.error
    val (texto, boton, accion) = when (alerta) {
        Alerta.PERMISO -> Triple(
            stringResource(R.string.alert_permission, permiso?.let { stringResource(it.titulo) }.orEmpty()),
            R.string.action_enable_permission,
            { permiso?.let(onResolverPermiso) ?: Unit }
        )
        Alerta.VENCIDA -> Triple(stringResource(R.string.alert_expired), R.string.sub_renew, onSuscribirse)
        Alerta.SIN_SUSCRIPCION ->
            Triple(stringResource(R.string.alert_no_sub), R.string.action_start_free_month, onSuscribirse)
    }

    Tarjeta(color = color.copy(alpha = 0.09f)) {
        Text(texto, fontWeight = FontWeight.SemiBold, color = color)
        Button(
            onClick = accion,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color)
        ) { Text(stringResource(boton), fontWeight = FontWeight.Bold) }
    }
}

/** Sin alarmas, una invitacion grande, no un mensaje de error. */
@Composable
private fun PrimeraAlarma(onCrear: () -> Unit) {
    Tarjeta(color = MaterialTheme.colorScheme.primaryContainer) {
        Text("⏰", fontSize = 40.sp)
        Text(
            stringResource(R.string.empty_alarms_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(stringResource(R.string.empty_alarms_body), color = MaterialTheme.colorScheme.onPrimaryContainer)
        Button(
            onClick = onCrear,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) { Text("+  " + stringResource(R.string.action_create_first_rule), fontWeight = FontWeight.Bold, fontSize = 17.sp) }
    }
}

/**
 * Como una alarma del reloj: el nombre grande, el switch a la derecha, y
 * tocarla la abre para editar. Apagada se ve atenuada.
 */
@Composable
private fun TarjetaDeAlarma(
    regla: Regla,
    avisoDeDireccion: Boolean,
    onCambiarActiva: (Boolean) -> Unit,
    onEditar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEditar)
                .padding(horizontal = 22.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (regla.activa) 1f else 0.45f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = regla.nombre,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
            }
            Switch(
                checked = regla.activa,
                onCheckedChange = onCambiarActiva,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

/** Probar a mano pero sin robar protagonismo a las alarmas. */
@Composable
private fun Prueba(onProbarAhora: () -> Unit, onProbarDespues: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onProbarAhora) { Text("🔔  " + stringResource(R.string.action_test_alarm)) }
            TextButton(onClick = onProbarDespues) { Text(stringResource(R.string.action_test_alarm_later)) }
        }
        Ayuda(stringResource(R.string.test_alarm_later_hint), Modifier.padding(horizontal = 12.dp))
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
 * Una alarma que busca una direccion puede fallar en apps que solo publican el
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
