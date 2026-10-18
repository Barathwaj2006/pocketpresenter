package com.pocketpresenter.protocol

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProtocolVectorsTest {

    private fun findVectorsJsonFile(): File {
        var currentDir: File? = File(System.getProperty("user.dir") ?: ".")
        while (currentDir != null) {
            val candidate = File(currentDir, "protocol/fixtures/vectors.json")
            if (candidate.exists()) {
                return candidate
            }
            currentDir = currentDir.parentFile
        }
        throw IllegalStateException("Could not find protocol/fixtures/vectors.json in any parent directory")
    }

    @Test
    fun testAllProtocolVectors() {
        val file = findVectorsJsonFile()
        assertTrue("vectors.json exists", file.exists())

        val jsonContent = file.readText(Charsets.UTF_8)
        val root = JSONObject(jsonContent)

        val defaultSid = root.getJSONObject("session").getString("sid")
        val vectors = root.getJSONArray("vectors")

        for (i in 0 until vectors.length()) {
            val vector = vectors.getJSONObject(i)
            val id = vector.getString("id")
            val handshakeComplete = vector.getBoolean("handshakeComplete")
            val expectedList = vector.getJSONArray("expect")

            val session = AndroidProtocolSession().apply {
                this.handshakeComplete = handshakeComplete
                this.sessionId = if (handshakeComplete) defaultSid else null
            }

            val decoder = AndroidFrameDecoder()
            data class ActualResult(val isOk: Boolean, val type: String?, val seq: Int, val errorCode: String?, val closeConnection: Boolean)
            val actualResults = mutableListOf<ActualResult>()

            if (vector.has("reads")) {
                val reads = vector.getJSONArray("reads")
                for (r in 0 until reads.length()) {
                    val chunk = reads.getString(r)
                    val decodeResults = decoder.feed(chunk)

                    for (dr in decodeResults) {
                        if (!dr.success) {
                            actualResults.add(ActualResult(false, null, 0, dr.errorCode, dr.closeConnection))
                        } else {
                            val vr = ProtocolValidator.validate(dr.envelope!!, session)
                            if (vr.isOk) {
                                session.markSequenceExecuted(dr.envelope.seq)
                                actualResults.add(ActualResult(true, dr.envelope.type, dr.envelope.seq, null, false))
                            } else {
                                actualResults.add(ActualResult(false, dr.envelope.type, dr.envelope.seq, vr.errorCode, vr.closeConnection))
                            }
                        }
                    }
                }
            } else if (vector.has("generate")) {
                val gen = vector.getJSONObject("generate")
                val padToBytes = gen.getInt("padToBytes")
                val padLength = maxOf(0, padToBytes - 120)
                val padString = "x".repeat(padLength)
                var largeJson = "{\"v\":1,\"sid\":\"$defaultSid\",\"seq\":1,\"type\":\"slide.next\",\"payload\":{\"pad\":\"$padString\"}}\n"
                if (largeJson.length < padToBytes) {
                    largeJson = largeJson.padEnd(padToBytes - 1, ' ') + "\n"
                }

                val decodeResults = decoder.feed(largeJson)
                for (dr in decodeResults) {
                    if (!dr.success) {
                        actualResults.add(ActualResult(false, null, 0, dr.errorCode, dr.closeConnection))
                    } else {
                        val vr = ProtocolValidator.validate(dr.envelope!!, session)
                        if (vr.isOk) {
                            actualResults.add(ActualResult(true, dr.envelope.type, dr.envelope.seq, null, false))
                        } else {
                            actualResults.add(ActualResult(false, dr.envelope.type, dr.envelope.seq, vr.errorCode, vr.closeConnection))
                        }
                    }
                }
            }

            assertEquals("Vector $id results count matches expect count", expectedList.length(), actualResults.size)

            for (e in 0 until expectedList.length()) {
                val exp = expectedList.getJSONObject(e)
                val act = actualResults[e]
                val expectedResult = exp.getString("result")

                if (expectedResult == "ok") {
                    assertTrue("Vector $id [$e]: Expected ok, got ${act.errorCode}", act.isOk)
                    if (exp.has("type")) {
                        assertEquals(exp.getString("type"), act.type)
                    }
                    if (exp.has("seq")) {
                        assertEquals(exp.getInt("seq"), act.seq)
                    }
                } else {
                    assertFalse("Vector $id [$e]: Expected error, got ok", act.isOk)
                    if (exp.has("code")) {
                        assertEquals(exp.getString("code"), act.errorCode)
                    }
                    if (exp.has("closeConnection")) {
                        assertEquals(exp.getBoolean("closeConnection"), act.closeConnection)
                    }
                }
            }
        }
    }
}
