package com.pocketpresenter.session

import com.pocketpresenter.protocol.AndroidHeartbeatManager
import com.pocketpresenter.protocol.Envelope
import com.pocketpresenter.protocol.MessageTypes
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidSessionStateMachineTest {

    @Test
    fun sessionManager_NegotiatesHandshake_AndCleansUpOnDisconnect() {
        var sentEnvelope: Envelope? = null
        var cleanupCalled = false

        val manager = AndroidSessionManagerImpl(
            onSendEnvelope = { sentEnvelope = it },
            onClearInputs = { cleanupCalled = true }
        )

        // 1. Initial state
        assertTrue(manager.state.value is SessionState.Disconnected)

        // 2. Start session -> PhoneReady
        manager.startSession()
        assertTrue(manager.state.value is SessionState.PhoneReady)
        assertNull(manager.currentSessionId)

        // 3. Receive hello from add-in
        val helloEnvelope = Envelope(
            v = 1,
            seq = 0,
            type = MessageTypes.HELLO,
            payload = JSONObject("{\"client\":\"addin\",\"appVersion\":\"0.1.0\"}")
        )

        manager.handleInboundEnvelope(helloEnvelope)

        // Verifications after hello
        assertTrue(manager.state.value is SessionState.ConnectedIdle)
        assertNotNull(manager.currentSessionId)
        val generatedSid = manager.currentSessionId

        assertNotNull(sentEnvelope)
        assertEquals(MessageTypes.HELLO_ACK, sentEnvelope!!.type)
        assertEquals(0, sentEnvelope!!.seq)
        val ackPayload = sentEnvelope!!.payload
        assertTrue(ackPayload.getBoolean("accepted"))
        assertEquals(generatedSid, ackPayload.getString("sessionId"))

        // 4. Create outbound slide.next envelope -> must carry sid and increment seq
        val nextEnv = manager.createOutboundEnvelope(MessageTypes.SLIDE_NEXT)
        assertNotNull(nextEnv)
        assertEquals(generatedSid, nextEnv!!.sid)
        assertEquals(1, nextEnv.seq)

        // 5. Disconnect -> cleans up and resets
        manager.disconnect()
        assertTrue(manager.state.value is SessionState.Disconnected)
        assertNull(manager.currentSessionId)
        assertTrue(cleanupCalled)
    }

    @Test
    fun heartbeatManager_EnforcesFastPingWhenButtonHeld_AndDeadLink() {
        var pingCount = 0
        var linkDeadCalled = false

        val hb = AndroidHeartbeatManager(
            onSendPing = { pingCount++ },
            onLinkDead = { linkDeadCalled = true }
        )

        val t0 = 1000L
        hb.reset(t0)

        // 1. Normal interval (5s) -> at t0 + 2s no ping
        hb.tick(t0 + 2000L)
        assertEquals(0, pingCount)

        // 2. Button held -> fast interval (1s) -> at t0 + 1.1s ping is sent
        hb.isButtonHeld = true
        hb.tick(t0 + 1100L)
        assertEquals(1, pingCount)

        // 3. 16s without inbound traffic -> dead link triggers
        hb.tick(t0 + 17000L)
        assertTrue(linkDeadCalled)
    }
}
