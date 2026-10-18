package com.pocketpresenter.bluetooth

import kotlinx.coroutines.flow.Flow

sealed interface TransportEvent {
    data class Connected(val peerAddress: String) : TransportEvent
    data class FrameReceived(val data: ByteArray) : TransportEvent
    data class Closed(val reason: String) : TransportEvent
}

/**
 * RFCOMM Bluetooth Classic transport interface.
 * Implements user-started session listener and send pipeline.
 */
interface RfcommTransport {
    val events: Flow<TransportEvent>

    suspend fun start()
    suspend fun send(frame: ByteArray)
    fun close()
}
