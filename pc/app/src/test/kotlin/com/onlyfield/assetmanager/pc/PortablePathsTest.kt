package com.onlyfield.assetmanager.pc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class PortablePathsTest {

    @Test
    fun usesUserProfileWhenNotPackaged() {
        assertEquals(PortablePaths.userProfileDataDir(), PortablePaths.resolveDataDir(appPath = null))
    }

    @Test
    fun usesDataFolderNextToExecutableWhenPackaged() {
        val installDir = Files.createTempDirectory("ofam_portable").toFile()
        val exe = File(installDir, "OnlyFieldAssetManager.exe")

        val dataDir = PortablePaths.resolveDataDir(appPath = exe.absolutePath)

        assertEquals(File(installDir, PortablePaths.DATA_FOLDER_NAME).absoluteFile, dataDir)
        assertTrue(dataDir.isDirectory)
        installDir.deleteRecursively()
    }
}
