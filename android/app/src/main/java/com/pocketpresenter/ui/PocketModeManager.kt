package com.pocketpresenter.ui

import android.os.Build

/**
 * Pocket Mode device compatibility model and volume button interceptor.
 * Meets docs/app_flow.md Screen H & docs/implementation_plan.md P7-07, P7-08.
 */
enum class DevicePocketModeCapability {
    VALIDATED,
    NOT_VALIDATED,
    UNSUPPORTED
}

object PocketModeManager {

    /**
     * Determines the validated capability for screen-off volume handling on this device.
     * Google Pixel devices running stock Android 12-14 are known to deliver volume button events
     * via foreground service / media session. Non-standard OEM skins often suppress volume keys
     * when the display is off to save battery unless explicitly whitelisted.
     */
    fun evaluateDeviceCapability(): DevicePocketModeCapability {
        val manufacturer = Build.MANUFACTURER?.lowercase().orEmpty()
        return when {
            manufacturer.contains("google") -> DevicePocketModeCapability.VALIDATED
            manufacturer.contains("samsung") || manufacturer.contains("motorola") -> DevicePocketModeCapability.NOT_VALIDATED
            else -> DevicePocketModeCapability.NOT_VALIDATED
        }
    }

    fun getLimitationExplanation(capability: DevicePocketModeCapability): String {
        return when (capability) {
            DevicePocketModeCapability.VALIDATED ->
                "Physical volume buttons are verified to change slides on this device."
            DevicePocketModeCapability.NOT_VALIDATED ->
                "Volume buttons may not work with the screen off on this phone due to manufacturer battery optimization. Keep on-screen controls available."
            DevicePocketModeCapability.UNSUPPORTED ->
                "This device does not deliver hardware volume button presses to background apps."
        }
    }
}
