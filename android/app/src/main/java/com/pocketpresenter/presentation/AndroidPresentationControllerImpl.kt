package com.pocketpresenter.presentation

import com.pocketpresenter.protocol.Envelope
import com.pocketpresenter.protocol.MessageTypes
import com.pocketpresenter.session.AndroidSessionManagerImpl
import org.json.JSONObject

/**
 * Concrete implementation of PresentationController that constructs and dispatches
 * slide navigation protocol envelopes (slide.next, slide.previous).
 */
class AndroidPresentationControllerImpl(
    private val sessionManager: AndroidSessionManagerImpl,
    private val onSendEnvelope: (Envelope) -> Unit
) : PresentationController {

    override fun nextSlide() {
        val envelope = sessionManager.createOutboundEnvelope(MessageTypes.SLIDE_NEXT, JSONObject())
        if (envelope != null) {
            onSendEnvelope(envelope)
        }
    }

    override fun previousSlide() {
        val envelope = sessionManager.createOutboundEnvelope(MessageTypes.SLIDE_PREVIOUS, JSONObject())
        if (envelope != null) {
            onSendEnvelope(envelope)
        }
    }

    override fun goToSlide(slideIndex: Int) {
        // Direct slide indexing can be routed if supported by protocol
    }
}
