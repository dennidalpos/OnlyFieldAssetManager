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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Modifier
import com.onlyfield.assetmanager.configurator.ProjectDestination
import com.onlyfield.assetmanager.ui.LocalMessages
import com.onlyfield.assetmanager.ui.Screen

/** Bottom-bar tab: screen, icon source and a short label that fits 360 dp with large text. */
class MainTab(val screen: Screen, val destination: ProjectDestination?, val labelKey: String)

/** "More" (project tools) lists everything else. */
val mainTabs: List<MainTab> = listOf(
    MainTab(Screen.Home, ProjectDestination.MAP, "ux.tab.map"),
    MainTab(Screen.Inventory, ProjectDestination.DEVICES, "ux.tab.devices"),
    MainTab(Screen.Racks, ProjectDestination.RACKS, "ux.tab.racks"),
    MainTab(Screen.Cabling, ProjectDestination.CABLING, "ux.tab.cabling"),
    MainTab(Screen.ProjectTools, null, "ux.nav.more"),
)

/** Open full-page overlays (editors, scanner); the bottom bar hides while any is shown. */
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
fun MainNavigationBar(current: Screen, onSelect: (Screen) -> Unit) {
    val i18n = LocalMessages.current
    NavigationBar {
        mainTabs.forEach { tab ->
            NavigationBarItem(selected = current == tab.screen, onClick = { if (current != tab.screen) onSelect(tab.screen) },
                icon = { Icon(tab.destination?.icon ?: Icons.Default.Menu, contentDescription = null) },
                // Short labels may use the neighbours' padding instead of being cut at large text sizes.
                label = { Text(i18n.text(tab.labelKey), style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false,
                    modifier = Modifier.wrapContentWidth(unbounded = true)) })
        }
    }
}
