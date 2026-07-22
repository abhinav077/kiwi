package com.abhinavsirohi.kiwi.data.local

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KiwiSettingsStoreTest {
    @Test
    fun validPin_acceptsFourToEightDigits() {
        assertTrue(isValidPin("1234".toCharArray()))
        assertTrue(isValidPin("12345678".toCharArray()))
    }

    @Test
    fun validPin_rejectsWrongLengthOrNonDigits() {
        assertFalse(isValidPin("123".toCharArray()))
        assertFalse(isValidPin("123456789".toCharArray()))
        assertFalse(isValidPin("12a4".toCharArray()))
    }
}
