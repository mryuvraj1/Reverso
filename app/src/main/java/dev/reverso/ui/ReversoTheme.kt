package dev.reverso.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

object ReversoColors {
    val Background = Color(0xFF0B0F19)
    val Surface = Color(0xFF111827)
    val Panel = Color(0xFF151B2B)
    val Raised = Color(0xFF202B3D)
    val Border = Color(0xFF27344A)
    val Ink = Color(0xFFF4F6FC)
    val Muted = Color(0xFFA0ABC0)
    val Blue = Color(0xFF8B9BFF)
    val Purple = Color(0xFFB49AFF)
    val Mint = Color(0xFF00D6A3)
    val Warning = Color(0xFFFFC66D)
}

@Composable
fun ReversoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = ReversoColors.Blue,
            secondary = ReversoColors.Mint,
            tertiary = ReversoColors.Purple,
            background = ReversoColors.Background,
            surface = ReversoColors.Panel,
            surfaceVariant = ReversoColors.Raised,
            onBackground = ReversoColors.Ink,
            onSurface = ReversoColors.Ink,
            onSurfaceVariant = ReversoColors.Muted,
        ),
        typography = Typography(defaultFontFamily = FontFamily.SansSerif),
        content = content,
    )
}
