package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.i18n.Messages

import java.awt.print.PrinterJob
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Helper class for Windows Desktop storage, directory picking, file picking, and printing integration.
 */
object DesktopStorageHelper {

    fun pickDirectory(
        title: String = Messages().text("text.79762e9e52e9"),
        currentDir: File? = null,
        i18n: Messages = Messages()): File? {
        val chooser = JFileChooser().apply {
            dialogTitle = title
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            if (currentDir != null && currentDir.exists()) {
                currentDirectory = currentDir
            }
        }
        val result = chooser.showOpenDialog(null)
        return if (result == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
    }

    fun pickOpenFile(
        title: String = Messages().text("text.b7ff83aeac87"),
        extensionDescription: String = Messages().text("text.43ff8b157bf2"),
        vararg extensions: String = arrayOf("ofam"),
        i18n: Messages = Messages()): File? {
        val chooser = JFileChooser().apply {
            dialogTitle = title
            isMultiSelectionEnabled = false
            fileFilter = FileNameExtensionFilter(extensionDescription, *extensions)
        }
        val result = chooser.showOpenDialog(null)
        return if (result == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
    }

    fun pickSaveFile(
        title: String = Messages().text("text.505a2a51b914"),
        defaultFileName: String = "progetto.ofam",
        extensionDescription: String = Messages().text("text.43ff8b157bf2"),
        vararg extensions: String = arrayOf("ofam"),
        i18n: Messages = Messages()): File? {
        val chooser = JFileChooser().apply {
            dialogTitle = title
            selectedFile = File(defaultFileName)
            fileFilter = FileNameExtensionFilter(extensionDescription, *extensions)
        }
        val result = chooser.showSaveDialog(null)
        return if (result == JFileChooser.APPROVE_OPTION) {
            var file = chooser.selectedFile
            val ext = extensions.firstOrNull() ?: ""
            if (ext.isNotEmpty() && !file.name.lowercase().endsWith(".$ext")) {
                file = File(file.parentFile, "${file.name}.$ext")
            }
            file
        } else null
    }

    fun isPrinterAvailable(): Boolean {
        return try {
            val job = PrinterJob.getPrinterJob()
            job.printService != null || PrinterJob.lookupPrintServices().isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }
}
