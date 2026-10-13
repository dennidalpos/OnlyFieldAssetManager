package com.onlyfield.assetmanager.core.forms

import com.onlyfield.assetmanager.core.i18n.Messages
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class FieldValidatorsTest {

    private val i18n = Messages(Locale.ITALIAN)

    @Test
    fun requiredValidation() {
        assertNull(FieldValidators.required("Nome", i18n = i18n))
        assertNotNull(FieldValidators.required("", i18n = i18n))
        assertNotNull(FieldValidators.required("   ", "Campo", i18n = i18n))
    }

    @Test
    fun intValidationBoundsAndFormatting() {
        assertNull(FieldValidators.int("", required = false, i18n = i18n))
        assertNotNull(FieldValidators.int("", required = true, i18n = i18n))
        assertNull(FieldValidators.int("42", min = 1, max = 100, i18n = i18n))
        assertNotNull(FieldValidators.int("abc", i18n = i18n))
        assertNotNull(FieldValidators.int("0", min = 1, i18n = i18n))
        assertNotNull(FieldValidators.int("101", max = 100, i18n = i18n))
        assertEquals(42, FieldValidators.parseInt(" 42 "))
        assertNull(FieldValidators.parseInt("invalid"))
    }

    @Test
    fun decimalValidationCommasAndMinMax() {
        assertNull(FieldValidators.decimal("", i18n = i18n))
        assertNull(FieldValidators.decimal("12,5", min = 0.0, max = 20.0, i18n = i18n))
        assertNull(FieldValidators.decimal(" 15.7 ", min = 10.0, max = 20.0, i18n = i18n))
        assertNotNull(FieldValidators.decimal("not_a_number", i18n = i18n))
        assertNotNull(FieldValidators.decimal("-5.0", min = 0.0, i18n = i18n))
        assertNotNull(FieldValidators.decimal("25.0", max = 20.0, i18n = i18n))
        assertEquals(12.5, FieldValidators.parseDecimal("12,5")!!, 0.001)
        assertNull(FieldValidators.parseDecimal("Infinity"))
        assertNull(FieldValidators.parseDecimal("NaN"))
    }

    @Test
    fun ipv4Validation() {
        assertNull(FieldValidators.ipv4("", i18n = i18n))
        assertNull(FieldValidators.ipv4("192.168.1.1", i18n = i18n))
        assertNull(FieldValidators.ipv4(" 10.0.0.255 ", i18n = i18n))
        assertNotNull(FieldValidators.ipv4("256.0.0.1", i18n = i18n))
        assertNotNull(FieldValidators.ipv4("192.168.1", i18n = i18n))
        assertNotNull(FieldValidators.ipv4("192.168.1.1.1", i18n = i18n))
        assertNotNull(FieldValidators.ipv4("192.168.1.a", i18n = i18n))
    }

    @Test
    fun cidrValidation() {
        assertNull(FieldValidators.cidr("", required = false, i18n = i18n))
        assertNotNull(FieldValidators.cidr("", required = true, i18n = i18n))
        assertNull(FieldValidators.cidr("192.168.1.0/24", i18n = i18n))
        assertNull(FieldValidators.cidr("10.0.0.0/0", i18n = i18n))
        assertNull(FieldValidators.cidr("172.16.0.0/32", i18n = i18n))
        assertNotNull(FieldValidators.cidr("192.168.1.0/33", i18n = i18n))
        assertNotNull(FieldValidators.cidr("192.168.1.0/-1", i18n = i18n))
        assertNotNull(FieldValidators.cidr("192.168.1.0", i18n = i18n))
        assertNotNull(FieldValidators.cidr("256.0.0.0/24", i18n = i18n))
    }

    @Test
    fun macValidationFormats() {
        assertNull(FieldValidators.mac("", i18n = i18n))
        assertNull(FieldValidators.mac("AA:BB:CC:DD:EE:FF", i18n = i18n))
        assertNull(FieldValidators.mac("aa-bb-cc-dd-ee-ff", i18n = i18n))
        assertNull(FieldValidators.mac("aabb.ccdd.eeff", i18n = i18n))
        assertNotNull(FieldValidators.mac("AA:BB:CC:DD:EE", i18n = i18n))
        assertNotNull(FieldValidators.mac("AABBCC-DDEEFF", i18n = i18n))
        assertNotNull(FieldValidators.mac("GG:HH:II:JJ:KK:LL", i18n = i18n))
    }
}
