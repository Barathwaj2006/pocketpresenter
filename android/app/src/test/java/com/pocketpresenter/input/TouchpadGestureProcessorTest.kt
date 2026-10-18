package com.pocketpresenter.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MockAndroidInputController : InputController {
    val deltas = mutableListOf<Pair<Int, Int>>()
    val buttonEvents = mutableListOf<Pair<AndroidMouseButton, String>>() // (button, "down" | "up")
    val scrollDeltas = mutableListOf<Int>()
    var clearStateCalled = false

    override fun sendPointerDelta(dx: Int, dy: Int) {
        deltas.add(Pair(dx, dy))
    }

    override fun sendButtonDown(button: AndroidMouseButton) {
        buttonEvents.add(Pair(button, "down"))
    }

    override fun sendButtonUp(button: AndroidMouseButton) {
        buttonEvents.add(Pair(button, "up"))
    }

    override fun sendScroll(dy: Int) {
        scrollDeltas.add(dy)
    }

    override fun clearState() {
        clearStateCalled = true
        // emulate release
        buttonEvents.add(Pair(AndroidMouseButton.LEFT, "up"))
    }
}

class TouchpadGestureProcessorTest {

    @Test
    fun singleFingerTap_producesLeftClick() {
        val mock = MockAndroidInputController()
        val processor = TouchpadGestureProcessor(mock)

        // Action down at t = 100ms
        processor.onTouchEvent(
            action = TouchpadGestureProcessor.ACTION_DOWN,
            pointerCount = 1,
            x1 = 100f,
            y1 = 100f,
            eventTimeMs = 100L
        )

        assertEquals(TouchpadGestureProcessor.State.ONE_FINGER_TRACKING, processor.currentState)

        // Action up at t = 200ms (duration 100ms < 250ms, displacement 0 < 24)
        processor.onTouchEvent(
            action = TouchpadGestureProcessor.ACTION_UP,
            pointerCount = 1,
            x1 = 100f,
            y1 = 100f,
            eventTimeMs = 200L
        )

        assertEquals(TouchpadGestureProcessor.State.IDLE, processor.currentState)
        assertEquals(2, mock.buttonEvents.size)
        assertEquals(Pair(AndroidMouseButton.LEFT, "down"), mock.buttonEvents[0])
        assertEquals(Pair(AndroidMouseButton.LEFT, "up"), mock.buttonEvents[1])
    }

    @Test
    fun singleFingerMove_producesPointerDeltas() {
        val mock = MockAndroidInputController()
        val processor = TouchpadGestureProcessor(mock, sensitivity = 1.0f, tapSlopPx = 10f)

        processor.onTouchEvent(
            action = TouchpadGestureProcessor.ACTION_DOWN,
            pointerCount = 1,
            x1 = 100f,
            y1 = 100f,
            eventTimeMs = 0L
        )

        // Move past tap slop
        processor.onTouchEvent(
            action = TouchpadGestureProcessor.ACTION_MOVE,
            pointerCount = 1,
            x1 = 120f,
            y1 = 100f,
            eventTimeMs = 50L
        )

        assertTrue(mock.deltas.isNotEmpty())
        assertEquals(20, mock.deltas[0].first)
        assertEquals(0, mock.deltas[0].second)

        // Move further
        processor.onTouchEvent(
            action = TouchpadGestureProcessor.ACTION_MOVE,
            pointerCount = 1,
            x1 = 125f,
            y1 = 110f,
            eventTimeMs = 100L
        )

        assertEquals(2, mock.deltas.size)
        assertEquals(5, mock.deltas[1].first)
        assertEquals(10, mock.deltas[1].second)

        processor.onTouchEvent(
            action = TouchpadGestureProcessor.ACTION_UP,
            pointerCount = 1,
            x1 = 125f,
            y1 = 110f,
            eventTimeMs = 150L
        )

        // Since it moved past slop, no tap click is emitted
        assertEquals(0, mock.buttonEvents.size)
    }

    @Test
    fun twoFingerTap_producesRightClick() {
        val mock = MockAndroidInputController()
        val processor = TouchpadGestureProcessor(mock)

        processor.onTouchEvent(
            action = TouchpadGestureProcessor.ACTION_DOWN,
            pointerCount = 1,
            x1 = 100f,
            y1 = 100f,
            eventTimeMs = 100L
        )

        processor.onTouchEvent(
            action = TouchpadGestureProcessor.ACTION_POINTER_DOWN,
            pointerCount = 2,
            x1 = 100f,
            y1 = 100f,
            x2 = 150f,
            y2 = 100f,
            eventTimeMs = 120L
        )

        assertEquals(TouchpadGestureProcessor.State.TWO_FINGER_TRACKING, processor.currentState)

        // Pointer up quickly without movement
        processor.onTouchEvent(
            action = TouchpadGestureProcessor.ACTION_POINTER_UP,
            pointerCount = 2,
            x1 = 100f,
            y1 = 100f,
            x2 = 150f,
            y2 = 100f,
            eventTimeMs = 200L
        )

        assertEquals(2, mock.buttonEvents.size)
        assertEquals(Pair(AndroidMouseButton.RIGHT, "down"), mock.buttonEvents[0])
        assertEquals(Pair(AndroidMouseButton.RIGHT, "up"), mock.buttonEvents[1])
    }

