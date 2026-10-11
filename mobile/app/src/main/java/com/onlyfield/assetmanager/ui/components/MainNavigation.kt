package com.onlyfield.assetmanager.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import com.onlyfield.assetmanager.configurator.ProjectArea
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import com.onlyfield.assetmanager.ui.LocalMessages
import com.onlyfield.assetmanager.ui.Screen

/** Bottom-bar tab: screen, icon source and a short label that fits 360 dp with large text. */
class MainTab(val screen: Screen, val area: ProjectArea) {
    val labelKey get() = area.labelKey
}

/** Project tools collect secondary modules and project operations. */
val mainTabs: List<MainTab> = listOf(
    MainTab(Screen.Home, ProjectArea.MAP),
    MainTab(Screen.Inventory, ProjectArea.INVENTORY),
    MainTab(Screen.Cabling, ProjectArea.CABLING),
    MainTab(Screen.ProjectTools, ProjectArea.PROJECT),
)

/** Primary destination for detail and secondary screens. */
fun mainTabFor(screen: Screen): Screen = when (screen) {
    Screen.Home, Screen.Floorplan -> Screen.Home
    Screen.Inventory, is Screen.DeviceDetail, Screen.Racks, is Screen.RackDetail -> Screen.Inventory
    Screen.Cabling -> Screen.Cabling
    else -> Screen.ProjectTools
}

/** Full-page overlays temporarily own the complete window. */
val LocalOverlayCount = staticCompositionLocalOf<MutableIntState?> { null }

/** Registers a full-page overlay for as long as it is composed. */
@Composable
fun TrackOverlay() {
    val count = LocalOverlayCount.current
    DisposableEffect(count) {
        count?.let { it.intValue++ }
        onDispose { count?.let { it.intValue-- } }
    }
}

@Composable
fun MainNavigationBar(current: Screen, enabled: Boolean = true, onSelect: (Screen) -> Unit) {
    val i18n = LocalMessages.current
    NavigationBar(modifier = Modifier.testTag("main-navigation"), containerColor = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 0.dp) {
        mainTabs.forEach { tab ->
            NavigationBarItem(enabled = enabled, selected = current == tab.screen, onClick = { if (current != tab.screen) onSelect(tab.screen) },
                icon = { Icon(tab.area.icon, contentDescription = null) },
                label = { Text(i18n.text(tab.labelKey), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) })
        }
    }
}

/** Same destinations and labels at tablet widths. */
@Composable
fun MainNavigationRail(current: Screen, enabled: Boolean = true, onSelect: (Screen) -> Unit) {
    val i18n = LocalMessages.current
    NavigationRail(Modifier.testTag("main-navigation").fillMaxHeight().width(104.dp).verticalScroll(rememberScrollState()), containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        mainTabs.forEach { tab ->
            NavigationRailItem(enabled = enabled, selected = current == tab.screen, onClick = { if (current != tab.screen) onSelect(tab.screen) },
                icon = { Icon(tab.area.icon, null) },
                label = { Text(i18n.text(tab.labelKey), maxLines = 2, style = MaterialTheme.typography.labelSmall) })
        }
    }
}
