package com.pocketpresenter.ui

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for PocketModeManager device capability evaluation and limitation messaging.
 * Meets docs/app_flow.md Screen H & docs/implementation_plan.md P7-07, P7-08.
 */
class PocketModeManagerTest {

    @Test
    fun evaluateDeviceCapability_returnsNonNullCapability() {
        val capability = PocketModeManager.evaluateDeviceCapability()
        assertNotNull(capability)
    }

    @Test
    fun limitationExplanation_coversAllCapabilities() {
        val validatedMsg = PocketModeManager.getLimitationExplanation(DevicePocketModeCapability.VALIDATED)
        assertTrue(validatedMsg.contains("verified") || validatedMsg.contains("slides"))

        val notValidatedMsg = PocketModeManager.getLimitationExplanation(DevicePocketModeCapability.NOT_VALIDATED)
        assertTrue(notValidatedMsg.contains("Volume buttons may not work with the screen off"))

        val unsupportedMsg = PocketModeManager.getLimitationExplanation(DevicePocketModeCapability.UNSUPPORTED)
        assertTrue(unsupportedMsg.contains("does not deliver"))
    }
}
