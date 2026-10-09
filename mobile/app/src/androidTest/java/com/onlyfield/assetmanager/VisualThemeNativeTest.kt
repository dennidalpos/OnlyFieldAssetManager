package com.onlyfield.assetmanager

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import com.onlyfield.assetmanager.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class VisualThemeNativeTest {
    @get:Rule val rule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val output by lazy {
        File(checkNotNull(context.getExternalFilesDir(null)), "restyling-evidence").apply { check(mkdirs() || isDirectory) }
    }

    @Test fun projectsAndInventoryRemainReadableAcrossThemesAndWidths() {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repository = ProjectRepository(db)
        val owner = ViewModelStore()
        lateinit var vm: ProjectViewModel
        val floor = Area(name = "Piano terra")
        val project = Project(name = "Campus Nord", description = "Rete e infrastrutture del campus",
            createdEpochMs = 1, updatedEpochMs = 1,
            sites = listOf(Site(name = "Edificio A", areas = listOf(floor), devices = listOf(
                Device(technicalName = "SW-01", alias = "Distribuzione", areaId = floor.id, ipAddress = "192.0.2.10"),
                Device(technicalName = "AP-01", areaId = floor.id, objectTypeId = "access-point", category = DeviceCategory.CUSTOM)))))
        var width by mutableIntStateOf(360)
        var dark by mutableStateOf(false)
        var font by mutableFloatStateOf(1f)
        try {
            rule.runOnIdle { vm = ProjectViewModel(repository); owner.put("visual", vm) }
            rule.setContent {
                val configuration = Configuration(LocalConfiguration.current).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                }
                CompositionLocalProvider(LocalMessages provides vm.i18n, LocalConfiguration provides configuration,
                    LocalDensity provides Density(context.resources.displayMetrics.widthPixels / width.toFloat(), font)) {
                    // Each synthetic density needs a fresh layout host; the ViewModel remains shared.
                    key(width, dark, font) { AppRoot(vm) { error("Unexpected exit") } }
                }
            }
            snapshot("welcome")
            runBlocking { repository.saveProject(project) }
            rule.waitUntil(10_000) { vm.projects.value.isNotEmpty() }
            for (screen in listOf("projects", "inventory")) {
                if (screen == "inventory") {
                    rule.runOnIdle { vm.openProject(project.id) }
                    rule.waitUntil(10_000) { vm.project.value != null && vm.busy == null }
                    rule.runOnIdle { vm.openTab(Screen.Inventory) }
                }
                for (w in listOf(360, 412, 600, 840)) for (d in listOf(false, true)) for (f in listOf(1f, 1.3f)) {
                    rule.runOnIdle { width = w; dark = d; font = f }
                    if (screen == "projects") {
                        rule.onNodeWithText(vm.i18n.text("text.d5555c9b8e0b", project.name)).assertIsDisplayed()
                        rule.onNodeWithText(project.name).assertIsDisplayed()
                    } else {
                        rule.waitUntil(5_000) {
                            val root = rule.onRoot().fetchSemanticsNode().boundsInRoot
                            val navigation = rule.onAllNodes(hasText(vm.i18n.text("ux.nav.devices")) and hasClickAction())
                                .fetchSemanticsNodes().singleOrNull()
                            navigation != null && (navigation.boundsInRoot.top < root.height / 2) == (w >= 600)
                        }
                        rule.onNodeWithText("SW-01 (Distribuzione)").assertIsDisplayed()
                        rule.onNode(hasSetTextAction()).performTextReplacement("missing-device")
                        rule.onNodeWithText(vm.i18n.text("ux.noResults")).assertIsDisplayed()
                        rule.onNodeWithContentDescription(vm.i18n.text("ux.clearSearch")).performClick()
                        rule.onNodeWithText("SW-01 (Distribuzione)").assertIsDisplayed()
                    }
                    snapshot("$screen-$w-$d-$f")
                }
            }
        } finally {
            rule.runOnIdle { owner.clear() }
            db.close()
        }
    }

    @Test fun launcherLayersKeepTheirSilhouetteInsideTheSafeZone() {
        val foreground = render(R.drawable.ic_launcher_foreground, 108)
        val monochrome = render(R.drawable.ic_launcher_monochrome, 108)
        try {
            var opaque = 0
            for (y in 0 until 108) for (x in 0 until 108) {
                val alpha = Color.alpha(foreground.getPixel(x, y))
                // Vector antialiasing rounds edge alpha differently for coloured and black strokes.
                assertTrue("Monochrome silhouette at $x,$y",
                    kotlin.math.abs(alpha - Color.alpha(monochrome.getPixel(x, y))) <= 2)
                if (alpha > 0) {
                    opaque++
                    assertTrue("Outside safe circle at $x,$y", (x + .5 - 54) * (x + .5 - 54) + (y + .5 - 54) * (y + .5 - 54) <= 33 * 33)
                }
            }
            assertTrue(opaque > 500)
            // The open centre must not collapse to a solid pin in themed launchers.
            assertEquals(0, Color.alpha(monochrome.getPixel(54, 38)))
            for (size in listOf(48, 432)) for (round in listOf(false, true)) for (mono in listOf(false, true)) {
                val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                try {
                    val canvas = Canvas(bitmap)
                    val path = Path().apply {
                        if (round) addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW)
                        else addRoundRect(0f, 0f, size.toFloat(), size.toFloat(), size * .22f, size * .22f, Path.Direction.CW)
                    }
                    canvas.clipPath(path)
                    if (mono) canvas.drawColor(Color.rgb(120, 214, 205))
                    else context.getDrawable(R.drawable.ic_launcher_background)!!.apply { setBounds(0, 0, size, size); draw(canvas) }
                    context.getDrawable(if (mono) R.drawable.ic_launcher_monochrome else R.drawable.ic_launcher_foreground)!!.apply {
                        // Launcher masks expose the inner 72 dp of each 108 dp layer.
                        val inset = size / 4
                        setBounds(-inset, -inset, size + inset, size + inset)
                        draw(canvas)
                    }
                    save(bitmap, "icon-$size-$round-$mono")
                } finally { bitmap.recycle() }
            }
        } finally { foreground.recycle(); monochrome.recycle() }
    }

    private fun render(resource: Int, size: Int) = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
        context.getDrawable(resource)!!.apply { setBounds(0, 0, size, size); draw(Canvas(bitmap)) }
    }

    private fun snapshot(name: String) {
        if (InstrumentationRegistry.getArguments().getString("matrixEvidence") != "true") return
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        try { save(bitmap, name) } finally { bitmap.recycle() }
    }

    private fun save(bitmap: Bitmap, name: String) {
        File(output, "$name.png").outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    }
}
