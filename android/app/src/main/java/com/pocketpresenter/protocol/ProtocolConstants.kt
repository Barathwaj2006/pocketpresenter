package com.pocketpresenter.protocol

object ProtocolConstants {
    const val CURRENT_PROTOCOL_VERSION = 1
    const val MAX_FRAME_BYTES = 4096

    const val MIN_POINTER_DELTA = -2000
    const val MAX_POINTER_DELTA = 2000

    const val MIN_POINTER_SCROLL = -1000
    const val MAX_POINTER_SCROLL = 1000

    const val MAX_DRAW_POINTS_PER_MESSAGE = 32

    const val MIN_EFFECT_INTENSITY = 0.0
    const val MAX_EFFECT_INTENSITY = 1.0

    const val MIN_EFFECT_SCALE = 0.5
    const val MAX_EFFECT_SCALE = 2.0

    val ALLOWED_EFFECTS = setOf("honeycomb", "diamond", "ripple", "off")
    val ALLOWED_POINTER_BUTTONS = setOf("left", "right")
    val ALLOWED_BUTTON_ACTIONS = setOf("down", "up")
}

object ErrorCodes {
    const val BAD_FRAME = "BAD_FRAME"
    const val BAD_VERSION = "BAD_VERSION"
    const val BAD_TYPE = "BAD_TYPE"
    const val BAD_FIELD = "BAD_FIELD"
    const val BAD_SESSION = "BAD_SESSION"
    const val NOT_READY = "NOT_READY"
    const val NO_ACTIVE_SLIDESHOW = "NO_ACTIVE_SLIDESHOW"
    const val NOT_ARMED = "NOT_ARMED"
    const val RATE_LIMITED = "RATE_LIMITED"
    const val DUPLICATE = "DUPLICATE"
    const val INTERNAL = "INTERNAL"
}
