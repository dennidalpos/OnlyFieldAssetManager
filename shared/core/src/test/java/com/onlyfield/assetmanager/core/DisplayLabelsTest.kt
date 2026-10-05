package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.display.EntityTypeLabels
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.validation.ValidationSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayLabelsTest {

    private fun <E : Enum<E>> assertReadable(values: Array<E>, label: (E) -> String) {
        val labels = values.map(label)
        labels.zip(values).forEach { (text, value) ->
            assertTrue("Empty label for $value", text.isNotBlank())
            if (value.name.contains('_')) assertNotEquals("Raw enum name shown for $value", value.name, text)
        }
        assertEquals("Duplicate labels in ${values.first()::class.simpleName}", labels.size, labels.toSet().size)
    }

    @Test
    fun everyEnumHasDistinctHumanLabels() {
        assertReadable(DeviceCategory.values()) { it.toDisplayString() }
        assertReadable(CableMedium.values()) { it.toDisplayString() }
        assertReadable(PowerFeedType.values()) { it.toDisplayString() }
        assertReadable(BadgeCategory.values()) { it.toDisplayString() }
        assertReadable(PoeStandard.values()) { it.toDisplayString() }
        assertReadable(PoeRole.values()) { it.toDisplayString() }
        assertReadable(VlanScopeType.values()) { it.toDisplayString() }
        assertReadable(AttachmentClassification.values()) { it.toDisplayString() }
        assertReadable(NumberingDirection.values()) { it.toDisplayString() }
        assertReadable(MountingType.values()) { it.toDisplayString() }
        assertReadable(ValidationSeverity.values()) { it.toDisplayString() }
        assertReadable(OperationalStatus.values()) { it.toDisplayString() }
    }

    @Test
    fun entityTypeLabelsAreTranslated() {
        assertEquals("Apparato", EntityTypeLabels.of("DEVICE"))
        assertEquals("Porta", EntityTypeLabels.of("port"))
        assertEquals("Sconosciuto", EntityTypeLabels.of("SCONOSCIUTO"))
    }
}
