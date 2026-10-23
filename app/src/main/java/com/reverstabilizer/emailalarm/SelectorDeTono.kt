package com.reverstabilizer.emailalarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Un icono por tono: se reconoce de un vistazo, sin leer. */
private fun iconoDe(tono: Tono): ImageVector = when (tono) {
    Tono.ALARMA -> Icons.AutoMirrored.Rounded.VolumeUp
    Tono.AURORA -> Icons.Outlined.WbTwilight
    Tono.CAMPANAS -> Icons.Outlined.NotificationsNone
    Tono.PULSO -> Icons.Outlined.MonitorHeart
    Tono.BRISA -> Icons.Outlined.Air
    Tono.DIGITAL -> Icons.Outlined.Memory
}

/**
 * Los seis tonos de la app en tarjetas, mas la puerta al selector del telefono.
 *
 * [sonido] es lo que tiene guardado la regla: la uri de un tono de la app, la
 * de uno del telefono, o null para el de alarma del sistema.
 */
@Composable
fun SelectorDeTono(
    sonido: String?,
    onElegir: (String?) -> Unit,
    onAbrirSelectorDelSistema: () -> Unit
) {
    val contexto = LocalContext.current
    val elegido = remember(sonido) { Tono.desde(sonido) }
    val prueba = remember { ReproductorDePrueba() }

    // Si se cierra el dialogo con un tono sonando, se corta. Una prueba que
    // sigue sonando sola seria exactamente lo que la app promete evitar.
    DisposableEffect(Unit) { onDispose { prueba.detener() } }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.field_sound), fontWeight = FontWeight.SemiBold)

        // Dos columnas armadas a mano: dentro de un dialogo que ya se desplaza,
        // una grilla con su propio desplazamiento pelea con el de afuera.
        Tono.entries.chunked(2).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                fila.forEach { tono ->
                    TarjetaDeTono(
                        tono = tono,
                        seleccionado = tono == elegido,
                        modifier = Modifier.weight(1f)
                    ) {
                        onElegir(tono.comoTexto(contexto))
                        prueba.reproducir(contexto, tono.uri(contexto))
                    }
                }
                // Si la fila quedara con uno solo, el hueco mantiene el ancho.
                if (fila.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        // El tono del telefono no tenia tarjeta: era una linea gris de ayuda que
        // no se parecia a nada de lo de arriba. Al volver del selector del
        // sistema, que no confirma nada al salir, no quedaba claro si la
        // eleccion habia entrado. Ahora es una tarjeta mas: mismo borde, mismo
        // tilde. Y como "sin tono propio" significa el de alarma del telefono,
        // esta tarjeta tambien cubre ese caso: siempre hay exactamente un tilde
        // en la pantalla.
        FilaDelTelefono(
            seleccionado = elegido == null,
            nombre = nombreDelSonido(sonido),
            onClick = {
                prueba.detener()
                onAbrirSelectorDelSistema()
            }
        )

        Ayuda(stringResource(R.string.sound_bundled_help, Tono.SEGUNDOS))

        // Probar vale para cualquier tono, no solo para los seis de la app:
        // el del telefono es justo el que menos se conoce de memoria.
        val paraProbar: Uri? = elegido?.uri(contexto)
            ?: sonido?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        TextButton(
            enabled = paraProbar != null,
            onClick = { paraProbar?.let { prueba.reproducir(contexto, it) } }
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                stringResource(R.string.sound_preview),
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

@Composable
private fun TarjetaDeTono(
    tono: Tono,
    seleccionado: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borde = if (seleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val fondo = if (seleccionado) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(fondo)
            .border(if (seleccionado) 2.dp else 1.dp, borde, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                iconoDe(tono),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.weight(1f))
            if (seleccionado) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = stringResource(R.string.sound_selected),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(stringResource(tono.etiqueta), fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.sound_duration, Tono.SEGUNDOS),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * El tono que viene del telefono, con el mismo peso visual que los seis de la app.
 *
 * Cuando esta elegido muestra el nombre del tono debajo: es la unica confirmacion
 * que el usuario recibe, porque el selector del sistema se cierra sin decir nada.
 */
@Composable
private fun FilaDelTelefono(
    seleccionado: Boolean,
    nombre: String,
    onClick: () -> Unit
) {
    val borde = if (seleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val fondo = if (seleccionado) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(fondo)
            .border(if (seleccionado) 2.dp else 1.dp, borde, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.Smartphone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                stringResource(
                    if (seleccionado) R.string.sound_phone_title else R.string.sound_from_phone
                ),
                fontWeight = FontWeight.Bold
            )
            if (seleccionado) {
                Text(
                    nombre,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (seleccionado) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = stringResource(R.string.sound_selected),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.size(6.dp))
        }
        // La flecha dice que la fila lleva a otra pantalla, este o no elegido.
        Icon(
            Icons.Rounded.KeyboardArrowRight,
            contentDescription = stringResource(R.string.sound_change),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * La prueba del tono mientras se arma la regla.
 *
 * Suena por el canal de alarma, igual que sonaria de verdad: probar por el
 * canal de multimedia enganaria, porque el volumen es otro. No hace bucle y
 * se corta sola: una prueba es una prueba.
 */
private class ReproductorDePrueba {
    private var mp: MediaPlayer? = null

    fun reproducir(context: Context, tono: Uri) {
        detener()
        mp = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context.applicationContext, tono)
                isLooping = false
                setOnCompletionListener { detener() }
                prepare()
                start()
            }
        }.getOrNull()
    }

    fun detener() {
        mp?.let { p ->
            runCatching { if (p.isPlaying) p.stop() }
            runCatching { p.release() }
        }
        mp = null
    }
}
