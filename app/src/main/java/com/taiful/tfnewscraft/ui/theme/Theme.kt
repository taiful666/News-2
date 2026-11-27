package com.taiful.tfnewscraft.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val BrandBlue = Color(0xFF1877F2)
private val DeepNavy = Color(0xFF0B1430)
private val EditorialBlue = Color(0xFF1F4E79)
private val WarmPaper = Color(0xFFF6F5F2)
private val CrispWhite = Color(0xFFFFFFFF)
private val InkText = Color(0xFF1C1B19)
private val DarkBg = Color(0xFF121212)
private val DarkSurface = Color(0xFF1B1B1D)
private val DarkText = Color(0xFFE8E6E1)
private val DarkAccent = Color(0xFF8AB4E8)

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = CrispWhite,
    secondary = EditorialBlue,
    onSecondary = CrispWhite,
    background = WarmPaper,
    onBackground = InkText,
    surface = CrispWhite,
    onSurface = InkText,
    surfaceVariant = Color(0xFFF0EEE9),
    onSurfaceVariant = Color(0xFF6B675F),
    outline = Color(0xFFE2DED5)
)

private val DarkColors = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DeepNavy,
    secondary = BrandBlue,
    onSecondary = CrispWhite,
    background = DarkBg,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = Color(0xFF242427),
    onSurfaceVariant = Color(0xFFA3A09A),
    outline = Color(0xFF323236)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

@Composable
fun TFReelsPostMakerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        shapes = AppShapes,
        content = content
    )
}
