package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.CoreModule

object ExchangeModule {
    const val MODULE_NAME: String = "shared:exchange"

    fun getDependenciesInfo(): String {
        return "Depends on " + CoreModule.MODULE_NAME
    }
}
