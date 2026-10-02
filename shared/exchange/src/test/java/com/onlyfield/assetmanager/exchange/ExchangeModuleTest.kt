package com.onlyfield.assetmanager.exchange

import org.junit.Assert.assertEquals
import org.junit.Test

class ExchangeModuleTest {
    @Test
    fun testExchangeModuleDependencies() {
        assertEquals("Depends on shared:core", ExchangeModule.getDependenciesInfo())
    }
}