    @Test
    fun twoFingerScroll_respectsReverseScroll() {
        val mock = MockAndroidInputController()
        val processor = TouchpadGestureProcessor(mock, reverseScroll = false)

        processor.onTouchEvent(
            action = TouchpadGestureProcessor.ACTION_DOWN,
            pointerCount = 1,
            x1 = 100f,
            y1 = 100f,
            eventTimeMs = 0L
        )
        processor.onTouchEvent(
            action = TouchpadGestureProcessor.ACTION_POINTER_DOWN,
            pointerCount = 2,
            x1 = 100f,
            y1 = 100f,
            x2 = 150f,
            y2 = 100f,
            eventTimeMs = 10L
        )

        // Scroll down (avg Y increases by 30)
        processor.onTouchEvent(
            action = TouchpadGestureProcessor.ACTION_MOVE,
            pointerCount = 2,
            x1 = 100f,
            y1 = 130f,
            x2 = 150f,
            y2 = 130f,
            eventTimeMs = 50L
        )

        assertTrue(mock.scrollDeltas.isNotEmpty())
        assertEquals(3, mock.scrollDeltas[0]) // 30 / 10 = 3

        // Now test reverse scroll
        val reverseMock = MockAndroidInputController()
        val reverseProcessor = TouchpadGestureProcessor(reverseMock, reverseScroll = true)

        reverseProcessor.onTouchEvent(
            action = TouchpadGestureProcessor.ACTION_DOWN,
            pointerCount = 1,
            x1 = 100f,
            y1 = 100f,
            eventTimeMs = 0L
        )
        reverseProcessor.onTouchEvent(
            action = TouchpadGestureProcessor.ACTION_POINTER_DOWN,
            pointerCount = 2,
            x1 = 100f,
            y1 = 100f,
            x2 = 150f,
            y2 = 100f,
            eventTimeMs = 10L
        )
        reverseProcessor.onTouchEvent(
            action = TouchpadGestureProcessor.ACTION_MOVE,
            pointerCount = 2,
            x1 = 100f,
            y1 = 130f,
            x2 = 150f,
            y2 = 130f,
            eventTimeMs = 50L
        )

        assertTrue(reverseMock.scrollDeltas.isNotEmpty())
        assertEquals(-3, reverseMock.scrollDeltas[0])
    }

    @Test
    fun longPressDrag_holdsButtonAndReleasesOnLift() {
        val mock = MockAndroidInputController()
        val processor = TouchpadGestureProcessor(mock, longPressDurationMs = 300L)

        processor.onTouchEvent(
            action = TouchpadGestureProcessor.ACTION_DOWN,
            pointerCount = 1,
            x1 = 100f,
            y1 = 100f,
            eventTimeMs = 100L
        )

        // Stationary move after 350ms -> triggers drag
        processor.onTouchEvent(
            action = TouchpadGestureProcessor.ACTION_MOVE,
            pointerCount = 1,
            x1 = 102f,
            y1 = 101f,
            eventTimeMs = 450L
        )

        assertEquals(TouchpadGestureProcessor.State.DRAGGING, processor.currentState)
        assertEquals(1, mock.buttonEvents.size)
        assertEquals(Pair(AndroidMouseButton.LEFT, "down"), mock.buttonEvents[0])

        // Dragging motion
        processor.onTouchEvent(
            action = TouchpadGestureProcessor.ACTION_MOVE,
            pointerCount = 1,
            x1 = 120f,
            y1 = 101f,
            eventTimeMs = 500L
        )
        assertTrue(mock.deltas.isNotEmpty())

        // Lift finger -> button up
        processor.onTouchEvent(
            action = TouchpadGestureProcessor.ACTION_UP,
            pointerCount = 1,
            x1 = 120f,
            y1 = 101f,
            eventTimeMs = 600L
        )

        assertEquals(TouchpadGestureProcessor.State.IDLE, processor.currentState)
        assertEquals(2, mock.buttonEvents.size)
        assertEquals(Pair(AndroidMouseButton.LEFT, "up"), mock.buttonEvents[1])
    }

    @Test
    fun cancelGesture_releasesDraggingButtonImmediately() {
        val mock = MockAndroidInputController()
        val processor = TouchpadGestureProcessor(mock, longPressDurationMs = 300L)

        processor.onTouchEvent(
            action = TouchpadGestureProcessor.ACTION_DOWN,
            pointerCount = 1,
            x1 = 100f,
            y1 = 100f,
            eventTimeMs = 100L
        )
        processor.onTouchEvent(
            action = TouchpadGestureProcessor.ACTION_MOVE,
            pointerCount = 1,
            x1 = 101f,
            y1 = 101f,
            eventTimeMs = 450L
        )

        assertEquals(TouchpadGestureProcessor.State.DRAGGING, processor.currentState)

        // External cancel (e.g. app pause, phone call, gesture cancel)
        processor.onTouchEvent(
            action = TouchpadGestureProcessor.ACTION_CANCEL,
            pointerCount = 1,
            x1 = 101f,
            y1 = 101f,
            eventTimeMs = 500L
        )

        assertEquals(TouchpadGestureProcessor.State.IDLE, processor.currentState)
        assertTrue(mock.clearStateCalled)
    }
}
