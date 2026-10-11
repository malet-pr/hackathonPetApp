package com.example.petcare.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SettingsStoreTest {
    @Test fun whitespaceDoesNotCountTowardTitleLimit() {
        assertEquals("x".repeat(60), SettingsStore.validateTitle("  " + "x".repeat(60) + "  "))
    }
    @Test fun blankAndOversizeTitlesCannotReachStorage() {
        assertThrows(IllegalArgumentException::class.java) { SettingsStore.validateTitle(" \n\t ") }
        assertThrows(IllegalArgumentException::class.java) { SettingsStore.validateTitle("x".repeat(61)) }
    }
}
