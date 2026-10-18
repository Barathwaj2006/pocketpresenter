package com.pocketpresenter.presentation

import com.pocketpresenter.protocol.Envelope
import com.pocketpresenter.protocol.MessageTypes
import com.pocketpresenter.session.AndroidSessionManagerImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AndroidPresentationControllerTest {

    @Test
    fun nextAndPrevious_whenConnected_sendCorrectEnvelopes() {
        val sentEnvelopes = mutableListOf<Envelope>()
        val sessionManager = AndroidSessionManagerImpl(
            onSendEnvelope = { sentEnvelopes.add(it) },
            onClearInputs = {}
        )

        val controller = AndroidPresentationControllerImpl(
            sessionManager = sessionManager,
            onSendEnvelope = { sentEnvelopes.add(it) }
        )

        // Prior to session handshake, calling next/previous does nothing
        controller.nextSlide()
        assertEquals(0, sentEnvelopes.size)

        // Handshake
        sessionManager.startSession()
        sessionManager.handleInboundEnvelope(
            Envelope(v = 1, sid = null, seq = 0, type = MessageTypes.HELLO)
        )
        // hello.ack is in sentEnvelopes[0]
        assertEquals(1, sentEnvelopes.size)
        val sid = sessionManager.currentSessionId
        assertNotNull(sid)

        // Now test nextSlide
        controller.nextSlide()
        assertEquals(2, sentEnvelopes.size)
        val nextEnv = sentEnvelopes[1]
        assertEquals(MessageTypes.SLIDE_NEXT, nextEnv.type)
        assertEquals(sid, nextEnv.sid)
        assertEquals(1, nextEnv.seq)

        // Test previousSlide
        controller.previousSlide()
        assertEquals(3, sentEnvelopes.size)
        val prevEnv = sentEnvelopes[2]
        assertEquals(MessageTypes.SLIDE_PREVIOUS, prevEnv.type)
        assertEquals(sid, prevEnv.sid)
        assertEquals(2, prevEnv.seq)
    }
}
