package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.DeviceModel
import kotlinx.serialization.json.Json

object DeviceModelSerializer {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Serializes a single [DeviceModel] to JSON.
     * Ensures only model definitions (templates, height, category, brand) are exported,
     * with no instance credentials, IPs, MACs or cables.
     */
    fun serializeModel(model: DeviceModel): String {
        return json.encodeToString(model)
    }

    /**
     * Serializes a list of [DeviceModel]s to JSON.
     */
    fun serializeModels(models: List<DeviceModel>): String {
        return json.encodeToString(models)
    }

    /**
     * Deserializes a single [DeviceModel] from JSON.
     */
    fun deserializeModel(jsonString: String): DeviceModel {
        return json.decodeFromString<DeviceModel>(jsonString)
    }

    /**
     * Deserializes a list of [DeviceModel]s from JSON.
     */
    fun deserializeModels(jsonString: String): List<DeviceModel> {
        return json.decodeFromString<List<DeviceModel>>(jsonString)
    }
}
