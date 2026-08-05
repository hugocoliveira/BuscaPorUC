package br.com.lit.busca.uc.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LitColorScheme = lightColorScheme(
    primary          = Primary,
    onPrimary        = OnPrimary,
    primaryContainer = PrimaryContainer,
    surface          = Surface,
    surfaceVariant   = SurfaceVariant,
    onSurface        = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    error            = ErrorColor,
    outline          = Outline
)

@Composable
fun BuscaPorUCTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LitColorScheme,
        typography  = AppTypography,
        content     = content
    )
}
