package com.onlyfield.assetmanager.pc

import java.io.File

/**
 * Resolves where the desktop editor keeps its data.
 *
 * When launched from the packaged executable (jpackage sets `jpackage.app-path`), data lives in a
 * `data` folder next to the .exe so the whole program folder can be copied or carried on a USB drive.
 * If that folder cannot be written (e.g. a read-only location), or when running from Gradle,
 * the user profile folder is used instead.
 */
object PortablePaths {

    const val DATA_FOLDER_NAME = "data"
    private const val USER_FOLDER_NAME = ".onlyfield_asset_manager"

    fun userProfileDataDir(): File = File(System.getProperty("user.home"), USER_FOLDER_NAME)

    /** Folder that contains the launcher .exe, or null when not running from a packaged app. */
    fun appInstallDir(appPath: String? = System.getProperty("jpackage.app-path")): File? =
        appPath?.takeIf { it.isNotBlank() }?.let { File(it).absoluteFile.parentFile }

    fun resolveDataDir(appPath: String? = System.getProperty("jpackage.app-path")): File {
        val installDir = appInstallDir(appPath) ?: return userProfileDataDir()
        val portableDir = File(installDir, DATA_FOLDER_NAME)
        return if (isWritableDir(portableDir)) portableDir else userProfileDataDir()
    }

    private fun isWritableDir(dir: File): Boolean = try {
        (dir.isDirectory || dir.mkdirs()) && File.createTempFile(".write_test", null, dir).delete()
    } catch (_: Exception) {
        false
    }
}
