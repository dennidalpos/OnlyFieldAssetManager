package com.onlyfield.assetmanager.pc

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.graphics.asSkiaBitmap
import com.onlyfield.assetmanager.configurator.map.MapSelection
import com.onlyfield.assetmanager.configurator.theme.BrandMark
import com.onlyfield.assetmanager.configurator.theme.OnlyFieldTheme
import com.onlyfield.assetmanager.core.model.ObjectRef
import com.onlyfield.assetmanager.core.model.PlacementTargetType
import org.jetbrains.skia.Image
import org.junit.Test
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalTestApi::class)
class SocialMediaAssetCaptureTest {

    private fun findRepoRoot(): File {
        var current = File(".").canonicalFile
        while (current != null) {
            if (File(current, "settings.gradle.kts").exists()) return current
            current = current.parentFile
        }
        error("Repo root with settings.gradle.kts not found!")
    }

    @Test
    fun captureCoverSlide() = runDesktopComposeUiTest(1360, 860) {
        val root = findRepoRoot()
        val outputDir = File(root, "assets/social").apply { mkdirs() }

        setContent {
            OnlyFieldTheme(dark = true) {
                CoverSlideContent()
            }
        }
        runOnIdle { }

        val file = File(outputDir, "cover_slide_01.png")
        val rootNode = onAllNodes(isRoot()).onLast()
        Image.makeFromBitmap(rootNode.captureToImage().asSkiaBitmap()).use { image ->
            val data = image.encodeToData() ?: error("Could not encode cover image")
            file.writeBytes(data.bytes)
        }
        println("Saved cover slide: ${file.absolutePath} (${file.length()} bytes)")
    }

    @Test
    fun captureAllKeyFeaturesScreenshots() = runDesktopComposeUiTest(1360, 860) {
        val root = findRepoRoot()
        val demoFile = File(root, "fixtures/demo/onlyfield-demo.ofam")
        check(demoFile.exists()) { "Demo file fixtures/demo/onlyfield-demo.ofam not found!" }

        val outputDir = File(root, "assets/social").apply { mkdirs() }
        val tempDir = Files.createTempDirectory("ofam-social-capture").toFile()
        val storage = DesktopStorageManager(tempDir)
        val state = DesktopAppState(storage)

        fun saveScreenshot(filename: String) {
            val file = File(outputDir, filename)
            val rootNode = onAllNodes(isRoot()).onLast()
            Image.makeFromBitmap(rootNode.captureToImage().asSkiaBitmap()).use { image ->
                val data = image.encodeToData() ?: error("Could not encode image data")
                file.writeBytes(data.bytes)
            }
            println("Saved screenshot: ${file.absolutePath} (${file.length()} bytes)")
        }

        try {
            // Import demo package with 366 devices, 8 racks, 1007 cables, floor plans
            state.importFile(demoFile, password = null, compare = false)
            setContent {
                DesktopApp(state)
            }

            // ----------------------------------------------------
            // 1. Planimetria e Mappe Interattive (Floor Map)
            // ----------------------------------------------------
            runOnIdle {
                state.dismissDialog()
                state.section = AppSection.FLOORPLANS
                val comSite = state.project?.sites?.firstOrNull { it.code == "COM" }
                    ?: state.project?.sites?.firstOrNull()
                if (comSite != null) {
                    state.selectedSiteId = comSite.id
                    val floor = comSite.areas.firstOrNull { it.name.contains("Laboratorio") }
                        ?: comSite.areas.firstOrNull { it.name.contains("Piano terra") }
                        ?: comSite.areas.firstOrNull()
                    if (floor != null) {
                        state.selectedAreaId = floor.id
                        val dev = comSite.devices.firstOrNull { it.areaId == floor.id }
                        if (dev != null) {
                            val mapState = state.mapUiState.floor(state.project!!, floor.id)
                            mapState.selection = MapSelection.Node(ObjectRef(PlacementTargetType.DEVICE, dev.id))
                        }
                    }
                }
            }
            runOnIdle { }
            saveScreenshot("live_01_floor_map.png")

            // ----------------------------------------------------
            // 2. Armadi Rack 19" e Layout Unità (Rack Elevation)
            // ----------------------------------------------------
            runOnIdle {
                state.dismissDialog()
                state.section = AppSection.RACKS
            }
            runOnIdle { }
            saveScreenshot("live_02_rack_elevation.png")

            // ----------------------------------------------------
            // 3. Scheda Apparato, Porte & Alimentazione PoE
            // ----------------------------------------------------
            runOnIdle {
                state.dismissDialog()
                state.section = AppSection.INVENTORY
            }
            runOnIdle { }
            onNodeWithText("Cerca nome, etichetta, IP, MAC, seriale…").performTextInput("SW-COM-LAB")
            runOnIdle { }
            val portNodes = onAllNodesWithText("Porte")
            if (portNodes.fetchSemanticsNodes().isNotEmpty()) {
                portNodes.onFirst().performClick()
                runOnIdle { }
            }
            saveScreenshot("live_03_device_ports.png")

            // ----------------------------------------------------
            // 4. Cablaggio Strutturato, Dorsali e Permutazioni
            // ----------------------------------------------------
            runOnIdle {
                state.dismissDialog()
                state.section = AppSection.CABLING
            }
            runOnIdle { }
            saveScreenshot("live_04_cabling_routes.png")

            // ----------------------------------------------------
            // 5. Gestione Progetto, Integrità & Scambio Sicuro / Documenti
            // ----------------------------------------------------
            runOnIdle {
                state.dismissDialog()
                state.section = AppSection.OVERVIEW
                state.dialog = AppDialog.Documents
            }
            runOnIdle { }
            saveScreenshot("live_05_project_exchange.png")

        } finally {
            state.shutdown()
            tempDir.deleteRecursively()
        }
    }
}

