package com.pocketpresenter.session

import kotlinx.coroutines.flow.StateFlow

sealed interface SessionState {
    data object Disconnected : SessionState
    data object PhoneReady : SessionState
    data object Connecting : SessionState
    data object ConnectedIdle : SessionState
    data object PresentingArmed : SessionState
    data object Disconnecting : SessionState
}

interface SessionManager {
    val state: StateFlow<SessionState>
    val currentSessionId: String?

    fun startSession()
    fun stopSession()
    fun updateState(newState: SessionState)
}
