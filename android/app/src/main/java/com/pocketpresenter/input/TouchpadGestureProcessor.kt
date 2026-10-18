package com.pocketpresenter.input

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Recognizes and transforms raw multi-touch screen events into presentation pointer gestures:
 * - 1 finger drag: relative cursor move
 * - 1 finger tap: left click
 * - 2 finger tap: right click
 * - 2 finger vertical drag: mouse wheel scroll
 * - Long-press hold then drag: left button click & drag
 * - Cancellation / pause: guarantees all held buttons are released immediately
 */
class TouchpadGestureProcessor(
    private val inputController: InputController,
    var sensitivity: Float = 1.0f,
    var reverseScroll: Boolean = false,
    var tapSlopPx: Float = 24.0f,
    var maxTapDurationMs: Long = 250L,
    var longPressDurationMs: Long = 400L
) {
    enum class State {
        IDLE,
        ONE_FINGER_TRACKING,
        DRAGGING,
        TWO_FINGER_TRACKING
    }

    // Motion event action constants
    companion object {
        const val ACTION_DOWN = 0
        const val ACTION_UP = 1
        const val ACTION_MOVE = 2
        const val ACTION_CANCEL = 3
        const val ACTION_POINTER_DOWN = 5
        const val ACTION_POINTER_UP = 6
    }

    var currentState: State = State.IDLE
        private set

    private var startX: Float = 0f
    private var startY: Float = 0f
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var downTimeMs: Long = 0L
    private var hasMovedPastSlop: Boolean = false

    private var twoFingerDownTimeMs: Long = 0L
    private var lastTwoFingerY: Float = 0f
    private var twoFingerMoved: Boolean = false

    fun onTouchEvent(
        action: Int,
        pointerCount: Int,
        x1: Float,
        y1: Float,
        x2: Float = 0f,
        y2: Float = 0f,
        eventTimeMs: Long
    ) {
        val actionMasked = action and 0xFF

        when (actionMasked) {
            ACTION_DOWN -> {
                startX = x1
                startY = y1
                lastX = x1
                lastY = y1
                downTimeMs = eventTimeMs
                hasMovedPastSlop = false
                currentState = State.ONE_FINGER_TRACKING
            }

            ACTION_POINTER_DOWN -> {
                if (pointerCount >= 2) {
                    if (currentState == State.DRAGGING) {
                        inputController.sendButtonUp(AndroidMouseButton.LEFT)
                    }
                    currentState = State.TWO_FINGER_TRACKING
                    twoFingerDownTimeMs = eventTimeMs
                    lastTwoFingerY = (y1 + y2) / 2f
                    twoFingerMoved = false
                }
            }

            ACTION_MOVE -> {
                when (currentState) {
                    State.ONE_FINGER_TRACKING -> {
                        val duration = eventTimeMs - downTimeMs
                        val dist = distance(startX, startY, x1, y1)

                        if (!hasMovedPastSlop && dist >= tapSlopPx) {
                            hasMovedPastSlop = true
                        }

                        // Long-press detection to enter drag mode
                        if (!hasMovedPastSlop && duration >= longPressDurationMs) {
                            currentState = State.DRAGGING
                            inputController.sendButtonDown(AndroidMouseButton.LEFT)
                            lastX = x1
                            lastY = y1
                            return
                        }

                        if (hasMovedPastSlop) {
                            val dx = (x1 - lastX) * sensitivity
                            val dy = (y1 - lastY) * sensitivity
                            inputController.sendPointerDelta(dx.roundToInt(), dy.roundToInt())
                            lastX = x1
                            lastY = y1
                        }
                    }

                    State.DRAGGING -> {
                        val dx = (x1 - lastX) * sensitivity
                        val dy = (y1 - lastY) * sensitivity
                        inputController.sendPointerDelta(dx.roundToInt(), dy.roundToInt())
                        lastX = x1
                        lastY = y1
                    }

                    State.TWO_FINGER_TRACKING -> {
                        val currentAvgY = (y1 + y2) / 2f
                        val dy = (currentAvgY - lastTwoFingerY) * sensitivity

                        if (abs(dy) >= 2.0f) {
                            twoFingerMoved = true
                            val scrollDelta = (if (reverseScroll) -dy else dy) / 10.0f
                            // Normal mouse wheel increment
                            inputController.sendScroll(scrollDelta.roundToInt())
                            lastTwoFingerY = currentAvgY
                        }
                    }

                    State.IDLE -> {}
                }
            }

            ACTION_POINTER_UP -> {
                if (currentState == State.TWO_FINGER_TRACKING) {
                    val duration = eventTimeMs - twoFingerDownTimeMs
                    if (duration <= maxTapDurationMs && !twoFingerMoved) {
                        // Two finger tap -> Right Click
                        inputController.sendButtonDown(AndroidMouseButton.RIGHT)
                        inputController.sendButtonUp(AndroidMouseButton.RIGHT)
                    }
                    currentState = State.IDLE
                }
            }

            ACTION_UP -> {
                when (currentState) {
                    State.ONE_FINGER_TRACKING -> {
                        val duration = eventTimeMs - downTimeMs
                        if (duration <= maxTapDurationMs && !hasMovedPastSlop) {
                            // Single finger tap -> Left Click
                            inputController.sendButtonDown(AndroidMouseButton.LEFT)
                            inputController.sendButtonUp(AndroidMouseButton.LEFT)
                        }
                    }

                    State.DRAGGING -> {
                        // Release left button on drag end
                        inputController.sendButtonUp(AndroidMouseButton.LEFT)
                    }

                    else -> {}
                }
                currentState = State.IDLE
            }

            ACTION_CANCEL -> {
                cancelGesture()
            }
        }
    }

    fun cancelGesture() {
        if (currentState == State.DRAGGING) {
            inputController.sendButtonUp(AndroidMouseButton.LEFT)
        }
        currentState = State.IDLE
        inputController.clearState()
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt((dx * dx) + (dy * dy))
    }
}
