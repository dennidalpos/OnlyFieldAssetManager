package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.i18n.AppLanguage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import com.sun.nio.file.ExtendedOpenOption

class SettingsFailureTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun malformedSettingsAreReportedOnceAndNeverOverwritten() {
        val dir = folder.newFolder()
        val file = File(dir, "settings.properties").apply { writeText("theme=dark\nlanguage=\\uBROKEN\n") }
        val bytes = file.readBytes()
        val state = DesktopAppState(DesktopStorageManager(dir))
        try {
            assertNotNull(state.error)
            assertFalse(state.darkTheme)
            state.toggleDarkTheme()
            state.changeLanguage(AppLanguage.ENGLISH)
            assertFalse(state.darkTheme)
            assertEquals(AppLanguage.SYSTEM, state.language)
            assertNotNull(state.error)
            assertArrayEquals(bytes, file.readBytes())
        } finally { state.shutdown() }
    }

    @Test fun lockedSettingsPreserveBytesAndBothPreferencesThenRetrySucceeds() {
        val dir = folder.newFolder()
        val file = File(dir, "settings.properties").apply { writeText("theme=light\nlanguage=it\ncustom=kept\n") }
        val bytes = file.readBytes()
        val state = DesktopAppState(DesktopStorageManager(dir))
        try {
            Files.newByteChannel(file.toPath(), StandardOpenOption.READ, ExtendedOpenOption.NOSHARE_DELETE).use {
                state.toggleDarkTheme()
                assertFalse(state.darkTheme)
                assertNotNull(state.error)
                state.changeLanguage(AppLanguage.ENGLISH)
                assertEquals(AppLanguage.ITALIAN, state.language)
                assertArrayEquals(bytes, file.readBytes())
            }
            state.toggleDarkTheme()
            assertTrue(state.darkTheme)
            assertNull(state.error)
            state.changeLanguage(AppLanguage.ENGLISH)
            assertEquals(AppLanguage.ENGLISH, state.language)
            assertNull(state.error)
            val props = java.util.Properties().apply { file.inputStream().use { load(it) } }
            assertEquals("kept", props.getProperty("custom"))
            assertEquals("dark", props.getProperty("theme"))
            assertEquals("en", props.getProperty("language"))
            assertFalse(dir.walkTopDown().any { it.extension == "tmp" })
        } finally { state.shutdown() }
    }

    @Test fun directoryAtSettingsPathIsAnErrorAndCanBeRetried() {
        val dir = folder.newFolder()
        val file = File(dir, "settings.properties").apply { mkdir() }
        val state = DesktopAppState(DesktopStorageManager(dir))
        try {
            assertNotNull(state.error)
            state.toggleDarkTheme()
            assertFalse(state.darkTheme)
            assertNotNull(state.error)
            assertTrue(file.delete())
            state.toggleDarkTheme()
            assertTrue(state.darkTheme)
            assertNull(state.error)
        } finally { state.shutdown() }
    }
}
