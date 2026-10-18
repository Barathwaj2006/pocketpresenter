package com.pocketpresenter.input

import com.pocketpresenter.protocol.Envelope
import com.pocketpresenter.protocol.MessageTypes
import com.pocketpresenter.session.AndroidSessionManagerImpl
import org.json.JSONObject

/**
 * Concrete Android InputController.
 * Constructs and emits pointer.delta, pointer.button, and pointer.scroll protocol frames.
 * Ensures all held buttons and states are cleared on disconnect or pause.
 */
class AndroidInputControllerImpl(
    private val sessionManager: AndroidSessionManagerImpl,
    private val onSendEnvelope: (Envelope) -> Unit
) : InputController {

    private val heldButtons = mutableSetOf<AndroidMouseButton>()

    override fun sendPointerDelta(dx: Int, dy: Int) {
        if (dx == 0 && dy == 0) return
        val payload = JSONObject().apply {
            put("dx", dx)
            put("dy", dy)
        }
        val envelope = sessionManager.createOutboundEnvelope(MessageTypes.POINTER_DELTA, payload)
        if (envelope != null) {
            onSendEnvelope(envelope)
        }
    }

    override fun sendButtonDown(button: AndroidMouseButton) {
        heldButtons.add(button)
        val buttonStr = if (button == AndroidMouseButton.RIGHT) "right" else "left"
        val payload = JSONObject().apply {
            put("button", buttonStr)
            put("action", "down")
        }
        val envelope = sessionManager.createOutboundEnvelope(MessageTypes.POINTER_BUTTON, payload)
        if (envelope != null) {
            onSendEnvelope(envelope)
        }
    }

    override fun sendButtonUp(button: AndroidMouseButton) {
        heldButtons.remove(button)
        val buttonStr = if (button == AndroidMouseButton.RIGHT) "right" else "left"
        val payload = JSONObject().apply {
            put("button", buttonStr)
            put("action", "up")
        }
        val envelope = sessionManager.createOutboundEnvelope(MessageTypes.POINTER_BUTTON, payload)
        if (envelope != null) {
            onSendEnvelope(envelope)
        }
    }

    override fun sendScroll(dy: Int) {
        if (dy == 0) return
        val payload = JSONObject().apply {
            put("dy", dy)
        }
        val envelope = sessionManager.createOutboundEnvelope(MessageTypes.POINTER_SCROLL, payload)
        if (envelope != null) {
            onSendEnvelope(envelope)
        }
    }

    override fun clearState() {
        if (heldButtons.isEmpty()) return
        for (btn in heldButtons.toList()) {
            sendButtonUp(btn)
        }
        heldButtons.clear()
    }
}
