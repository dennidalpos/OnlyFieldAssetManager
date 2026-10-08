package com.onlyfield.assetmanager.pc

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.awt.Container
import java.awt.EventQueue
import java.awt.KeyboardFocusManager
import java.awt.Window
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JDialog
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JTextField
import javax.swing.Timer

@RunWith(Parameterized::class)
class NativePickerOwnershipTest(private val picker: String) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "picker={0}")
        fun pickers() = listOf(arrayOf("directory"), arrayOf("open"), arrayOf("multiple"), arrayOf("save"))
    }

    @Test fun cancellationKeepsPickerOwnedAndReturnsKeyboardFocus() {
        val initialFocus = CountDownLatch(1)
        val returnedFocus = CountDownLatch(1)
        val focusCount = AtomicInteger()
        val owner = AtomicReference<Window>()
        val dialogBounds = AtomicReference<java.awt.Rectangle>()
        lateinit var frame: JFrame
        lateinit var field: JTextField
        lateinit var timer: Timer
        val title = "Native picker regression: $picker"
        EventQueue.invokeAndWait {
            frame = JFrame(title).apply { setBounds(80, 80, 900, 600) }
            field = JTextField("Retained keyboard focus").apply {
                addFocusListener(object : FocusAdapter() {
                    override fun focusGained(event: FocusEvent) {
                        if (focusCount.incrementAndGet() == 1) initialFocus.countDown()
                        else returnedFocus.countDown()
                    }
                })
            }
            frame.add(field)
            frame.isVisible = true
            field.requestFocusInWindow()
            timer = Timer(100, null).apply {
                addActionListener {
                    fun chooser(container: Container): JFileChooser? =
                        container.components.firstNotNullOfOrNull { child ->
                            (child as? JFileChooser) ?: (child as? Container)?.let { chooser(it) }
                        }
                    Window.getWindows().filterIsInstance<JDialog>()
                        .firstOrNull { it.isShowing && it.title == title }?.let { dialog ->
                            owner.set(dialog.owner)
                            dialogBounds.set(dialog.bounds)
                            stop()
                            checkNotNull(chooser(dialog)).cancelSelection()
                        }
                }
            }
        }
        try {
            assertTrue("Fixture did not receive focus", initialFocus.await(5, TimeUnit.SECONDS))
            EventQueue.invokeAndWait {
                assertSame(frame, KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow)
                timer.start()
                when (picker) {
                    "directory" -> assertNull(DesktopStorageHelper.pickDirectory(title))
                    "open" -> assertNull(DesktopStorageHelper.pickOpenFile(title, "Packages", "ofam"))
                    "multiple" -> assertTrue(DesktopStorageHelper.pickOpenFiles(title, "Images", "png").isEmpty())
                    "save" -> assertNull(DesktopStorageHelper.pickSaveFile(title, "test.ofam", "Packages", "ofam"))
                    else -> error("Unknown picker")
                }
            }
            assertSame("Picker must belong to its calling frame", frame, owner.get())
            assertTrue("Picker must be placed inside its frame", frame.bounds.contains(dialogBounds.get()))
            assertTrue("Keyboard focus did not return", returnedFocus.await(5, TimeUnit.SECONDS))
            EventQueue.invokeAndWait {
                assertSame(field, KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner)
            }
        } finally {
            EventQueue.invokeAndWait {
                timer.stop()
                frame.dispose()
            }
        }
    }
}
