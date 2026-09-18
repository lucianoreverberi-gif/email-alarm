package com.reverstabilizer.emailalarm

import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.reverstabilizer.emailalarm.ui.theme.Aviso

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

/** Cuantos correos se ven sin expandir. Con mas, la lista tapa lo demas. */
private const val CORREOS_VISIBLES = 4

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
    var historialAbierto by rememberSaveable { mutableStateOf(false) }

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

    // Historiales viejos pueden tener avisos de cuenta: no son correos.
    val correos = detecciones.filter { it.resultado != Resultado.AVISO_CUENTA.name }
    val correosVisibles = if (historialAbierto) correos else correos.take(CORREOS_VISIBLES)

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // Abajo queda lugar para que el boton flotante no tape lo ultimo.
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Encabezado(
                    appsEscuchadas = appsInstaladas.filter { it.paquete in appsEscuchadas },
                    escuchando = permisos.firstOrNull { it.clave == "listener" }?.concedido ?: true,
                    onAbrirAjustes = onAbrirAjustes
                )
            }

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

            // Mis alarmas: lo primero y lo mas grande
            item { Seccion(stringResource(R.string.section_rules)) }
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

            // Probar: su propia seccion, sin tarjeta. Las tarjetas blancas son
            // solo alarmas; esto es una accion, y tiene que verse distinto.
            item {
                Seccion(
                    titulo = stringResource(R.string.test_title),
                    subtitulo = stringResource(R.string.test_body)
                )
            }
            item { BotonesDePrueba(onProbarAhora, onProbarDespues) }

            // Actividad reciente
            item {
                Seccion(
                    titulo = stringResource(R.string.section_history),
                    subtitulo = stringResource(R.string.history_hint),
                    accion = if (correos.isNotEmpty()) {
                        { TextButton(onClick = onBorrarHistorial) { Text(stringResource(R.string.history_clear)) } }
                    } else {
                        null
                    }
                )
            }
            item {
                Tarjeta(relleno = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    if (correos.isEmpty()) {
                        FilaVacia(Icons.Outlined.Email, stringResource(R.string.history_empty))
                    }
                    correosVisibles.forEachIndexed { i, d ->
                        if (i > 0) Separador()
                        FilaDeCorreo(d)
                    }
                    if (correos.size > CORREOS_VISIBLES) {
                        TextButton(
                            onClick = { historialAbierto = !historialAbierto },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                if (historialAbierto) stringResource(R.string.history_show_less)
                                else stringResource(R.string.history_show_all, correos.size)
                            )
                        }
                    }
                }
            }

            // Permisos: una linea; adentro, solo lo pendiente (o todo, si se abre)
            item {
                TarjetaDePermisos(
                    permisos = permisos,
                    abiertos = permisosAbiertos,
                    onAlternar = { permisosAbiertos = !permisosAbiertos },
                    onResolver = onResolverPermiso
                )
            }

            // Apps que escucho
            item { Seccion(stringResource(R.string.section_apps)) }
            item {
                Tarjeta(relleno = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    if (appsInstaladas.isEmpty()) FilaVacia(Icons.Outlined.Email, stringResource(R.string.apps_none))
                    appsInstaladas.forEachIndexed { i, app ->
                        if (i > 0) Separador()
                        FilaDeApp(app, app.paquete in appsEscuchadas) { onCambiarApp(app.paquete, it) }
                    }
                }
            }
        }

        // Como en el reloj: el boton para agregar siempre a mano, sin scrollear.
        // Sin alarmas no hace falta: la tarjeta de la primera ya es el boton.
        if (reglas.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = nuevaAlarma,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_new_rule), style = MaterialTheme.typography.labelLarge) },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            )
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

// --- Encabezado y alertas ---

/** Logo, nombre, y si la app esta escuchando: la prueba de vida, de un vistazo. */
@Composable
private fun Encabezado(appsEscuchadas: List<AppCorreo>, escuchando: Boolean, onAbrirAjustes: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
    ) {
        Logo(48.dp)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (escuchando) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val activo = appsEscuchadas.isNotEmpty()
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(
                                if (activo) MaterialTheme.colorScheme.primary else Aviso.texto,
                                CircleShape
                            )
                    )
                    Text(
                        text = if (activo) {
                            stringResource(R.string.listening, appsEscuchadas.joinToString(" · ") { it.nombre })
                        } else {
                            stringResource(R.string.listening_none)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }
        IconButton(onClick = onAbrirAjustes) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.action_settings),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** El icono de la app: sobre verde con el fondo redondeado, como en el cajon de apps. */
@Composable
internal fun Logo(tamano: Dp) {
    Box(
        modifier = Modifier
            .size(tamano)
            .clip(RoundedCornerShape(tamano * 0.28f))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        // El dibujo trae el margen de los iconos adaptativos (108 con 18 de
        // cada lado): se agranda para que el sobre llene la baldosa.
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.requiredSize(tamano * 1.5f)
        )
    }
}

