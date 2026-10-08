package com.onlyfield.assetmanager.configurator.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

private val Light = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = Color(0xFF545F71),
    secondaryContainer = Color(0xFFD8E3F8),
    tertiary = Color(0xFF00696E),
    tertiaryContainer = Color(0xFFB4ECEF),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    background = Color(0xFFF8F9FC),
    surface = Color(0xFFF8F9FC),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF003063),
    primaryContainer = Color(0xFF00468C),
    onPrimaryContainer = Color(0xFFD6E3FF),
    tertiary = Color(0xFF80D4D9),
)

/** Squared corners: dialogs, cards and menus read as defined panels, not pills. */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(6.dp),
    extraLarge = RoundedCornerShape(8.dp),
)

@Composable
fun OnlyFieldTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, shapes = AppShapes, content = content)
}

// M3 buttons use a fixed full-corner token that Shapes cannot override, hence these wrappers.
// Import them explicitly: an explicit import wins over `androidx.compose.material3.*`.
val ButtonShape: Shape = RoundedCornerShape(4.dp)
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
