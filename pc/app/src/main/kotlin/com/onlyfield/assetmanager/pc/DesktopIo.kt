package com.onlyfield.assetmanager.pc

import java.awt.EventQueue
import java.awt.Toolkit
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

/** Keeps synchronous edit results while AWT continues dispatching paint events. */
internal class DesktopIo(private val busyChanged: (Boolean) -> Unit) : AutoCloseable {
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "ofam-desktop-io").apply { isDaemon = true } }

    fun <T> run(action: () -> T): T {
        if (!EventQueue.isDispatchThread()) return action()
        val loop = Toolkit.getDefaultToolkit().systemEventQueue.createSecondaryLoop()
        busyChanged(true)
        try {
            val result = worker.submit(Callable {
                try { action() } finally { EventQueue.invokeLater { loop.exit() } }
            })
            check(loop.enter()) { "Cannot dispatch UI events while saving" }
            return try { result.get() } catch (e: ExecutionException) { throw (e.cause ?: e) }
        } finally { busyChanged(false) }
    }

    override fun close() { worker.shutdown() }
}
