package com.onlyfield.assetmanager

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.platform.app.InstrumentationRegistry
import com.onlyfield.assetmanager.configurator.*
import com.onlyfield.assetmanager.configurator.theme.OnlyFieldTheme
import com.onlyfield.assetmanager.core.forms.*
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ConfiguratorMatrixNativeTest {
    @get:Rule val rule = createComposeRule()
    private val i18n = Messages()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val output by lazy {
        File(checkNotNull(context.getExternalFilesDir(null)), "configurator-matrix-evidence").apply { check(mkdirs() || isDirectory) }
    }
    private data class Display(val width: Int, val dark: Boolean, val font: Float) {
        val name get() = "${width}dp-${if (dark) "dark" else "light"}-$font"
    }
    private val displays = listOf(360, 412).flatMap { width ->
        listOf(false, true).flatMap { dark -> listOf(1f, 1.3f).map { Display(width, dark, it) } }
    }

    private fun device(name: String) = Device(technicalName = name, objectTypeId = "switch",
        category = DeviceCategory.NETWORK_SWITCH,
        hardware = HardwareSpec(portGroups = DevicePresets.forType("switch")!!.result(mapOf("ports" to "48", "uplinks" to "0")).groups))
        .let { it.copy(ports = HardwareConfigurator.ports(it.hardware.portGroups, it.id)) }

    @Test fun portActionsRemainReachableAndFooterControlsNeverOverlap() {
        val sw = device("SW-MATRIX")
        val peer = device("SW-PEER")
        val initial = Project(name = "Isolated footer matrix", createdEpochMs = 1, updatedEpochMs = 1,
            sites = listOf(Site(name = "Matrix site", devices = listOf(sw, peer))))
        val project = HardwareConfigurator.connect(initial, sw.ports.first().id, peer.ports.first().id, CableMedium.ETHERNET_COPPER)
        val display = mutableStateOf(displays.first())
        var showDetails by mutableStateOf(false)
        rule.setContent {
            val d = display.value
            CompositionLocalProvider(LocalDensity provides Density(context.resources.displayMetrics.widthPixels / d.width.toFloat(), d.font)) {
                OnlyFieldTheme(d.dark) { Surface {
                    PortQuickDialog(project, sw.ports.first().id, i18n,
                        PortQuickActions({ _, _ -> }, photo = { _, _ -> }, details = if (showDetails) ({ _ -> }) else null), {})
                } }
            }
        }
        displays.forEach { d -> listOf(false, true).forEach { details ->
            rule.runOnIdle { display.value = d; showDetails = details }
            snapshot("footer-${d.name}-details-$details")
            listOf("quick.photoPort", "quick.photoCable", "quick.insertPassage", "quick.disconnect").forEach { key ->
                rule.onNode(hasText(i18n.text(key)) and hasClickAction()).performScrollTo().assertIsDisplayed()
            }
            val keys = listOf("ux.close") +
                if (details) listOf("quick.details") else emptyList()
            val buttons = keys.map { key ->
                rule.onNode(hasText(i18n.text(key)) and hasClickAction()).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            }
            buttons.forEachIndexed { a, first -> buttons.drop(a + 1).forEachIndexed { offset, second ->
                assertFalse("${d.name}: ${keys[a]} overlaps ${keys[a + offset + 1]}", first.overlaps(second))
            } }
        } }
    }

    @Test fun densePortConfigurationCanSaveAndDiscardAcrossDisplayMatrix() {
        val sw = device("SW-MATRIX")
        val original = Project(name = "Isolated UI matrix", createdEpochMs = 1, updatedEpochMs = 1,
            sites = listOf(Site(name = "Matrix site", devices = listOf(sw))))
        val project = mutableStateOf(original)
        val display = mutableStateOf(displays.first())
        var generation by mutableIntStateOf(0)
        rule.setContent {
            val d = display.value
            CompositionLocalProvider(LocalDensity provides Density(context.resources.displayMetrics.widthPixels / d.width.toFloat(), d.font)) {
                OnlyFieldTheme(d.dark) { Surface {
                    key(generation) { PortQuickDialog(project.value, sw.ports.first().id, i18n,
                        PortQuickActions({ updated, _ -> project.value = updated }), {}) }
                } }
            }
        }
        displays.forEach { d ->
            rule.runOnIdle { display.value = d; project.value = original; generation++ }
            click("visual.configure")
            rule.onNodeWithContentDescription("P48: Libera").performScrollTo().assertIsDisplayed()
            snapshot("dense-${d.name}")
            click("visual.editLayout")
            rule.onNodeWithContentDescription("P1: Libera").performScrollTo().performClick()
            rule.onNodeWithContentDescription("P4: Libera").performScrollTo().performClick()
            rule.runOnIdle { assertEquals(original, project.value) }
            fixedAction("ux.saveChanges").performClick()
            rule.runOnIdle {
                val updated = project.value.sites.single().devices.single()
                assertEquals(sw.ports.map { it.id }, updated.ports.map { it.id })
                val order = PortArrangement.layout(updated, updated.ports).order
                assertEquals("4", order.first())
                assertEquals("1", order[25])
            }
            click("visual.configure")
            click("visual.poePorts")
            rule.onNodeWithContentDescription("P3: Libera").performScrollTo().performClick()
            click("visual.enablePoe")
            assertNull(project.value.sites.single().devices.single().ports.first { it.name == "P3" }.hardware.poeStandard)
            fixedAction("ux.saveChanges").performClick()
            assertEquals(listOf("P3"), project.value.sites.single().devices.single().ports.filter { it.hardware.poeStandard != null }.map { it.name })
            val committed = project.value
            click("visual.configure")
            click("visual.editLayout")
            rule.onNodeWithContentDescription("P5: Libera").performScrollTo().performClick()
            rule.onNodeWithContentDescription("P6: Libera").performScrollTo().performClick()
            rule.onNodeWithText(i18n.text("quick.back")).assertIsDisplayed().performClick()
            snapshot("discard-${d.name}")
            fixedAction("ux.cancelChanges").performClick()
            rule.runOnIdle { assertEquals(committed, project.value) }
        }
    }

    @Test fun occupiedPortActionsAndPathRemainReachableAcrossDisplayMatrix() {
        val sw = device("SW-MATRIX")
        val peer = device("SW-PEER")
        val initial = Project(name = "Isolated connected UI matrix", createdEpochMs = 1, updatedEpochMs = 1,
            sites = listOf(Site(name = "Matrix site", devices = listOf(sw, peer))))
        val original = HardwareConfigurator.connect(initial, sw.ports.first().id, peer.ports.first().id, CableMedium.ETHERNET_COPPER)
        val project = mutableStateOf(original)
        val display = mutableStateOf(displays.first())
        var generation by mutableIntStateOf(0)
        val photos = mutableListOf<Pair<AttachmentTargetType, String>>()
        rule.setContent {
            val d = display.value
            CompositionLocalProvider(LocalDensity provides Density(context.resources.displayMetrics.widthPixels / d.width.toFloat(), d.font)) {
                OnlyFieldTheme(d.dark) { Surface {
                    key(generation) { PortQuickDialog(project.value, sw.ports.first().id, i18n,
                        PortQuickActions({ updated, _ -> project.value = updated }, photo = { type, id -> photos += type to id }), {}) }
                } }
            }
        }
        displays.forEach { d ->
            rule.runOnIdle { display.value = d; project.value = original; photos.clear(); generation++ }
            snapshot("occupied-${d.name}")
            scrollAction("quick.photoPort").performClick()
            scrollAction("quick.photoCable").performClick()
            assertEquals(listOf(AttachmentTargetType.PORT to sw.ports.first().id, AttachmentTargetType.CABLE to original.cables.single().id), photos)
            rule.onNodeWithText(i18n.text("config.traceTitle")).performScrollTo().assertIsDisplayed()
            rule.onNode(hasText("SW-PEER") and hasText("P1")).performScrollTo().assertIsDisplayed()
            snapshot("path-${d.name}")
            scrollAction("quick.disconnect").performClick()
            fixedAction("text.18c9d912a210").performClick()
            assertEquals(original, project.value)
            scrollAction("quick.disconnect").performClick()
            rule.onAllNodesWithText(i18n.text("quick.disconnect")).onLast().assertIsDisplayed().performClick()
            rule.runOnIdle { assertTrue(project.value.cables.isEmpty()) }
            scrollAction("quick.connectTo").performClick()
            rule.onNodeWithText("SW-PEER").performScrollTo().performClick()
            rule.onNodeWithContentDescription("P48: Libera").performScrollTo().performClick()
            rule.onNode(hasText(i18n.text("quick.continueNext")) and isToggleable())
                .performScrollTo().assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            snapshot("connect-${d.name}")
            fixedAction("quick.connect").performClick()
            rule.runOnIdle {
                assertEquals(sw.ports.first().id, project.value.cables.single().portAId)
                assertEquals(peer.ports.last().id, project.value.cables.single().portBId)
            }
        }
    }

    @Test fun modelSearchPreservesDraftUntilSelectionAcrossDisplayMatrix() {
        val models = (1..12).map { DeviceModel(name = "Modello $it", category = DeviceCategory.NETWORK_SWITCH) }
        val project = Project(name = "Isolated model matrix", createdEpochMs = 1, updatedEpochMs = 1,
            sites = listOf(Site(name = "Matrix site")), deviceModels = models)
        val original = inventoryDeviceDraft(project, null)
        val draft = mutableStateOf(original)
        val display = mutableStateOf(displays.first())
        var generation by mutableIntStateOf(0)
        rule.setContent {
            val d = display.value
            CompositionLocalProvider(LocalDensity provides Density(context.resources.displayMetrics.widthPixels / d.width.toFloat(), d.font)) {
                OnlyFieldTheme(d.dark) { Surface {
                    key(generation) { Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState())) {
                        ObjectConfigurator(project, draft.value, i18n) { draft.value = it }
                    } }
                } }
            }
        }
        displays.forEach { d ->
            rule.runOnIdle { display.value = d; draft.value = original; generation++ }
            rule.onNode(hasContentDescription("Modello:", substring = true)).performScrollTo().performClick()
            rule.onNode(hasSetTextAction() and hasText("Cerca")).assertIsDisplayed().performTextInput("12")
            closeSoftKeyboard()
            rule.onNodeWithText("Modello 1").assertDoesNotExist()
            rule.onNodeWithText("Modello 12").assertIsDisplayed()
            rule.runOnIdle { assertEquals(original, draft.value) }
            snapshot("model-search-${d.name}", root = isPopup())
            rule.onNodeWithText("Modello 12").performClick()
            rule.runOnIdle { assertEquals(models.last().id, draft.value.device.deviceModelId) }
        }
    }

    @Test fun connectedGroupReductionRequiresApprovalAcrossDisplayMatrix() {
        val group = PortTemplate("P", portCount = 2, connector = "RJ45", speed = "1G")
        val sw = Device(technicalName = "SW-MATRIX", hardware = HardwareSpec(portGroups = listOf(group)))
            .let { it.copy(ports = HardwareConfigurator.ports(listOf(group), it.id)) }
        val peer = Device(technicalName = "SW-PEER").let { it.copy(ports = listOf(Port(name = "Uplink", deviceId = it.id))) }
        val initial = Project(name = "Isolated connected group matrix", createdEpochMs = 1, updatedEpochMs = 1,
            sites = listOf(Site(name = "Matrix site", devices = listOf(sw, peer))))
        val project = HardwareConfigurator.connect(initial, sw.ports.last().id, peer.ports.single().id, CableMedium.ETHERNET_COPPER)
        val original = MapObjectDraft.forDevice(project, sw)
        val draft = mutableStateOf(original)
        val display = mutableStateOf(displays.first())
        var generation by mutableIntStateOf(0)
        rule.setContent {
            val d = display.value
            CompositionLocalProvider(LocalDensity provides Density(context.resources.displayMetrics.widthPixels / d.width.toFloat(), d.font)) {
                OnlyFieldTheme(d.dark) { Surface {
                    key(generation) { Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState())) {
                        ObjectConfigurator(project, draft.value, i18n, initialSection = ConfiguratorPage.PORTS) { draft.value = it }
                    } }
                } }
            }
        }
        displays.forEach { d ->
            rule.runOnIdle { display.value = d; draft.value = original; generation++ }
            rule.onNode(hasText("2 × RJ45", substring = true)).performScrollTo().performClick()
            rule.onNode(hasSetTextAction() and hasText("Numero porte")).performScrollTo().performTextReplacement("1")
            closeSoftKeyboard()
            rule.runOnIdle { assertTrue(draft.value.errors(project).containsKey("ports")) }
            val approval = rule.onNode(hasText(i18n.text("config.removeConnected")) and isToggleable())
            approval.performScrollTo().assertIsDisplayed().assertIsOff()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            snapshot("group-approval-${d.name}", root = isRoot())
            approval.performClick()
            rule.runOnIdle { assertTrue(draft.value.allowConnectedRemoval) }
            rule.onNodeWithText("Porte", useUnmergedTree = true).performScrollTo().performClick()
            rule.runOnIdle {
                assertTrue(draft.value.errors(project).isEmpty())
                val saved = draft.value.apply(project)
                assertEquals(sw.ports.first().id, saved.sites.single().devices.first().ports.single().id)
                assertEquals(project.cables.single().id, saved.cables.single().id)
                assertNull(saved.cables.single().portAId)
                assertEquals(peer, saved.sites.single().devices.last())
            }
            snapshot("group-reduced-${d.name}", root = isRoot())
        }
    }

    private fun click(key: String) = rule.onNodeWithText(i18n.text(key)).performScrollTo().performClick()
    private fun scrollAction(key: String): SemanticsNodeInteraction = rule.onNodeWithText(i18n.text(key)).performScrollTo().assertIsDisplayed()
    private fun fixedAction(key: String): SemanticsNodeInteraction = rule.onNodeWithText(i18n.text(key)).assertIsDisplayed()
    private fun snapshot(name: String, root: SemanticsMatcher = isDialog()) {
        if (InstrumentationRegistry.getArguments().getString("matrixEvidence") != "true") return
        rule.waitForIdle()
        val bitmap = rule.onAllNodes(root).onLast().captureToImage().asAndroidBitmap()
        try { File(output, "$name.png").outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) } }
        finally { bitmap.recycle() }
        val roots = rule.onAllNodes(isRoot(), useUnmergedTree = true)
        val tree = roots.fetchSemanticsNodes().indices.joinToString("\n") { roots[it].printToString(maxDepth = 100) }
        File(output, "$name.txt").writeText("API ${android.os.Build.VERSION.SDK_INT}; ${android.os.Build.MODEL}; $name\n" +
            "LocalDensity matrix; isolated Compose host and in-memory project; system settings unchanged.\n" +
            tree)
    }
}
