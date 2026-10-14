package com.henriquesebastiao.downtify.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppFontTest {
    @Test
    fun savedNamesComeBack() {
        AppFont.entries.forEach { assertEquals(it, AppFont.of(it.name)) }
    }

    @Test
    fun anUnknownOrMissingNameFallsBackToDowntifysOwnFont() {
        assertEquals(AppFont.Downtify, AppFont.of(null))
        assertEquals(AppFont.Downtify, AppFont.of("Comic Sans"))
        assertEquals(AppFont.Downtify, AppFont.Default)
    }

    @Test
    fun offersDowntifysSystemAndGoogleSansFlex() {
        assertEquals(listOf("Downtify", "System", "GoogleSansFlex"), AppFont.entries.map { it.name })
    }
}
