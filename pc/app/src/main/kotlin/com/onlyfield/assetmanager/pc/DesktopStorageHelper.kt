package com.onlyfield.assetmanager.pc

import java.awt.print.PrinterJob
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Helper class for Windows Desktop storage, file picking, and printing integration.
 */
object DesktopStorageHelper {

    fun pickOpenFile(
        title: String = "Apri pacchetto .ofam",
        extensionDescription: String = "Pacchetti OnlyField Asset Manager (*.ofam)",
        vararg extensions: String = arrayOf("ofam")
    ): File? {
        val chooser = JFileChooser().apply {
            dialogTitle = title
            isMultiSelectionEnabled = false
            fileFilter = FileNameExtensionFilter(extensionDescription, *extensions)
        }
        val result = chooser.showOpenDialog(null)
        return if (result == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
    }

    fun pickSaveFile(
        title: String = "Esporta pacchetto .ofam",
        defaultFileName: String = "progetto.ofam",
        extensionDescription: String = "Pacchetti OnlyField Asset Manager (*.ofam)",
        vararg extensions: String = arrayOf("ofam")
    ): File? {
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
