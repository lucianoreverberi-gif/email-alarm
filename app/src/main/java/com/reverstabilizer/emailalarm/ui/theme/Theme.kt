package com.reverstabilizer.emailalarm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

private val EsquemaClaro = lightColorScheme(
    primary = VerdeMarca,
    onPrimary = Color.White,
    primaryContainer = VerdeFondo,
    onPrimaryContainer = VerdeOscuro,
    secondary = VerdeMarca,
    secondaryContainer = VerdeFondo,
    onSecondaryContainer = VerdeOscuro,
    background = GrisFondo,
    onBackground = GrisTexto,
    surface = Color.White,
    onSurface = GrisTexto,
    surfaceVariant = GrisRelleno,
    onSurfaceVariant = GrisSuave,
    // Los dialogos y menus usan estos: sin ellos salen con tinte lila.
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F9F8),
    surfaceContainer = Color(0xFFF4F7F5),
    surfaceContainerHigh = Color(0xFFF9FBFA),
    surfaceContainerHighest = GrisRelleno,
    outline = Color(0xFF9AA39D),
    outlineVariant = GrisLinea,
    error = RojoAviso
)

private val EsquemaOscuro = darkColorScheme(
    primary = VerdeClaro,
    onPrimary = Color(0xFF07281A),
    primaryContainer = Color(0xFF125234),
    onPrimaryContainer = VerdeFondo,
    secondary = VerdeClaro,
    secondaryContainer = Color(0xFF125234),
    onSecondaryContainer = VerdeFondo,
    background = NegroAlarma,
    onBackground = BlancoAlarma,
    surface = Color(0xFF171C19),
    onSurface = BlancoAlarma,
    surfaceVariant = Color(0xFF242B27),
    onSurfaceVariant = GrisAlarma,
    surfaceContainerLowest = Color(0xFF0F1311),
    surfaceContainerLow = Color(0xFF141916),
    surfaceContainer = Color(0xFF181D1A),
    surfaceContainerHigh = Color(0xFF1D2320),
    surfaceContainerHighest = Color(0xFF242B27),
    outline = Color(0xFF6B746F),
    outlineVariant = Color(0xFF2C3430),
    error = RojoAvisoClaro
)

/** Colores de aviso (ambar), que Material no trae. */
object Aviso {
    val texto: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) AmbarTextoOscuro else AmbarTexto

    val fondo: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) AmbarFondoOscuro else AmbarFondo
}

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
