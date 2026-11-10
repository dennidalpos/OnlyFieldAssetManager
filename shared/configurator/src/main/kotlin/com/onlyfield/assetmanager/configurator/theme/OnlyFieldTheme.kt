package com.onlyfield.assetmanager.configurator.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val Light = lightColorScheme(
    primary = Color(0xFF174E78),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9EAF8),
    onPrimaryContainer = Color(0xFF103B5C),
    inversePrimary = Color(0xFFA4CEF0),
    secondary = Color(0xFF506474),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0EAF1),
    onSecondaryContainer = Color(0xFF263E4E),
    tertiary = Color(0xFF006B68),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC1EEEA),
    onTertiaryContainer = Color(0xFF004D4A),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF3F6F9),
    onBackground = Color(0xFF192B39),
    surface = Color(0xFFFCFDFE),
    onSurface = Color(0xFF192B39),
    surfaceVariant = Color(0xFFE2EAF0),
    onSurfaceVariant = Color(0xFF4D606E),
    surfaceTint = Color(0xFF174E78),
    inverseSurface = Color(0xFF293D4B),
    inverseOnSurface = Color(0xFFEDF3F7),
    outline = Color(0xFF718390),
    outlineVariant = Color(0xFFCAD6DF),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFCFDFE),
    surfaceDim = Color(0xFFD6E0E7),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFECF1F5),
    surfaceContainerHigh = Color(0xFFE5ECF2),
    surfaceContainerHighest = Color(0xFFDDE6EE),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFA4CEF0),
    onPrimary = Color(0xFF07304E),
    primaryContainer = Color(0xFF214C6A),
    onPrimaryContainer = Color(0xFFD9EAF8),
    inversePrimary = Color(0xFF174E78),
    secondary = Color(0xFFB8CBD9),
    onSecondary = Color(0xFF233744),
    secondaryContainer = Color(0xFF344B5B),
    onSecondaryContainer = Color(0xFFDFEAF2),
    tertiary = Color(0xFF78D6CD),
    onTertiary = Color(0xFF003B38),
    tertiaryContainer = Color(0xFF00534F),
    onTertiaryContainer = Color(0xFFC1EEEA),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0D1821),
    onBackground = Color(0xFFE0EAF1),
    surface = Color(0xFF101D27),
    onSurface = Color(0xFFE0EAF1),
    surfaceVariant = Color(0xFF304450),
    onSurfaceVariant = Color(0xFFB7C7D2),
    surfaceTint = Color(0xFFA4CEF0),
    inverseSurface = Color(0xFFE0EAF1),
    inverseOnSurface = Color(0xFF293D4B),
    outline = Color(0xFF889EAD),
    outlineVariant = Color(0xFF3D5261),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF344753),
    surfaceDim = Color(0xFF0D1821),
    surfaceContainerLowest = Color(0xFF09131B),
    surfaceContainerLow = Color(0xFF152530),
    surfaceContainer = Color(0xFF1B2D39),
    surfaceContainerHigh = Color(0xFF243845),
    surfaceContainerHighest = Color(0xFF2E4350),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

private val DefaultTypography = Typography()
private val AppTypography = Typography(
    headlineLarge = DefaultTypography.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = DefaultTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = DefaultTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = DefaultTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
)

@Composable
fun OnlyFieldTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = AppTypography, shapes = AppShapes, content = content)
}

// M3 buttons use a fixed full-corner token that Shapes cannot override, hence these wrappers.
// Import them explicitly: an explicit import wins over `androidx.compose.material3.*`.
val ButtonShape: Shape = RoundedCornerShape(8.dp)
val ButtonPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
private val TextButtonPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonShape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = ButtonPadding,
    content: @Composable RowScope.() -> Unit,
) = androidx.compose.material3.Button(onClick, modifier, enabled, shape, colors, contentPadding = contentPadding, content = content)

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    contentPadding: PaddingValues = ButtonPadding,
    content: @Composable RowScope.() -> Unit,
) = androidx.compose.material3.OutlinedButton(onClick, modifier, enabled, shape, colors, contentPadding = contentPadding, content = content)

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    contentPadding: PaddingValues = TextButtonPadding,
    content: @Composable RowScope.() -> Unit,
) = androidx.compose.material3.TextButton(onClick, modifier, enabled, shape, colors, contentPadding = contentPadding, content = content)

/** Shared spacing for page hosts, fields and action groups. */
object AppSpacing {
    val tiny = 4.dp
    val small = 8.dp
    val inset = 12.dp
    val content = 16.dp
    val section = 24.dp
    val formWidth = 640.dp
    fun page(width: androidx.compose.ui.unit.Dp) = if (width < 600.dp) content else section
}
