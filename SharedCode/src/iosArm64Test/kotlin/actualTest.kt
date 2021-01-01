package com.stnamco.shared

import kotlin.test.Test
import kotlin.test.assertEquals

open class ActualTest {

    @Test
    fun testPlatformName() {
        assertEquals("iOS", Platform().platformName())
    }
}