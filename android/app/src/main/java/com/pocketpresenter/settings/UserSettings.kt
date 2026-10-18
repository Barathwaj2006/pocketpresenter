package com.pocketpresenter.settings

/**
 * Local preferences model for Android app (persisted in DataStore).
 * See docs/backend_schema.md section 3.
 */
data class UserSettings(
    val touchpadSensitivity: Float = 1.0f,
    val reverseScroll: Boolean = false,
    val tapToClick: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val pocketModeArmPromptDismissed: Boolean = false,
    val selectedEffect: String = "honeycomb",
    val effectIntensity: Float = 0.35f,
    val effectScale: Float = 1.0f,
    val reducedMotion: Boolean = false
)
