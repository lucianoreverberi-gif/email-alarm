package com.reverstabilizer.emailalarm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EsquemaClaro = lightColorScheme(
    primary = VerdeMarca,
    onPrimary = Color.White,
    primaryContainer = VerdeFondo,
    onPrimaryContainer = Color(0xFF0B4F2C),
    secondary = VerdeMarca,
    background = GrisFondo,
    onBackground = GrisTexto,
    surface = Color.White,
    onSurface = GrisTexto,
    error = RojoAviso
)

private val EsquemaOscuro = darkColorScheme(
    primary = VerdeClaro,
    onPrimary = Color(0xFF07281A),
    primaryContainer = Color(0xFF125234),
    onPrimaryContainer = VerdeFondo,
    secondary = VerdeClaro,
    background = NegroAlarma,
    onBackground = BlancoAlarma,
    surface = Color(0xFF171C19),
    onSurface = BlancoAlarma,
    error = RojoAvisoClaro
)

/**
 * Sin color dinamico a proposito: Material You tomaria los colores del fondo de
 * pantalla del usuario y la app perderia el verde de la marca en cada telefono.
 */
@Composable
fun EmailalarmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro,
        typography = Typography,
        content = content
    )
}
