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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taiful.tfnewscraft.R

val NotoSansBengali = FontFamily(
    Font(R.font.noto_sans_bengali, FontWeight.Normal),
    Font(R.font.noto_sans_bengali, FontWeight.Medium),
    Font(R.font.noto_sans_bengali, FontWeight.SemiBold),
    Font(R.font.noto_sans_bengali, FontWeight.Bold)
)

val NotoSerifBengali = FontFamily(
    Font(R.font.noto_serif_bengali, FontWeight.Normal),
    Font(R.font.noto_serif_bengali, FontWeight.Bold)
)

val TiroBangla = FontFamily(
    Font(R.font.tiro_bangla, FontWeight.Normal)
)

// Editorial Newsroom & Broadcast Studio Palette
private val NewsroomCrimson = Color(0xFFC81E2B)
private val NewsroomCrimsonSoft = Color(0xFFFDECEE)
private val EditorialNavy = Color(0xFF153866)
private val WarmNewsprint = Color(0xFFF6F4EF)
private val PaperSurface = Color(0xFFFFFFFF)
private val PaperVariant = Color(0xFFEDEAE2)
private val InkPrimary = Color(0xFF181715)
private val InkMuted = Color(0xFF5E5A52)
private val Hairline = Color(0xFFDDD8CC)

private val BroadcastObsidian = Color(0xFF0E1117)
private val BroadcastCard = Color(0xFF171C26)
private val BroadcastElevated = Color(0xFF212836)
private val BroadcastRed = Color(0xFFFF5C67)
private val BroadcastGold = Color(0xFFF5B942)
private val BroadcastText = Color(0xFFF1EFEA)
private val BroadcastMuted = Color(0xFFA6AAB5)

private val EditorialLightColors = lightColorScheme(
    primary = NewsroomCrimson,
    onPrimary = Color.White,
    primaryContainer = NewsroomCrimsonSoft,
    onPrimaryContainer = Color(0xFF690A13),
    secondary = EditorialNavy,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1ECF9),
    onSecondaryContainer = Color(0xFF0D2342),
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    background = WarmNewsprint,
    onBackground = InkPrimary,
    surface = PaperSurface,
    onSurface = InkPrimary,
    surfaceVariant = PaperVariant,
    onSurfaceVariant = InkMuted,
    outline = Hairline,
    outlineVariant = Color(0xFFEAE6DC)
)

private val BroadcastDarkColors = darkColorScheme(
    primary = BroadcastRed,
    onPrimary = Color(0xFF3E040A),
    primaryContainer = Color(0xFF58111A),
    onPrimaryContainer = Color(0xFFFFD9DC),
    secondary = Color(0xFF7CB2FF),
    onSecondary = Color(0xFF0A2447),
    secondaryContainer = Color(0xFF1B355C),
    onSecondaryContainer = Color(0xFFD6E6FF),
    tertiary = BroadcastGold,
    onTertiary = Color(0xFF332100),
    background = BroadcastObsidian,
    onBackground = BroadcastText,
    surface = BroadcastCard,
    onSurface = BroadcastText,
    surfaceVariant = BroadcastElevated,
    onSurfaceVariant = BroadcastMuted,
    outline = Color(0xFF313A4B),
    outlineVariant = Color(0xFF252D3B)
)

val StudioTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = NotoSerifBengali,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 34.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = NotoSerifBengali,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 29.sp
    ),
    titleLarge = TextStyle(
        fontFamily = NotoSerifBengali,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = NotoSansBengali,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = NotoSansBengali,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = NotoSansBengali,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = NotoSansBengali,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    bodySmall = TextStyle(
        fontFamily = NotoSansBengali,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = NotoSansBengali,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = NotoSansBengali,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = NotoSansBengali,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

val StudioShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun TFReelsPostMakerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) BroadcastDarkColors else EditorialLightColors,
        typography = StudioTypography,
        shapes = StudioShapes,
        content = content
    )
}
