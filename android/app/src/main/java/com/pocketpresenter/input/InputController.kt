package com.pocketpresenter.input

enum class AndroidMouseButton {
    LEFT,
    RIGHT
}

/**
 * Controller for touchpad movement, taps, scroll, and drag gestures.
 * Touch and gesture states must be cleared immediately on disconnect or pause.
 */
interface InputController {
    fun sendPointerDelta(dx: Int, dy: Int)
    fun sendButtonDown(button: AndroidMouseButton)
    fun sendButtonUp(button: AndroidMouseButton)
    fun sendScroll(dy: Int)
    fun clearState()
}
