package com.pocketpresenter.session

import com.pocketpresenter.protocol.Envelope
import com.pocketpresenter.protocol.MessageTypes
import com.pocketpresenter.protocol.ProtocolConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Android session controller.
 * Manages session state machine, generates UUID session IDs in hello.ack,
 * tracks sequential message counters, and guarantees gesture cleanup on disconnect.
 */
class AndroidSessionManagerImpl(
    private val onSendEnvelope: (Envelope) -> Unit,
    private val onClearInputs: () -> Unit
) : SessionManager {

    private val _state = MutableStateFlow<SessionState>(SessionState.Disconnected)
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    override var currentSessionId: String? = null
        private set

    private var outboundSeqCounter: Int = 0

    override fun startSession() {
        if (_state.value is SessionState.Disconnected) {
            outboundSeqCounter = 0
            currentSessionId = null
            _state.value = SessionState.PhoneReady
        }
    }

    override fun stopSession() {
        disconnect()
    }

    override fun updateState(newState: SessionState) {
        _state.value = newState
    }

    fun handleInboundEnvelope(envelope: Envelope) {
        when (envelope.type) {
            MessageTypes.HELLO -> {
                // Connecting add-in sent hello -> reply with hello.ack containing a fresh UUID session id
                val newSid = UUID.randomUUID().toString()
                currentSessionId = newSid
                outboundSeqCounter = 0

                val ackPayload = JSONObject().apply {
                    put("accepted", true)
                    put("version", ProtocolConstants.CURRENT_PROTOCOL_VERSION)
                    put("sessionId", newSid)
                    put("capabilities", JSONArray(listOf("pointer", "slides", "effects", "draw")))
                }

                val ackEnvelope = Envelope(
                    v = ProtocolConstants.CURRENT_PROTOCOL_VERSION,
                    sid = null, // hello.ack does not have sid in outer envelope
                    seq = outboundSeqCounter++,
                    type = MessageTypes.HELLO_ACK,
                    payload = ackPayload
                )

                onSendEnvelope(ackEnvelope)
                _state.value = SessionState.ConnectedIdle
            }
        }
    }

    fun createOutboundEnvelope(type: String, payload: JSONObject = JSONObject()): Envelope? {
        val sid = currentSessionId ?: return null
        return Envelope(
            v = ProtocolConstants.CURRENT_PROTOCOL_VERSION,
            sid = sid,
            seq = outboundSeqCounter++,
            type = type,
            payload = payload
        )
    }

    fun disconnect() {
        if (_state.value is SessionState.Disconnected) return

        _state.value = SessionState.Disconnecting

        // Mandatory cleanup: reset sequence, clear held gestures and touch states
        currentSessionId = null
        outboundSeqCounter = 0
        onClearInputs()

        _state.value = SessionState.Disconnected
    }
}
