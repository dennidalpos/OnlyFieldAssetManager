package com.onlyfield.assetmanager.pc

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.onlyfield.assetmanager.core.model.Project
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.rules.TemporaryFolder
import java.awt.EventQueue
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalTestApi::class)
@RunWith(Parameterized::class)
class BusyDialogTest(private val failWorker: Boolean) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "failure={0}")
        fun outcomes() = listOf(arrayOf(false), arrayOf(true))
    }

    @get:Rule val compose = createComposeRule()
    @get:Rule val folder = TemporaryFolder()

    @Test fun workerBlocksTheOpenFormAndKeepsItsDraftAfterSuccessOrFailure() {
        val state = DesktopAppState(DesktopStorageManager(folder.newFolder()))
        val release = CountDownLatch(1)
        val finished = CountDownLatch(1)
        val failure = AtomicReference<Throwable>()
        var launched = false
        try {
            val project = Project(name = "Busy form", createdEpochMs = 0, updatedEpochMs = 0)
            state.importFile(state.storage.saveProjectLocally(project), compare = false)
            compose.setContent { DesktopApp(state) }
            compose.onNodeWithText("Aggiungi sede").performClick()
            compose.onNode(hasSetTextAction()).performTextInput("Retained draft")
            launched = true
            EventQueue.invokeLater {
                try {
                    state.runIo {
                        check(release.await(10, TimeUnit.SECONDS)) { "Worker was not released" }
                        if (failWorker) throw IOException("Synthetic save failure")
                    }
                } catch (e: Exception) { failure.set(e) }
                finally { finished.countDown() }
            }
            val message = state.i18n.text("work.busy")
            compose.waitUntil(5000) { compose.onAllNodesWithText(message).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(hasText(message) and hasAnyAncestor(isDialog())).assertIsDisplayed()
            val busy = compose.onNode(isDialog() and hasAnyDescendant(hasText(message)))
            busy.performKeyInput { pressKey(Key.Escape); pressKey(Key.Tab); pressKey(Key.Enter) }
            busy.performTouchInput { click(Offset(1f, 1f)) }
            compose.onNodeWithText(message).assertIsDisplayed()
            release.countDown()
            assertTrue(finished.await(5, TimeUnit.SECONDS))
            if (failWorker) assertTrue(failure.get() is IOException) else assertNull(failure.get())
            compose.onNodeWithText(message).assertDoesNotExist()
            compose.onNode(hasSetTextAction() and hasText("Retained draft")).assertIsDisplayed()
            compose.onNodeWithText("Salva").performClick()
            compose.runOnIdle { assertEquals("Retained draft", state.project!!.sites.single().name) }
        } finally {
            release.countDown()
            if (launched) check(finished.await(5, TimeUnit.SECONDS)) { "Worker did not finish" }
            state.shutdown()
        }
    }
}
