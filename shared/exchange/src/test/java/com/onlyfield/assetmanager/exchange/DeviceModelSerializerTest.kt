package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.DeviceCategory
import com.onlyfield.assetmanager.core.model.DeviceModel
import com.onlyfield.assetmanager.core.model.PortSide
import com.onlyfield.assetmanager.core.model.PortTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DeviceModelSerializerTest {

    @Test
    fun testSerializeAndDeserializeDeviceModel() {
        val model = DeviceModel(
            name = "Switch 24 Ports M3",
            brand = "Cisco",
            modelNumber = "C9200-24T",
            category = DeviceCategory.NETWORK_SWITCH,
            defaultHeightU = 1,
            portTemplates = listOf(
                PortTemplate(
                    namePrefix = "Gi1/0/",
                    startNumber = 1,
                    portCount = 24,
                    side = PortSide.FRONT,
                    mediaType = "RJ45"
                ),
                PortTemplate(
                    namePrefix = "Te1/1/",
                    startNumber = 1,
                    portCount = 4,
                    side = PortSide.FRONT,
                    mediaType = "SFP+"
                )
            ),
            notes = "Standard access switch model"
        )

        val jsonString = DeviceModelSerializer.serializeModel(model)
        assertNotNull(jsonString)

        val deserialized = DeviceModelSerializer.deserializeModel(jsonString)
        assertEquals(model.id, deserialized.id)
        assertEquals(model.name, deserialized.name)
        assertEquals(model.brand, deserialized.brand)
        assertEquals(model.modelNumber, deserialized.modelNumber)
        assertEquals(model.category, deserialized.category)
        assertEquals(model.defaultHeightU, deserialized.defaultHeightU)
        assertEquals(2, deserialized.portTemplates.size)
        assertEquals(24, deserialized.portTemplates[0].portCount)
    }
}
