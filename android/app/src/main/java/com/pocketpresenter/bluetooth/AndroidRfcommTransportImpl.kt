package com.pocketpresenter.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Concrete RFCOMM transport implementation utilizing Android BluetoothServerSocket.
 * Operates strictly on Dispatchers.IO background coroutines.
 */
class AndroidRfcommTransportImpl(
    private val bluetoothAdapter: BluetoothAdapter?
) : RfcommTransport {

    companion object {
        const val SERVICE_NAME = "PocketPresenter"
        val SERVICE_UUID: UUID = UUID.fromString("7a52e910-482b-4d43-851f-6a9b400b8c32")
    }

    private val _events = MutableSharedFlow<TransportEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<TransportEvent> = _events.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var serverSocket: BluetoothServerSocket? = null
    private var connectedSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    @SuppressLint("MissingPermission")
    override suspend fun start() = withContext(Dispatchers.IO) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _events.emit(TransportEvent.Closed("Bluetooth adapter is disabled or unavailable"))
            return@withContext
        }

        try {
            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID)
            scope.launch {
                listenLoop()
            }
        } catch (e: Exception) {
            _events.emit(TransportEvent.Closed("Failed to open RFCOMM listener: ${e.message}"))
        }
    }

    private suspend fun listenLoop() = withContext(Dispatchers.IO) {
        try {
            val socket = serverSocket?.accept() ?: return@withContext
            connectedSocket = socket
            outputStream = socket.outputStream

            _events.emit(TransportEvent.Connected(socket.remoteDevice?.address ?: "Unknown"))

            readLoop(socket.inputStream)
        } catch (e: Exception) {
            if (e !is CancellationException) {
                _events.emit(TransportEvent.Closed("Connection error: ${e.message}"))
            }
        } finally {
            close()
        }
    }

    private suspend fun readLoop(inputStream: InputStream) = withContext(Dispatchers.IO) {
        val buffer = ByteArray(4096)
        while (isActive) {
            val bytesRead = inputStream.read(buffer)
            if (bytesRead <= 0) break

            val chunk = ByteArray(bytesRead)
            System.arraycopy(buffer, 0, chunk, 0, bytesRead)
            _events.emit(TransportEvent.FrameReceived(chunk))
        }
        _events.emit(TransportEvent.Closed("Peer disconnected"))
    }

    override suspend fun send(frame: ByteArray) = withContext(Dispatchers.IO) {
        val stream = outputStream ?: throw IllegalStateException("Transport is not connected")
        stream.write(frame)
        stream.flush()
    }

    override fun close() {
        try {
            connectedSocket?.close()
            serverSocket?.close()
        } catch (_: Exception) {
        } finally {
            connectedSocket = null
            serverSocket = null
            outputStream = null
        }
    }
}