/** Roja: una alarma no va a sonar. El problema y el boton que lo arregla, juntos. */
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

    Tarjeta(color = color.copy(alpha = 0.08f)) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = color)
            Text(
                texto,
                style = MaterialTheme.typography.titleSmall,
                color = color,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
        Button(
            onClick = accion,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color)
        ) { Text(stringResource(boton)) }
    }
}

// --- Alarmas ---

/** Sin alarmas, una invitacion grande, no un mensaje de error. */
@Composable
private fun PrimeraAlarma(onCrear: () -> Unit) {
    Tarjeta(color = MaterialTheme.colorScheme.primaryContainer, relleno = PaddingValues(24.dp)) {
        Box(
            Modifier.size(64.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.AlarmAdd,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.empty_alarms_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            stringResource(R.string.empty_alarms_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Button(
            onClick = onCrear,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_create_first_rule), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Como una alarma del reloj: icono, nombre grande, switch a la derecha, y
 * tocarla la abre para editar. Apagada se ve atenuada.
 */
@Composable
private fun TarjetaDeAlarma(
    regla: Regla,
    avisoDeDireccion: Boolean,
    onCambiarActiva: (Boolean) -> Unit,
    onEditar: () -> Unit
) {
    val encendida = regla.activa
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEditar)
                .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 18.dp),
            verticalAlignment = Alignment.Top
        ) {
            Circulo(
                icono = Icons.Rounded.Alarm,
                fondo = if (encendida) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                tinte = if (encendida) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
                    .alpha(if (encendida) 1f else 0.5f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = regla.nombre,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (regla.remitente.isNotBlank()) {
                    Condicion(Icons.Outlined.Person, regla.remitente, stringResource(R.string.cd_sender))
                }
                if (regla.palabraClave.isNotBlank()) {
                    // Con las dos, se lee como una frase: basta con una.
                    val texto = if (regla.remitente.isNotBlank()) {
                        stringResource(R.string.rule_or_keyword, regla.palabraClave)
                    } else {
                        stringResource(R.string.rule_keyword, regla.palabraClave)
                    }
                    Condicion(Icons.Outlined.TextFields, texto, stringResource(R.string.cd_keyword))
                }
                if (regla.sonido != null) {
                    Condicion(Icons.Outlined.MusicNote, nombreDelSonido(regla.sonido), stringResource(R.string.cd_sound))
                }
                if (avisoDeDireccion) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .background(Aviso.fondo, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = Aviso.texto, modifier = Modifier.size(16.dp))
                        Text(
                            stringResource(R.string.rule_address_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = Aviso.texto,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
            Switch(
                checked = encendida,
                onCheckedChange = onCambiarActiva,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

/** Una condicion de la alarma: icono chico y el texto. */
@Composable
private fun Condicion(icono: ImageVector, texto: String, descripcion: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = descripcion, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Text(
            texto,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

/** Probar a mano pero sin robar protagonismo a las alarmas. */
@Composable
private fun BotonesDePrueba(onProbarAhora: () -> Unit, onProbarDespues: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        FilledTonalButton(
            onClick = onProbarAhora,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.test_now))
        }
        OutlinedButton(
            onClick = onProbarDespues,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.test_later))
        }
    }
}

// --- Actividad ---

/** Icono de la app, quien, cuando, el asunto, y que decidio Email Alarm. */
@Composable
private fun FilaDeCorreo(d: Deteccion) {
    val cuando = DateUtils.formatSameDayTime(
        d.hora, System.currentTimeMillis(), java.text.DateFormat.SHORT, java.text.DateFormat.SHORT
    ).toString()

    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.Top) {
        IconoDeApp(AppsDeCorreo.porNombre(d.app)?.paquete, 36.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    d.remitente,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    cuando,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(
                sinFirmasParaMostrar(d.asunto),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            ResultadoDelCorreo(d, Modifier.padding(top = 4.dp))
        }
    }
}

/** Verde si sono, gris si no coincidio, rojo solo si debia sonar y no sono. */
@Composable
private fun ResultadoDelCorreo(d: Deteccion, modifier: Modifier = Modifier) {
    val resultado = runCatching { Resultado.valueOf(d.resultado) }.getOrDefault(Resultado.SIN_COINCIDENCIA)
    val verde = MaterialTheme.colorScheme.primary
    val gris = MaterialTheme.colorScheme.onSurfaceVariant
    val rojo = MaterialTheme.colorScheme.error
    val (texto, color, icono) = when (resultado) {
        Resultado.SONO -> Triple(stringResource(R.string.history_rang, d.regla.orEmpty()), verde, Icons.Rounded.NotificationsActive)
        Resultado.SIN_SUSCRIPCION -> Triple(stringResource(R.string.history_no_sub), rojo, Icons.Rounded.PriorityHigh)
        Resultado.SIN_COINCIDENCIA, Resultado.AVISO_CUENTA -> Triple(stringResource(R.string.history_no_match), gris, null)
    }
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(texto, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** El icono real de la app de correo, como lo ve la persona en su telefono. */
@Composable
private fun IconoDeApp(paquete: String?, tamano: Dp) {
    val contexto = LocalContext.current
    val icono = remember(paquete) {
        paquete?.let {
            runCatching { contexto.packageManager.getApplicationIcon(it).toBitmap(128, 128).asImageBitmap() }.getOrNull()
        }
    }
    if (icono != null) {
        Image(icono, contentDescription = null, modifier = Modifier.size(tamano))
    } else {
        Circulo(Icons.Outlined.Email, tamano = tamano)
    }
}

// --- Permisos y apps ---

@Composable
private fun TarjetaDePermisos(
    permisos: List<EstadoDePermiso>,
    abiertos: Boolean,
    onAlternar: () -> Unit,
    onResolver: (EstadoDePermiso) -> Unit
) {
    val concedidos = permisos.count { it.concedido }
    val todos = concedidos == permisos.size
    val visibles = if (abiertos) permisos else permisos.filter { !it.concedido }

    Tarjeta(relleno = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onAlternar)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Circulo(
                icono = if (todos) Icons.Rounded.VerifiedUser else Icons.Rounded.Shield,
                fondo = if (todos) MaterialTheme.colorScheme.primaryContainer else Aviso.fondo,
                tinte = if (todos) MaterialTheme.colorScheme.primary else Aviso.texto
            )
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(stringResource(R.string.section_permissions), style = MaterialTheme.typography.titleMedium)
                Ayuda(stringResource(R.string.permissions_summary, concedidos, permisos.size))
            }
            Icon(
                if (abiertos) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = stringResource(if (abiertos) R.string.permissions_hide else R.string.permissions_show),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        visibles.forEach { permiso ->
            Separador()
            FilaDePermiso(permiso) { onResolver(permiso) }
        }
    }
}

@Composable
private fun FilaDePermiso(permiso: EstadoDePermiso, onResolver: () -> Unit) {
    // Rojo solo si sin este permiso la alarma no suena. Los opcionales, ambar.
    val color = when {
        permiso.concedido -> MaterialTheme.colorScheme.primary
        permiso.imprescindible -> MaterialTheme.colorScheme.error
        else -> Aviso.texto
    }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(28.dp).background(color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (permiso.concedido) Icons.Rounded.Check else Icons.Rounded.PriorityHigh,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(stringResource(permiso.titulo), style = MaterialTheme.typography.titleSmall)
            if (!permiso.concedido) Ayuda(stringResource(permiso.explicacion))
        }
        if (!permiso.concedido && permiso.intent != null) {
            TextButton(onClick = onResolver) { Text(stringResource(R.string.action_enable)) }
        }
    }
}

@Composable
private fun FilaDeApp(app: AppCorreo, escuchada: Boolean, onCambiar: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCambiar(!escuchada) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconoDeApp(app.paquete, 36.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(app.nombre, style = MaterialTheme.typography.titleSmall)
            if (!app.exponeDireccion) Ayuda(stringResource(R.string.app_no_address))
        }
        Switch(checked = escuchada, onCheckedChange = onCambiar)
    }
}

@Composable
private fun FilaVacia(icono: ImageVector, texto: String) {
    Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Circulo(icono, tamano = 36.dp)
        Ayuda(texto, Modifier.padding(start = 12.dp))
    }
}

// --- Piezas compartidas con el dialogo y los ajustes ---

/** Titulo de seccion, con una accion opcional a la derecha. */
@Composable
internal fun Seccion(
    titulo: String,
    subtitulo: String? = null,
    accion: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            if (subtitulo != null) Ayuda(subtitulo)
        }
        accion?.invoke()
    }
}

/** Icono dentro de un circulo de color suave: marca de que tipo es cada tarjeta. */
@Composable
internal fun Circulo(
    icono: ImageVector,
    fondo: Color = MaterialTheme.colorScheme.primaryContainer,
    tinte: Color = MaterialTheme.colorScheme.primary,
    tamano: Dp = 44.dp
) {
    Box(Modifier.size(tamano).background(fondo, CircleShape), contentAlignment = Alignment.Center) {
        Icon(icono, contentDescription = null, tint = tinte, modifier = Modifier.size(tamano * 0.52f))
    }
}

/** Tarjeta blanca con aire adentro y una sombra minima que la separa del fondo. */
@Composable
internal fun Tarjeta(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    relleno: PaddingValues = PaddingValues(18.dp),
    contenido: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (color == MaterialTheme.colorScheme.surface) 1.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(relleno),
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
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    )
}

/**
 * Una alarma que busca una direccion falla en apps que solo publican el nombre
 * cuando lo que va antes de la @ no sirve para reconocerlo ("rrhh@", "info@").
 * Si sirve ("lucianoreverberi@" con "Luciano Reverberi"), no se molesta a nadie.
 */
private fun necesitaAvisoDeDireccion(
    regla: Regla,
    instaladas: List<AppCorreo>,
    escuchadas: Set<String>
): Boolean {
    if (!regla.remitente.contains("@")) return false
    if (MotorDeReglas.parteLocalUtil(regla.remitente) != null) return false
    return instaladas.any { it.paquete in escuchadas && !it.exponeDireccion }
}
