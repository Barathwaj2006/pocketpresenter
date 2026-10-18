package com.pocketpresenter.protocol

import org.json.JSONObject

data class ValidationResult(
    val isOk: Boolean,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val closeConnection: Boolean = false,
    val isDuplicate: Boolean = false
) {
    companion object {
        fun success() = ValidationResult(isOk = true)
        fun error(code: String, message: String, closeConnection: Boolean = false) =
            ValidationResult(isOk = false, errorCode = code, errorMessage = message, closeConnection = closeConnection)
        fun duplicate(message: String) =
            ValidationResult(isOk = false, errorCode = ErrorCodes.DUPLICATE, errorMessage = message, isDuplicate = true)
    }
}

class AndroidProtocolSession {
    var handshakeComplete: Boolean = false
    var sessionId: String? = null
    var lastSequenceReceived: Int = -1
    private val executedSequences = mutableSetOf<Int>()

    fun reset(newSessionId: String? = null) {
        handshakeComplete = !newSessionId.isNullOrEmpty()
        sessionId = newSessionId
        lastSequenceReceived = -1
        executedSequences.clear()
    }

    fun hasExecutedSequence(seq: Int): Boolean = seq in executedSequences
    fun markSequenceExecuted(seq: Int) {
        executedSequences.add(seq)
    }
}

object ProtocolValidator {
    fun validate(envelope: Envelope, session: AndroidProtocolSession): ValidationResult {
        // 1. Version check
        if (envelope.v != ProtocolConstants.CURRENT_PROTOCOL_VERSION) {
            return ValidationResult.error(ErrorCodes.BAD_VERSION, "Unsupported protocol version ${envelope.v}")
        }

        // 2. Type check
        if (!MessageTypes.isValidType(envelope.type)) {
            return ValidationResult.error(ErrorCodes.BAD_TYPE, "Unknown message type '${envelope.type}'")
        }

        // 3. Handshake check
        if (!session.handshakeComplete) {
            if (envelope.type != MessageTypes.HELLO && envelope.type != MessageTypes.HELLO_ACK) {
                return ValidationResult.error(ErrorCodes.NOT_READY, "Message '${envelope.type}' rejected: handshake not complete")
            }
        } else {
            if (envelope.sid.isNullOrEmpty() || envelope.sid != session.sessionId) {
                return ValidationResult.error(ErrorCodes.BAD_SESSION, "Invalid or mismatching session id: '${envelope.sid}'")
            }
        }

        // 4. Duplicate sequence check for discrete commands
        if (envelope.type == MessageTypes.SLIDE_NEXT || envelope.type == MessageTypes.SLIDE_PREVIOUS) {
            if (session.hasExecutedSequence(envelope.seq)) {
                return ValidationResult.duplicate("Duplicate sequence ${envelope.seq} for ${envelope.type}")
            }
        }

        // 5. Payload bounds validation
        return validatePayload(envelope.type, envelope.payload)
    }

    private fun validatePayload(type: String, payload: JSONObject): ValidationResult {
        when (type) {
            MessageTypes.POINTER_DELTA -> {
                if (payload.has("dx")) {
                    val dx = payload.optInt("dx")
                    if (dx < ProtocolConstants.MIN_POINTER_DELTA || dx > ProtocolConstants.MAX_POINTER_DELTA) {
                        return ValidationResult.error(ErrorCodes.BAD_FIELD, "dx out of bounds")
                    }
                }
                if (payload.has("dy")) {
                    val dy = payload.optInt("dy")
                    if (dy < ProtocolConstants.MIN_POINTER_DELTA || dy > ProtocolConstants.MAX_POINTER_DELTA) {
                        return ValidationResult.error(ErrorCodes.BAD_FIELD, "dy out of bounds")
                    }
                }
            }
            MessageTypes.POINTER_SCROLL -> {
                if (payload.has("dy")) {
                    val dy = payload.optInt("dy")
                    if (dy < ProtocolConstants.MIN_POINTER_SCROLL || dy > ProtocolConstants.MAX_POINTER_SCROLL) {
                        return ValidationResult.error(ErrorCodes.BAD_FIELD, "scroll dy out of bounds")
                    }
                }
            }
            MessageTypes.POINTER_BUTTON -> {
                if (payload.has("button")) {
                    val btn = payload.optString("button")
                    if (btn !in ProtocolConstants.ALLOWED_POINTER_BUTTONS) {
                        return ValidationResult.error(ErrorCodes.BAD_FIELD, "Invalid button '$btn'")
                    }
                }
                if (payload.has("action")) {
                    val act = payload.optString("action")
                    if (act !in ProtocolConstants.ALLOWED_BUTTON_ACTIONS) {
                        return ValidationResult.error(ErrorCodes.BAD_FIELD, "Invalid action '$act'")
                    }
                }
            }
            MessageTypes.EFFECT_SET -> {
                if (payload.has("effect")) {
                    val eff = payload.optString("effect")
                    if (eff !in ProtocolConstants.ALLOWED_EFFECTS) {
                        return ValidationResult.error(ErrorCodes.BAD_FIELD, "Unknown effect '$eff'")
                    }
                }
                if (payload.has("intensity")) {
                    val intensity = payload.optDouble("intensity")
                    if (intensity < ProtocolConstants.MIN_EFFECT_INTENSITY || intensity > ProtocolConstants.MAX_EFFECT_INTENSITY) {
                        return ValidationResult.error(ErrorCodes.BAD_FIELD, "intensity out of range")
                    }
                }
                if (payload.has("scale")) {
                    val scale = payload.optDouble("scale")
                    if (scale < ProtocolConstants.MIN_EFFECT_SCALE || scale > ProtocolConstants.MAX_EFFECT_SCALE) {
                        return ValidationResult.error(ErrorCodes.BAD_FIELD, "scale out of range")
                    }
                }
            }
            MessageTypes.DRAW_POINT -> {
                if (payload.has("points")) {
                    val pts = payload.optJSONArray("points")
                    if (pts != null && pts.length() > ProtocolConstants.MAX_DRAW_POINTS_PER_MESSAGE) {
                        return ValidationResult.error(ErrorCodes.BAD_FIELD, "Exceeded max draw points")
                    }
                }
            }
        }

        return ValidationResult.success()
    }
}
