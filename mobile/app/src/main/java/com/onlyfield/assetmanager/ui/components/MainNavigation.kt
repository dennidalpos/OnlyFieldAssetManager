package com.onlyfield.assetmanager.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
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
import com.onlyfield.assetmanager.configurator.ProjectDestination
import com.onlyfield.assetmanager.ui.LocalMessages
import com.onlyfield.assetmanager.ui.Screen

/** Bottom-bar tab: screen, icon source and a short label that fits 360 dp with large text. */
class MainTab(val screen: Screen, val destination: ProjectDestination?) {
    val labelKey get() = destination?.labelKey ?: "ux.nav.projectHome"
}

/** "More" (project tools) lists everything else. */
val mainTabs: List<MainTab> = listOf(
    MainTab(Screen.Home, ProjectDestination.MAP),
    MainTab(Screen.Inventory, ProjectDestination.DEVICES),
    MainTab(Screen.Racks, ProjectDestination.RACKS),
    MainTab(Screen.Cabling, ProjectDestination.CABLING),
    MainTab(Screen.ProjectTools, null),
)

/** Primary destination for detail and secondary screens. */
fun mainTabFor(screen: Screen): Screen = when (screen) {
    Screen.Home, Screen.Floorplan -> Screen.Home
    Screen.Inventory, is Screen.DeviceDetail -> Screen.Inventory
    Screen.Racks, is Screen.RackDetail -> Screen.Racks
    Screen.Cabling -> Screen.Cabling
    else -> Screen.ProjectTools
}

/** Open full-page overlays (editors, scanner); navigation remains visible but disabled while any is shown. */
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
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 0.dp) {
        mainTabs.forEach { tab ->
            NavigationBarItem(enabled = enabled, selected = current == tab.screen, onClick = { if (current != tab.screen) onSelect(tab.screen) },
                icon = { Icon(tab.destination?.icon ?: Icons.Default.Menu, contentDescription = null) },
                // Keep large labels inside their own touch target.
                label = { Text(i18n.text(tab.labelKey), style = MaterialTheme.typography.labelSmall, maxLines = 2) })
        }
    }
}

/** Same destinations and labels at tablet widths. */
@Composable
fun MainNavigationRail(current: Screen, enabled: Boolean = true, onSelect: (Screen) -> Unit) {
    val i18n = LocalMessages.current
    NavigationRail(Modifier.fillMaxHeight().width(104.dp).verticalScroll(rememberScrollState()), containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        mainTabs.forEach { tab ->
            NavigationRailItem(enabled = enabled, selected = current == tab.screen, onClick = { if (current != tab.screen) onSelect(tab.screen) },
                icon = { Icon(tab.destination?.icon ?: Icons.Default.Menu, null) },
                label = { Text(i18n.text(tab.labelKey), maxLines = 2, style = MaterialTheme.typography.labelSmall) })
        }
    }
}