@Composable
private fun CoverSlideContent() {
    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F1E2A),
            Color(0xFF09141D)
        )
    )
    val cardBorder = Color(0xFF28546D)
    val cardBg = Color(0xFF132838)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .padding(48.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Brand & App Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BrandMark(size = 54.dp)
                    Column {
                        Text(
                            text = "OnlyField Asset Manager",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF4FAFF),
                            fontSize = 26.sp
                        )
                        Text(
                            text = "RILIEVI E DOCUMENTAZIONE RETI AZIENDALI SUL CAMPO",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF55D6C7),
                            letterSpacing = 1.5.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF1C3A4B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF387D97))
                ) {
                    Text(
                        text = "100% OFFLINE-FIRST",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF55D6C7)
                    )
                }
            }

            // Hero section: Catchy practical explanation
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tutta la tua rete aziendale in tasca e su PC.\nSenza cloud, senza abbonamenti, senza sorprese.",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = 38.sp,
                    lineHeight = 46.sp
                )
                Text(
                    text = "L'assistente digitale per tecnici e installatori: mappa apparati, armadi rack 19'', tratte di cablaggio e planimetrie degli edifici direttamente durante il sopralluogo.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFBDD8E8),
                    fontSize = 18.sp,
                    lineHeight = 26.sp
                )
            }

            // 3 Key Pillar Cards (Practical & Non-technical)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Pillar 1
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, cardBorder, RoundedCornerShape(16.dp)),
                    color = cardBg
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "📍 MAPPE E RACK", style = MaterialTheme.typography.labelLarge, color = Color(0xFF55D6C7), fontWeight = FontWeight.Bold)
                        Text(text = "Planimetrie e Montanti", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            text = "Posiziona switch, access point e prese a muro direttamente sulla pianta reale dell'edificio e visualizza l'armadio rack U per U a colpo d'occhio.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFBDD8E8),
                            lineHeight = 22.sp
                        )
                    }
                }

                // Pillar 2
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, cardBorder, RoundedCornerShape(16.dp)),
                    color = cardBg
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "⚡ CABLAGGIO E PORTE", style = MaterialTheme.typography.labelLarge, color = Color(0xFF55D6C7), fontWeight = FontWeight.Bold)
                        Text(text = "Cavi, Tratte e PoE", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            text = "Traccia ogni tratta dal patch panel allo switch. Controlla assorbimenti PoE, porte libere e dorsali in fibra senza rischiare di staccare il cavo errato.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFBDD8E8),
                            lineHeight = 22.sp
                        )
                    }
                }

                // Pillar 3
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, cardBorder, RoundedCornerShape(16.dp)),
                    color = cardBg
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "🔒 ZERO CLOUD", style = MaterialTheme.typography.labelLarge, color = Color(0xFF55D6C7), fontWeight = FontWeight.Bold)
                        Text(text = "Dati Tuoi e Riservati", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            text = "Rilievi veloci su smartphone Android, regia su PC Windows. Scambio progetti con file .ofam protetti da crittografia solida, senza server esterni.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFBDD8E8),
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Bottom Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Disponibile per Windows 11 x64 (Portable) & Android 14+",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF86A8BC),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Scorri per scoprire le 5 funzioni chiave dal vivo 👉",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF55D6C7),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
