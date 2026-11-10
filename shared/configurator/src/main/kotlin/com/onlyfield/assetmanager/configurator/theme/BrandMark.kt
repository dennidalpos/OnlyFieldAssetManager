package com.onlyfield.assetmanager.configurator.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val TerritoryMark = ImageVector.Builder("Territory", 108.dp, 108.dp, 108f, 108f).apply {
    addPath(addPathNodes("M0,0 H108 V108 H0 Z"), fill = SolidColor(Color(0xFF102C42)))
    addPath(addPathNodes("M54,26 C40,26 30,36 30,49 C30,64 54,82 54,82 C54,82 78,64 78,49 C78,36 68,26 54,26 Z"),
        stroke = SolidColor(Color(0xFFF4FAFF)), strokeLineWidth = 5f, strokeLineJoin = StrokeJoin.Round)
    addPath(addPathNodes("M43,45 L65,45 L54,61 Z"),
        stroke = SolidColor(Color(0xFF55D6C7)), strokeLineWidth = 3f, strokeLineJoin = StrokeJoin.Round)
    addPath(addPathNodes("M47,45 A4,4 0,1 1,39,45 A4,4 0,1 1,47,45 Z M69,45 A4,4 0,1 1,61,45 A4,4 0,1 1,69,45 Z M58,61 A4,4 0,1 1,50,61 A4,4 0,1 1,58,61 Z"),
        fill = SolidColor(Color(0xFF55D6C7)))
}.build()

/** Decorative mark; the adjacent heading supplies the accessible name. */
@Composable
internal fun BrandMark(size: Dp = 48.dp) {
    Surface(shape = MaterialTheme.shapes.medium) {
        Image(TerritoryMark, contentDescription = null, modifier = Modifier.size(size))
    }
}
