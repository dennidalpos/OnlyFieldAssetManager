package com.onlyfield.assetmanager.pc

import java.io.File

/** Resolves portable data beside the executable, with a profile fallback. */
object PortablePaths {

    const val DATA_FOLDER_NAME = "data"
    private const val USER_FOLDER_NAME = ".onlyfield_asset_manager"

    fun userProfileDataDir(): File = File(System.getProperty("user.home"), USER_FOLDER_NAME)

    /** Launcher folder, when packaged. */
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
