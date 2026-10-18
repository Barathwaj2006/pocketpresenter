package com.pocketpresenter.protocol

data class AndroidDecodeResult(
    val success: Boolean,
    val envelope: Envelope? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val closeConnection: Boolean = false
) {
    companion object {
        fun ok(envelope: Envelope) = AndroidDecodeResult(success = true, envelope = envelope)
        fun error(code: String, message: String, closeConnection: Boolean = false) =
            AndroidDecodeResult(success = false, errorCode = code, errorMessage = message, closeConnection = closeConnection)
    }
}

class AndroidFrameDecoder {
    private val buffer = StringBuilder()

    fun feed(chunk: String): List<AndroidDecodeResult> {
        val results = mutableListOf<AndroidDecodeResult>()

        for (ch in chunk) {
            buffer.append(ch)

            if (buffer.length > ProtocolConstants.MAX_FRAME_BYTES) {
                buffer.clear()
                results.add(AndroidDecodeResult.error(ErrorCodes.BAD_FRAME, "Frame exceeded max size of 4096 bytes", closeConnection = true))
                return results
            }

            if (ch == '\n') {
                val line = buffer.toString().trimEnd('\r', '\n')
                buffer.clear()

                if (line.isEmpty()) {
                    continue
                }

                try {
                    val envelope = Envelope.fromJson(line)
                    if (envelope.type.isEmpty()) {
                        results.add(AndroidDecodeResult.error(ErrorCodes.BAD_FRAME, "Malformed envelope JSON"))
                    } else {
                        results.add(AndroidDecodeResult.ok(envelope))
                    }
                } catch (e: Exception) {
                    results.add(AndroidDecodeResult.error(ErrorCodes.BAD_FRAME, "JSON parse error: ${e.message}"))
                }
            }
        }

        return results
    }

    fun reset() {
        buffer.clear()
    }
}

object AndroidFrameEncoder {
    fun encode(envelope: Envelope): String {
        val jsonStr = envelope.toJson().toString()
        val frame = "$jsonStr\n"
        if (frame.toByteArray(Charsets.UTF_8).size > ProtocolConstants.MAX_FRAME_BYTES) {
            throw IllegalStateException("Encoded frame exceeds max allowed size of ${ProtocolConstants.MAX_FRAME_BYTES} bytes")
        }
        return frame
    }
}
