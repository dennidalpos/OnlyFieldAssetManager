package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.DeviceModel
import kotlinx.serialization.json.Json

object DeviceModelSerializer {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Serializes a model definition without instance data. */
    fun serializeModel(model: DeviceModel): String {
        return json.encodeToString(model)
    }

    /** Serializes model definitions. */
    fun serializeModels(models: List<DeviceModel>): String {
        return json.encodeToString(models)
    }

    /** Deserializes one model. */
    fun deserializeModel(jsonString: String): DeviceModel {
        return json.decodeFromString<DeviceModel>(jsonString)
    }

    /** Deserializes models. */
    fun deserializeModels(jsonString: String): List<DeviceModel> {
        return json.decodeFromString<List<DeviceModel>>(jsonString)
    }
}
