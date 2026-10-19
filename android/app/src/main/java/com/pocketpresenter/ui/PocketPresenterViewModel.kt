package com.pocketpresenter.ui

import android.content.Context
import android.view.KeyEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpresenter.input.AndroidInputControllerImpl
import com.pocketpresenter.input.AndroidMouseButton
import com.pocketpresenter.input.TouchpadGestureProcessor
import com.pocketpresenter.presentation.AndroidPresentationControllerImpl
import com.pocketpresenter.protocol.Envelope
import com.pocketpresenter.protocol.MessageTypes
import com.pocketpresenter.service.PresentationForegroundService
import com.pocketpresenter.session.AndroidSessionManagerImpl
import com.pocketpresenter.session.SessionState
import com.pocketpresenter.settings.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class AppScreen {
    SETUP,
    HOME,
    CONTROLLER,
    SETTINGS,
    POCKET_MODE_PROMPT
}

enum class ControllerMode {
    TOUCHPAD,
    SLIDES
}

/**
 * Main application ViewModel.
 * Retains state across screen orientation changes and process recreation.
 * Implements the State-Driven Controls model from docs/app_flow.md §3.
 */
class PocketPresenterViewModel(
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope = coroutineScope ?: viewModelScope

    // Outbound envelope dispatcher (hook for transport or mock)
    var onSendEnvelope: ((Envelope) -> Unit)? = null
    var onStartTransport: (() -> Unit)? = null
    var onStopTransport: (() -> Unit)? = null

    fun setConnectedDeviceName(name: String?) {
        _connectedDeviceName.value = name
    }

    val sessionManager: AndroidSessionManagerImpl = AndroidSessionManagerImpl(
        onSendEnvelope = { env -> onSendEnvelope?.invoke(env) },
        onClearInputs = { inputController.clearState() }
    )

    val inputController: AndroidInputControllerImpl = AndroidInputControllerImpl(
        sessionManager = sessionManager,
        onSendEnvelope = { env -> onSendEnvelope?.invoke(env) }
    )

    val presentationController: AndroidPresentationControllerImpl = AndroidPresentationControllerImpl(
        sessionManager = sessionManager,
        onSendEnvelope = { env -> onSendEnvelope?.invoke(env) }
    )

    val gestureProcessor: TouchpadGestureProcessor = TouchpadGestureProcessor(
        inputController = inputController
    )

    val sessionState: StateFlow<SessionState> = sessionManager.state

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _controllerMode = MutableStateFlow(ControllerMode.TOUCHPAD)
    val controllerMode: StateFlow<ControllerMode> = _controllerMode.asStateFlow()

    private val _userSettings = MutableStateFlow(UserSettings())
    val userSettings: StateFlow<UserSettings> = _userSettings.asStateFlow()

    private val _currentError = MutableStateFlow<AppError?>(null)
    val currentError: StateFlow<AppError?> = _currentError.asStateFlow()

    private val _isDragLocked = MutableStateFlow(false)
    val isDragLocked: StateFlow<Boolean> = _isDragLocked.asStateFlow()

    private val _pocketModeActive = MutableStateFlow(false)
    val pocketModeActive: StateFlow<Boolean> = _pocketModeActive.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>("Windows PowerPoint PC")
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    init {
        PresentationForegroundService.onVolumeButtonAction = { direction ->
            if (sessionState.value is SessionState.PresentingArmed) {
                if (direction > 0) {
                    presentationController.nextSlide()
                } else if (direction < 0) {
                    presentationController.previousSlide()
                }
            }
        }

        // Observe session state changes to update gesture processor, settings, and foreground notifications
        scope.launch {
            sessionState.collect { state ->
                when (state) {
                    is SessionState.Disconnected -> {
                        inputController.clearState()
                        if (_isDragLocked.value) {
                            _isDragLocked.value = false
                        }
                    }
                    is SessionState.Disconnecting -> {
                        inputController.clearState()
                    }
                    is SessionState.PresentingArmed -> {
                        if (_currentScreen.value == AppScreen.HOME) {
                            _currentScreen.value = AppScreen.CONTROLLER
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setControllerMode(mode: ControllerMode) {
        _controllerMode.value = mode
    }

    fun startSession(context: Context? = null) {
        sessionManager.startSession()
        context?.let {
            PresentationForegroundService.start(it, "Waiting for PowerPoint")
        }
        onStartTransport?.invoke()
    }

    fun stopSession(context: Context? = null) {
        sessionManager.stopSession()
        context?.let {
            PresentationForegroundService.stop(it)
        }
        onStopTransport?.invoke()
    }

    /**
     * Live effect selection (P7-04).
     * If session is in ConnectedIdle or PresentingArmed, sends effect.set envelope live.
     */
    fun selectEffect(effectId: String) {
        val updated = _userSettings.value.copy(selectedEffect = effectId)
        _userSettings.value = updated
        dispatchLiveEffect(updated)
    }

    fun updateEffectIntensity(intensity: Float) {
        val updated = _userSettings.value.copy(effectIntensity = intensity.coerceIn(0.1f, 1.0f))
        _userSettings.value = updated
        dispatchLiveEffect(updated)
    }

    fun updateReducedMotion(reducedMotion: Boolean) {
        val updated = _userSettings.value.copy(reducedMotion = reducedMotion)
        _userSettings.value = updated
        dispatchLiveEffect(updated)
    }

    fun updateTouchpadSensitivity(sensitivity: Float) {
        val coerced = sensitivity.coerceIn(0.2f, 3.0f)
        _userSettings.value = _userSettings.value.copy(touchpadSensitivity = coerced)
        gestureProcessor.sensitivity = coerced
    }

    fun updateReverseScroll(reverse: Boolean) {
        _userSettings.value = _userSettings.value.copy(reverseScroll = reverse)
        gestureProcessor.reverseScroll = reverse
    }

    fun updateHaptics(enabled: Boolean) {
        _userSettings.value = _userSettings.value.copy(hapticsEnabled = enabled)
    }

    private fun dispatchLiveEffect(settings: UserSettings) {
        val state = sessionState.value
        if (state is SessionState.ConnectedIdle || state is SessionState.PresentingArmed) {
            val payload = JSONObject().apply {
                put("effect", settings.selectedEffect)
                put("intensity", settings.effectIntensity.toDouble())
                put("reducedMotion", settings.reducedMotion)
            }
            val envelope = sessionManager.createOutboundEnvelope(MessageTypes.EFFECT_SET, payload)
            if (envelope != null) {
                onSendEnvelope?.invoke(envelope)
            }
        }
    }

    /**
     * Drag lock toggle. Explicit click & drag without needing long press hold.
     */
    fun toggleDragLock() {
        if (!isTouchpadControlsEnabled()) return

        val willLock = !_isDragLocked.value
        _isDragLocked.value = willLock
        if (willLock) {
            inputController.sendButtonDown(AndroidMouseButton.LEFT)
        } else {
            inputController.sendButtonUp(AndroidMouseButton.LEFT)
        }
    }

    fun setPocketMode(enabled: Boolean) {
        _pocketModeActive.value = enabled
    }

    fun dismissError() {
        _currentError.value = null
    }

    fun reportError(error: AppError) {
        _currentError.value = error
    }

    /**
     * Volume button handling conforming to docs/app_flow.md §3 & Screen H.
     * Only sends next/previous when session is PresentingArmed.
     * Returns true when consumed.
     */
    fun handleVolumeKeyEvent(keyCode: Int, action: Int): Boolean {
        if (action != KeyEvent.ACTION_DOWN) return false

        if (sessionState.value !is SessionState.PresentingArmed) {
            // Do not send volume keys when not presenting
            return false
        }

        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                presentationController.nextSlide()
                true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                presentationController.previousSlide()
                true
            }
            else -> false
        }
    }

    /**
     * Checks whether touchpad and slide controls are currently enabled
     * according to the state-driven controls table in docs/app_flow.md §3.
     */
    fun isTouchpadControlsEnabled(): Boolean {
        return sessionState.value is SessionState.PresentingArmed
    }

    fun getPhoneStatusLabel(): String {
        return when (sessionState.value) {
            is SessionState.Disconnected -> "Not connected"
            is SessionState.PhoneReady -> "Waiting for PowerPoint"
            is SessionState.Connecting -> "Connecting…"
            is SessionState.ConnectedIdle -> "Connected — start the slide show"
            is SessionState.PresentingArmed -> "Presenting"
            is SessionState.Disconnecting -> "Ending session…"
        }
    }
}
