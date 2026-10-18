package com.pocketpresenter.protocol

import org.json.JSONObject

data class Envelope(
    val v: Int = ProtocolConstants.CURRENT_PROTOCOL_VERSION,
    val sid: String? = null,
    val seq: Int = 0,
    val type: String,
    val payload: JSONObject = JSONObject()
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("v", v)
        if (sid != null) {
            json.put("sid", sid)
        }
        json.put("seq", seq)
        json.put("type", type)
        json.put("payload", payload)
        return json
    }

    companion object {
        fun fromJson(jsonStr: String): Envelope {
            val obj = JSONObject(jsonStr)
            val v = obj.optInt("v", 1)
            val sid = if (obj.has("sid")) obj.getString("sid") else null
            val seq = obj.optInt("seq", 0)
            val type = obj.optString("type", "")
            val payload = obj.optJSONObject("payload") ?: JSONObject()
            return Envelope(v = v, sid = sid, seq = seq, type = type, payload = payload)
        }
    }
}
