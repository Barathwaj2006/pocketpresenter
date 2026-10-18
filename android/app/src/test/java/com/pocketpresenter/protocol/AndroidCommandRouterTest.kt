package com.pocketpresenter.protocol

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class AndroidCommandRouterTest {

    @Test
    fun commandRouter_DispatchesStateUpdateAndAcks() {
        var lastLink = ""
        var lastShow = ""
        var lastAckRef = -1
        var lastAckOk = false

        val listener = object : AndroidStateUpdateListener {
            override fun onStateUpdate(link: String, show: String, effect: String, slide: Int, slideCount: Int) {
                lastLink = link
                lastShow = show
            }

            override fun onCommandAck(refSeq: Int, ok: Boolean) {
                lastAckRef = refSeq
                lastAckOk = ok
            }

            override fun onCommandError(refSeq: Int, code: String, message: String) { }

            override fun onPong(timestamp: Long) { }
        }

        val router = AndroidCommandRouter(listener)
        val sid = UUID.randomUUID().toString()
        val session = AndroidProtocolSession().apply {
            handshakeComplete = true
            sessionId = sid
        }

        // 1. state.update
        val statePayload = JSONObject("{\"link\":\"connected_idle\",\"show\":\"running\",\"effect\":\"honeycomb\",\"slide\":2,\"slideCount\":10}")
        val stateEnv = Envelope(v = 1, sid = sid, seq = 1, type = MessageTypes.STATE_UPDATE, payload = statePayload)

        val res1 = router.route(stateEnv, session)
        assertTrue(res1.isOk)
        assertEquals("connected_idle", lastLink)
        assertEquals("running", lastShow)

        // 2. command.ack
        val ackPayload = JSONObject("{\"ref\":42,\"ok\":true}")
        val ackEnv = Envelope(v = 1, sid = sid, seq = 2, type = MessageTypes.COMMAND_ACK, payload = ackPayload)

        val res2 = router.route(ackEnv, session)
        assertTrue(res2.isOk)
        assertEquals(42, lastAckRef)
        assertTrue(lastAckOk)
    }
}
