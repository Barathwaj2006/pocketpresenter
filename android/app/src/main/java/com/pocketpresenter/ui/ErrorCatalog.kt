package com.pocketpresenter.ui

/**
 * Single actionable recovery item conforming to docs/app_flow.md section 4:
 * "Every error shows one recommended next action. Never show raw stack traces."
 */
data class AppError(
    val id: String,
    val message: String,
    val actionLabel: String,
    val actionType: ActionType
) {
    enum class ActionType {
        ENABLE_BLUETOOTH,
        REQUEST_PERMISSION,
        OPEN_APP_SETTINGS,
        OPEN_BLUETOOTH_SETTINGS,
        RECONNECT,
        DISMISS,
        VIEW_SUPPORTED_DEVICES
    }
}

object ErrorCatalog {
    val BLUETOOTH_OFF = AppError(
        id = "BLUETOOTH_OFF",
        message = "Bluetooth is off.",
        actionLabel = "Turn on Bluetooth",
        actionType = AppError.ActionType.ENABLE_BLUETOOTH
    )

    val BLUETOOTH_PERMISSION_DENIED = AppError(
        id = "BLUETOOTH_PERMISSION_DENIED",
        message = "PocketPresenter needs Bluetooth permission to connect to your PC.",
        actionLabel = "Grant permission",
        actionType = AppError.ActionType.REQUEST_PERMISSION
    )

    val PERMISSION_PERMANENTLY_DENIED = AppError(
        id = "PERMISSION_PERMANENTLY_DENIED",
        message = "Bluetooth permission is turned off for this app.",
        actionLabel = "Open app settings",
        actionType = AppError.ActionType.OPEN_APP_SETTINGS
    )

    val LACKS_BLUETOOTH_CLASSIC = AppError(
        id = "LACKS_BLUETOOTH_CLASSIC",
        message = "This phone can't use Bluetooth Classic, which PocketPresenter needs.",
        actionLabel = "Supported devices",
        actionType = AppError.ActionType.VIEW_SUPPORTED_DEVICES
    )

    val NO_PC_PAIRING = AppError(
        id = "NO_PC_PAIRING",
        message = "Pair your phone with your PC first.",
        actionLabel = "Open Bluetooth settings",
        actionType = AppError.ActionType.OPEN_BLUETOOTH_SETTINGS
    )

    val FOREGROUND_SESSION_BLOCKED = AppError(
        id = "FOREGROUND_SESSION_BLOCKED",
        message = "Android didn't allow the session to start in the background.",
        actionLabel = "Open PocketPresenter",
        actionType = AppError.ActionType.DISMISS
    )

    val POWERPOINT_TIMEOUT = AppError(
        id = "POWERPOINT_TIMEOUT",
        message = "Waiting for PowerPoint.",
        actionLabel = "In PowerPoint, open PocketPresenter → Connect phone.",
        actionType = AppError.ActionType.DISMISS
    )

    val BAD_VERSION = AppError(
        id = "BAD_VERSION",
        message = "The phone app and PowerPoint add-in versions don't match.",
        actionLabel = "Update",
        actionType = AppError.ActionType.VIEW_SUPPORTED_DEVICES
    )

    val NO_ACTIVE_SLIDESHOW = AppError(
        id = "NO_ACTIVE_SLIDESHOW",
        message = "No slide show is running.",
        actionLabel = "Start the slide show in PowerPoint.",
        actionType = AppError.ActionType.DISMISS
    )

    val NOT_ARMED = AppError(
        id = "NOT_ARMED",
        message = "Control is paused.",
        actionLabel = "Return to slide show window / reconnect",
        actionType = AppError.ActionType.RECONNECT
    )

    val LINK_LOST = AppError(
        id = "LINK_LOST",
        message = "Connection lost.",
        actionLabel = "Reconnect now",
        actionType = AppError.ActionType.RECONNECT
    )

    val SCREEN_OFF_UNVALIDATED = AppError(
        id = "SCREEN_OFF_UNVALIDATED",
        message = "Volume buttons may not work with the screen off on this phone.",
        actionLabel = "Supported devices",
        actionType = AppError.ActionType.VIEW_SUPPORTED_DEVICES
    )

    fun fromProtocolErrorCode(code: String): AppError {
        return when (code) {
            "BAD_VERSION" -> BAD_VERSION
            "NO_ACTIVE_SLIDESHOW" -> NO_ACTIVE_SLIDESHOW
            "NOT_ARMED" -> NOT_ARMED
            "LINK_LOST" -> LINK_LOST
            else -> AppError(
                id = code,
                message = "Error: $code",
                actionLabel = "Dismiss",
                actionType = AppError.ActionType.DISMISS
            )
        }
    }
}
