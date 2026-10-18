package com.pocketpresenter.protocol

import org.json.JSONObject

interface AndroidStateUpdateListener {
    fun onStateUpdate(link: String, show: String, effect: String, slide: Int, slideCount: Int)
    fun onCommandAck(refSeq: Int, ok: Boolean)
    fun onCommandError(refSeq: Int, code: String, message: String)
    fun onPong(timestamp: Long)
}

/**
 * Routes inbound frames received from the PowerPoint add-in to Android UI and state listeners.
 */
class AndroidCommandRouter(
    private val listener: AndroidStateUpdateListener
) {
    fun route(envelope: Envelope, session: AndroidProtocolSession): ValidationResult {
        val valResult = ProtocolValidator.validate(envelope, session)
        if (!valResult.isOk) {
            return valResult
        }

        session.markSequenceExecuted(envelope.seq)

        when (envelope.type) {
            MessageTypes.STATE_UPDATE -> {
                val link = envelope.payload.optString("link", "disconnected")
                val show = envelope.payload.optString("show", "none")
                val effect = envelope.payload.optString("effect", "off")
                val slide = envelope.payload.optInt("slide", 0)
                val slideCount = envelope.payload.optInt("slideCount", 0)
                listener.onStateUpdate(link, show, effect, slide, slideCount)
            }
            MessageTypes.COMMAND_ACK -> {
                val ref = envelope.payload.optInt("ref", 0)
                val ok = envelope.payload.optBoolean("ok", true)
                listener.onCommandAck(ref, ok)
            }
            MessageTypes.COMMAND_ERROR -> {
                val ref = envelope.payload.optInt("ref", 0)
                val code = envelope.payload.optString("code", ErrorCodes.INTERNAL)
                val msg = envelope.payload.optString("message", "")
                listener.onCommandError(ref, code, msg)
            }
            MessageTypes.SESSION_PONG -> {
                val t = envelope.payload.optLong("t", 0L)
                listener.onPong(t)
            }
        }

        return ValidationResult.success()
    }
}
