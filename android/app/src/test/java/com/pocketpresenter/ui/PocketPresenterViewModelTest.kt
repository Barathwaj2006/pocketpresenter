package com.pocketpresenter.ui

import android.view.KeyEvent
import com.pocketpresenter.protocol.Envelope
import com.pocketpresenter.protocol.MessageTypes
import com.pocketpresenter.session.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PocketPresenterViewModel.
 * Verifies the State-Driven Controls Table (docs/app_flow.md §3),
 * Volume Key routing, Live Effect Dispatching, and Error Handling.
 */
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class PocketPresenterViewModelTest {

    private lateinit var viewModel: PocketPresenterViewModel
    private val sentEnvelopes = mutableListOf<Envelope>()

    @Before
    fun setup() {
        viewModel = PocketPresenterViewModel(coroutineScope = CoroutineScope(Dispatchers.Unconfined))
        sentEnvelopes.clear()
        viewModel.onSendEnvelope = { env -> sentEnvelopes.add(env) }
    }

    @Test
    fun stateDrivenControlsTable_matchesAppFlowSpecification() {
        // Disconnected
        viewModel.sessionManager.updateState(SessionState.Disconnected)
        assertEquals("Not connected", viewModel.getPhoneStatusLabel())
        assertFalse(viewModel.isTouchpadControlsEnabled())

        // PhoneReady
        viewModel.sessionManager.updateState(SessionState.PhoneReady)
        assertEquals("Waiting for PowerPoint", viewModel.getPhoneStatusLabel())
        assertFalse(viewModel.isTouchpadControlsEnabled())

        // Connecting
        viewModel.sessionManager.updateState(SessionState.Connecting)
        assertEquals("Connecting…", viewModel.getPhoneStatusLabel())
        assertFalse(viewModel.isTouchpadControlsEnabled())

        // ConnectedIdle
        viewModel.sessionManager.updateState(SessionState.ConnectedIdle)
        assertEquals("Connected — start the slide show", viewModel.getPhoneStatusLabel())
        assertFalse(viewModel.isTouchpadControlsEnabled())

        // PresentingArmed
        viewModel.sessionManager.updateState(SessionState.PresentingArmed)
        assertEquals("Presenting", viewModel.getPhoneStatusLabel())
        assertTrue(viewModel.isTouchpadControlsEnabled())

        // Disconnecting
        viewModel.sessionManager.updateState(SessionState.Disconnecting)
        assertEquals("Ending session…", viewModel.getPhoneStatusLabel())
        assertFalse(viewModel.isTouchpadControlsEnabled())
    }

    @Test
    fun volumeKeys_onlySentWhenPresentingArmed() {
        // Disconnected -> volume keys must NOT be sent or consumed
        viewModel.sessionManager.updateState(SessionState.Disconnected)
        val handledDisconnected = viewModel.handleVolumeKeyEvent(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_DOWN)
        assertFalse(handledDisconnected)
        assertTrue(sentEnvelopes.isEmpty())

        // ConnectedIdle -> volume keys must NOT be sent or consumed
        viewModel.sessionManager.updateState(SessionState.ConnectedIdle)
        val handledConnectedIdle = viewModel.handleVolumeKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN)
        assertFalse(handledConnectedIdle)
        assertTrue(sentEnvelopes.isEmpty())

        // PresentingArmed -> Volume Up sends slide.next and consumes key event
        viewModel.sessionManager.updateState(SessionState.PresentingArmed)
        // Simulate handshake so currentSessionId is present
        viewModel.sessionManager.handleInboundEnvelope(
            Envelope(v = 1, sid = null, seq = 0, type = MessageTypes.HELLO, payload = org.json.JSONObject())
        )
        viewModel.sessionManager.updateState(SessionState.PresentingArmed)
        sentEnvelopes.clear()

        val handledNext = viewModel.handleVolumeKeyEvent(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_DOWN)
        assertTrue(handledNext)
        assertEquals(1, sentEnvelopes.size)
        assertEquals(MessageTypes.SLIDE_NEXT, sentEnvelopes[0].type)

        // Volume Down sends slide.previous and consumes key event
        val handledPrev = viewModel.handleVolumeKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN)
        assertTrue(handledPrev)
        assertEquals(2, sentEnvelopes.size)
        assertEquals(MessageTypes.SLIDE_PREVIOUS, sentEnvelopes[1].type)

        // Key Up event is ignored by handler
        val handledKeyUp = viewModel.handleVolumeKeyEvent(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_UP)
        assertFalse(handledKeyUp)
    }

    @Test
    fun effectSelection_dispatchesLiveEnvelopeWhenArmedOrConnectedIdle() {
        // Handshake to set currentSessionId
        viewModel.sessionManager.handleInboundEnvelope(
            Envelope(v = 1, sid = null, seq = 0, type = MessageTypes.HELLO, payload = org.json.JSONObject())
        )
        sentEnvelopes.clear()

        // 1. ConnectedIdle -> dispatches effect.set live
        viewModel.sessionManager.updateState(SessionState.ConnectedIdle)
        viewModel.selectEffect("diamond")
        assertEquals(1, sentEnvelopes.size)
        assertEquals(MessageTypes.EFFECT_SET, sentEnvelopes[0].type)
        assertEquals("diamond", sentEnvelopes[0].payload.getString("effect"))
        assertEquals("diamond", viewModel.userSettings.value.selectedEffect)

        // 2. PresentingArmed -> dispatches effect.set live
        sentEnvelopes.clear()
        viewModel.sessionManager.updateState(SessionState.PresentingArmed)
        viewModel.selectEffect("ripple")
        assertEquals(1, sentEnvelopes.size)
        assertEquals(MessageTypes.EFFECT_SET, sentEnvelopes[0].type)
        assertEquals("ripple", sentEnvelopes[0].payload.getString("effect"))

        // Intensity update -> dispatches effect.set live
        sentEnvelopes.clear()
        viewModel.updateEffectIntensity(0.75f)
        assertEquals(1, sentEnvelopes.size)
        assertEquals(0.75, sentEnvelopes[0].payload.getDouble("intensity"), 0.01)

        // Reduced motion update -> dispatches effect.set live
        sentEnvelopes.clear()
        viewModel.updateReducedMotion(true)
        assertEquals(1, sentEnvelopes.size)
        assertTrue(sentEnvelopes[0].payload.getBoolean("reducedMotion"))

        // 3. Disconnected -> updates settings locally, does NOT dispatch envelope
        sentEnvelopes.clear()
        viewModel.sessionManager.updateState(SessionState.Disconnected)
        viewModel.selectEffect("honeycomb")
        assertTrue(sentEnvelopes.isEmpty())
        assertEquals("honeycomb", viewModel.userSettings.value.selectedEffect)
    }

    @Test
    fun dragLock_togglesLeftMouseButtonDownAndUp() {
        // Cannot toggle when not presenting armed
        viewModel.sessionManager.updateState(SessionState.ConnectedIdle)
        viewModel.toggleDragLock()
        assertFalse(viewModel.isDragLocked.value)

        // Arm session and set valid sid
        viewModel.sessionManager.handleInboundEnvelope(
            Envelope(v = 1, sid = null, seq = 0, type = MessageTypes.HELLO, payload = org.json.JSONObject())
        )
        viewModel.sessionManager.updateState(SessionState.PresentingArmed)
        sentEnvelopes.clear()

        // Toggle drag lock ON -> sends left button down
        viewModel.toggleDragLock()
        assertTrue(viewModel.isDragLocked.value)
        assertEquals(1, sentEnvelopes.size)
        assertEquals(MessageTypes.POINTER_BUTTON, sentEnvelopes[0].type)
        assertEquals("down", sentEnvelopes[0].payload.getString("action"))
        assertEquals("left", sentEnvelopes[0].payload.getString("button"))

        // Toggle drag lock OFF -> sends left button up
        viewModel.toggleDragLock()
        assertFalse(viewModel.isDragLocked.value)
        assertEquals(2, sentEnvelopes.size)
        assertEquals(MessageTypes.POINTER_BUTTON, sentEnvelopes[1].type)
        assertEquals("up", sentEnvelopes[1].payload.getString("action"))
    }

    @Test
    fun navigationAndErrorReporting() {
        assertEquals(AppScreen.HOME, viewModel.currentScreen.value)
        viewModel.navigateTo(AppScreen.SETUP)
        assertEquals(AppScreen.SETUP, viewModel.currentScreen.value)

        assertNull(viewModel.currentError.value)
        viewModel.reportError(ErrorCatalog.BLUETOOTH_OFF)
        assertNotNull(viewModel.currentError.value)
        assertEquals("BLUETOOTH_OFF", viewModel.currentError.value?.id)

        viewModel.dismissError()
        assertNull(viewModel.currentError.value)
    }

    @Test
    fun settingsUpdate_updatesGestureProcessor() {
        viewModel.updateTouchpadSensitivity(2.0f)
        assertEquals(2.0f, viewModel.userSettings.value.touchpadSensitivity, 0.01f)
        assertEquals(2.0f, viewModel.gestureProcessor.sensitivity, 0.01f)

        viewModel.updateReverseScroll(true)
        assertTrue(viewModel.userSettings.value.reverseScroll)
        assertTrue(viewModel.gestureProcessor.reverseScroll)
    }
}
