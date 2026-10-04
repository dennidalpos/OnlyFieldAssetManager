package com.onlyfield.assetmanager.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.style.TextOverflow
import com.onlyfield.assetmanager.configurator.ProjectDestination
import com.onlyfield.assetmanager.ui.LocalMessages
import com.onlyfield.assetmanager.ui.Screen

/** Screens reachable from the bottom bar; "More" (project tools) lists the rest. */
val mainTabs: List<Pair<Screen, ProjectDestination?>> = listOf(
    Screen.Home to ProjectDestination.MAP,
    Screen.Inventory to ProjectDestination.DEVICES,
    Screen.Racks to ProjectDestination.RACKS,
    Screen.Cabling to ProjectDestination.CABLING,
    Screen.ProjectTools to null,
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
        mainTabs.forEach { (screen, destination) ->
            val label = destination?.title(i18n) ?: i18n.text("ux.nav.more")
            NavigationBarItem(selected = current == screen, onClick = { if (current != screen) onSelect(screen) },
                icon = { Icon(destination?.icon ?: Icons.Default.Menu, contentDescription = null) },
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) })
        }
    }
}
