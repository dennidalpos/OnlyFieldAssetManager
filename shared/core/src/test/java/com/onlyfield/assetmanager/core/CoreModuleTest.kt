package com.onlyfield.assetmanager.core

import org.junit.Assert.assertEquals
import org.junit.Test

class CoreModuleTest {
    @Test
    fun testCoreModuleName() {
        assertEquals("shared:core", CoreModule.MODULE_NAME)
    }
}
