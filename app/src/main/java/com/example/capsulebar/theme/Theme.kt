package com.example.capsulebar.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// == MATERIAL 3 OFFICIAL SHAPE SCALE ==========================================
val CapsuleBarShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(8.dp),
    medium     = RoundedCornerShape(12.dp),
    large      = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// == PURE BLACK DARK PALETTE (AMOLED 100% Black with Material You tones) ======
val PureBlack = Color(0xFF000000)
val DarkSurfaceElevated = Color(0xFF09090C)
val DarkContainer = Color(0xFF141418)
val DarkContainerHigh = Color(0xFF1D1D23)
val DarkContainerHighest = Color(0xFF272730)
val DarkBorder = Color(0xFF2C2C36)

// == PURE WHITE LIGHT PALETTE (Crisp 100% White with Material You tones) ========
val PureWhite = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFFFFFFF)
val LightContainer = Color(0xFFF3F5F9)
val LightContainerHigh = Color(0xFFE9ECF2)
val LightContainerHighest = Color(0xFFDFE3EC)
val LightBorder = Color(0xFFD6DBE4)

// Fallback Material You static palettes (Android < 12)
private val StaticDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    background = PureBlack,
    onBackground = Color(0xFFF2F2F6),
    surface = PureBlack,
    onSurface = Color(0xFFF2F2F6),
    surfaceVariant = DarkContainer,
    onSurfaceVariant = Color(0xFFA1A1AA),
    surfaceContainer = DarkContainer,
    surfaceContainerHigh = DarkContainerHigh,
    surfaceContainerHighest = DarkContainerHighest,
    outline = DarkBorder,
    outlineVariant = Color(0xFF1F1F26)
)

private val StaticLightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    background = PureWhite,
    onBackground = Color(0xFF191C1E),
    surface = PureWhite,
    onSurface = Color(0xFF191C1E),
    surfaceVariant = LightContainer,
    onSurfaceVariant = Color(0xFF44474E),
    surfaceContainer = LightContainer,
    surfaceContainerHigh = LightContainerHigh,
    surfaceContainerHighest = LightContainerHighest,
    outline = LightBorder,
    outlineVariant = Color(0xFFE2E6EE)
)

// M3 Full Typography Scale
val CapsuleBarTypography = Typography(
    titleLarge  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,  fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,  fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,  fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge   = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,  fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,  fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall   = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,  fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,  fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,  fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,  fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp)
)

@Composable
fun CapsuleBarTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = true, // Material You: pulls wallpaper colors on API 31+
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) {
                val dyn = dynamicDarkColorScheme(context)
                dyn.copy(
                    background = PureBlack,
                    surface = PureBlack,
                    surfaceContainerLowest = PureBlack,
                    surfaceContainerLow = DarkSurfaceElevated,
                    surfaceContainer = DarkContainer,
                    surfaceContainerHigh = DarkContainerHigh,
                    surfaceContainerHighest = DarkContainerHighest,
                    surfaceVariant = DarkContainer,
                    onBackground = Color(0xFFF2F2F6),
                    onSurface = Color(0xFFF2F2F6),
                    outline = DarkBorder
                )
            } else {
                val dyn = dynamicLightColorScheme(context)
                dyn.copy(
                    background = PureWhite,
                    surface = PureWhite,
                    surfaceContainerLowest = PureWhite,
                    surfaceContainerLow = LightSurfaceElevated,
                    surfaceContainer = LightContainer,
                    surfaceContainerHigh = LightContainerHigh,
                    surfaceContainerHighest = LightContainerHighest,
                    surfaceVariant = LightContainer,
                    onBackground = Color(0xFF191C1E),
                    onSurface = Color(0xFF191C1E),
                    outline = LightBorder
                )
            }
        }
        darkTheme -> StaticDarkColorScheme
        else -> StaticLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = CapsuleBarShapes,
        typography = CapsuleBarTypography,
        content = content
    )
}
