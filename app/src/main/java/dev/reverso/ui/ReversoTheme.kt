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

private val DefaultTypography = Typography()
private val ReversoTypography = DefaultTypography.copy(
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = FontFamily.SansSerif),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = FontFamily.SansSerif),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = FontFamily.SansSerif),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = FontFamily.SansSerif),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = FontFamily.SansSerif),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = FontFamily.SansSerif),
    titleLarge = DefaultTypography.titleLarge.copy(fontFamily = FontFamily.SansSerif),
    titleMedium = DefaultTypography.titleMedium.copy(fontFamily = FontFamily.SansSerif),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = FontFamily.SansSerif),
    bodyLarge = DefaultTypography.bodyLarge.copy(fontFamily = FontFamily.SansSerif),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
    bodySmall = DefaultTypography.bodySmall.copy(fontFamily = FontFamily.SansSerif),
    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = FontFamily.SansSerif),
    labelMedium = DefaultTypography.labelMedium.copy(fontFamily = FontFamily.SansSerif),
    labelSmall = DefaultTypography.labelSmall.copy(fontFamily = FontFamily.SansSerif),
)

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
        typography = ReversoTypography,
        content = content,
    )
}
