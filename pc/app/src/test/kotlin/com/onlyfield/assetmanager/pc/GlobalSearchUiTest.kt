package com.onlyfield.assetmanager.pc

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.onlyfield.assetmanager.configurator.map.GlobalSearchDialog
import com.onlyfield.assetmanager.core.display.SearchHit
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GlobalSearchUiTest {
    @get:Rule val rule = createComposeRule()

    private val north = Area(name = "Nord 1")
    private val remote = Device(technicalName = "SW-05", areaId = north.id, ipAddress = "10.1.1.5")
    private val project = Project(name = "Sito", createdEpochMs = 1, updatedEpochMs = 1, sites = listOf(Site(name = "Sede Nord", areas = listOf(north), devices = listOf(remote))))

    @Test fun typingFindsTheDeviceAndPickReturnsItsFloor() {
        var picked: SearchHit? = null
        rule.setContent { MaterialTheme { GlobalSearchDialog(project, Messages(), emptyList(), { picked = it }, {}) } }
        rule.onNodeWithText("Una porta si cerca con l'apparato, ad esempio «SW-01 P5».").assertExists()
        rule.onNode(hasSetTextAction()).performTextInput("10.1.1")
        rule.onNodeWithText("SW-05").performClick()
        rule.runOnIdle {
            assertEquals(remote.id, picked?.focus?.id)
            assertEquals(north.id, picked?.areaId)
        }
    }

    @Test fun emptyQueryListsRecentItems() {
        rule.setContent { MaterialTheme { GlobalSearchDialog(project, Messages(), listOf(remote.id), {}, {}) } }
        rule.onNodeWithText("Recenti").assertExists()
        rule.onNodeWithText("SW-05").assertExists()
    }
}
