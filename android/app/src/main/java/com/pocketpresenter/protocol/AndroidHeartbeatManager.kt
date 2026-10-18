package com.pocketpresenter.protocol

import kotlin.random.Random

/**
 * Android heartbeat manager implementing 5s normal ping, 1s fast ping while button/touch held (ADR-0013),
 * and 15s dead link detection.
 */
class AndroidHeartbeatManager(
    private val onSendPing: (timestamp: Long) -> Unit,
    private val onLinkDead: () -> Unit
) {
    var isButtonHeld: Boolean = false

    private var lastInboundTimestampMs: Long = 0L
    private var lastPingSentTimestampMs: Long = 0L

    fun reset(nowMs: Long = System.currentTimeMillis()) {
        lastInboundTimestampMs = nowMs
        lastPingSentTimestampMs = nowMs
        isButtonHeld = false
    }

    fun recordInboundTraffic(nowMs: Long = System.currentTimeMillis()) {
        lastInboundTimestampMs = nowMs
    }

    fun tick(nowMs: Long = System.currentTimeMillis()) {
        // 1. Check dead link (15s)
        if (lastInboundTimestampMs > 0 && (nowMs - lastInboundTimestampMs) > 15000L) {
            onLinkDead()
            return
        }

        // 2. Check ping interval (1s if button held, 5s otherwise)
        val interval = if (isButtonHeld) 1000L else 5000L
        if (nowMs - lastPingSentTimestampMs >= interval) {
            lastPingSentTimestampMs = nowMs
            onSendPing(nowMs)
        }
    }
}

/**
 * Bounded exponential backoff policy for Android reconnection attempts.
 */
class AndroidTimeoutBackoff(
    private val initialDelayMs: Long = 1000L,
    private val maxDelayMs: Long = 10000L,
    private val multiplier: Double = 2.0
) {
    var attempt: Int = 0
        private set

    var currentDelayMs: Long = 0L
        private set

    fun nextDelay(): Long {
        attempt++
        val rawDelay = (initialDelayMs * Math.pow(multiplier, (attempt - 1).toDouble())).toLong()
        val boundedDelay = if (rawDelay > maxDelayMs) maxDelayMs else rawDelay

        // 10% jitter
        val jitter = 1.0 + (Random.nextDouble() * 0.2 - 0.1)
        currentDelayMs = (boundedDelay * jitter).toLong()
        return currentDelayMs
    }

    fun reset() {
        attempt = 0
        currentDelayMs = 0L
    }
}
