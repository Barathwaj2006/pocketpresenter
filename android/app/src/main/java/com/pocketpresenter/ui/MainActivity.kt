package com.pocketpresenter.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.pocketpresenter.bluetooth.AndroidRfcommTransportImpl
import com.pocketpresenter.bluetooth.TransportEvent
import com.pocketpresenter.input.AndroidMouseButton
import com.pocketpresenter.protocol.AndroidFrameDecoder
import com.pocketpresenter.protocol.AndroidFrameEncoder
import com.pocketpresenter.ui.components.ErrorBanner
import com.pocketpresenter.ui.components.PocketModeDialog
import com.pocketpresenter.ui.screens.ControllerScreen
import com.pocketpresenter.ui.screens.HomeScreen
import com.pocketpresenter.ui.screens.SettingsScreen
import com.pocketpresenter.ui.screens.SetupScreen
import com.pocketpresenter.ui.theme.GraphiteBackground
import com.pocketpresenter.ui.theme.PocketPresenterTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * PocketPresenter Main Activity.
 * Intercepts physical volume keys for slide advancement when presenting.
 * Retains ViewModel state across screen rotation and process recreation (P7-10).
 */
class MainActivity : ComponentActivity() {

    private val viewModel: PocketPresenterViewModel by viewModels()

    private var hasBluetoothPermission by mutableStateOf(false)
    private var isBluetoothEnabled by mutableStateOf(true)
    private var showPocketModeDialog by mutableStateOf(false)

    private var rfcommTransport: AndroidRfcommTransportImpl? = null
    private val frameDecoder = AndroidFrameDecoder()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasBluetoothPermission = isGranted
        if (!isGranted) {
            viewModel.reportError(ErrorCatalog.BLUETOOTH_PERMISSION_DENIED)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissionsAndBluetooth()
        setupTransport()

        setContent {
            PocketPresenterTheme {
                PocketPresenterMainApp(
                    viewModel = viewModel,
                    hasBluetoothPermission = hasBluetoothPermission,
                    isBluetoothEnabled = isBluetoothEnabled,
                    showPocketModeDialog = showPocketModeDialog,
                    onRequestPermission = { requestBluetoothPermission() },
                    onOpenPocketModeDialog = { showPocketModeDialog = true },
                    onDismissPocketModeDialog = { showPocketModeDialog = false },
                    onAction = { actionType -> handleAppAction(actionType) }
                )
            }
        }
    }

    private fun setupTransport() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        val transport = AndroidRfcommTransportImpl(adapter)
        rfcommTransport = transport

        viewModel.onStartTransport = {
            lifecycleScope.launch(Dispatchers.IO) {
                transport.start()
            }
        }

        viewModel.onStopTransport = {
            transport.close()
        }

        viewModel.onSendEnvelope = { envelope ->
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val frame = AndroidFrameEncoder.encode(envelope)
                    transport.send(frame.toByteArray(Charsets.UTF_8))
                } catch (_: Exception) {}
            }
        }

        lifecycleScope.launch {
            transport.events.collect { event ->
                when (event) {
                    is TransportEvent.Connected -> {
                        viewModel.setConnectedDeviceName(event.peerAddress)
                    }
                    is TransportEvent.FrameReceived -> {
                        val text = String(event.data, Charsets.UTF_8)
                        val results = frameDecoder.feed(text)
                        for (res in results) {
                            if (res.success && res.envelope != null) {
                                viewModel.sessionManager.handleInboundEnvelope(res.envelope)
                            }
                        }
                    }
                    is TransportEvent.Closed -> {
                        viewModel.sessionManager.disconnect()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        rfcommTransport?.close()
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndBluetooth()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (viewModel.handleVolumeKeyEvent(keyCode, KeyEvent.ACTION_DOWN)) {
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (viewModel.isTouchpadControlsEnabled()) {
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun checkPermissionsAndBluetooth() {
        hasBluetoothPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val adapter = BluetoothAdapter.getDefaultAdapter()
        isBluetoothEnabled = adapter?.isEnabled ?: false
    }

    private fun requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    private fun handleAppAction(actionType: AppError.ActionType) {
        when (actionType) {
            AppError.ActionType.ENABLE_BLUETOOTH -> {
                startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            }
            AppError.ActionType.REQUEST_PERMISSION -> {
                requestBluetoothPermission()
            }
            AppError.ActionType.OPEN_APP_SETTINGS -> {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            AppError.ActionType.OPEN_BLUETOOTH_SETTINGS -> {
                startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            }
            AppError.ActionType.RECONNECT -> {
                viewModel.startSession(this)
            }
            AppError.ActionType.VIEW_SUPPORTED_DEVICES -> {
                viewModel.navigateTo(AppScreen.SETTINGS)
            }
            AppError.ActionType.DISMISS -> {
                viewModel.dismissError()
            }
        }
    }
}

@Composable
fun PocketPresenterMainApp(
    viewModel: PocketPresenterViewModel,
    hasBluetoothPermission: Boolean,
    isBluetoothEnabled: Boolean,
    showPocketModeDialog: Boolean,
    onRequestPermission: () -> Unit,
    onOpenPocketModeDialog: () -> Unit,
    onDismissPocketModeDialog: () -> Unit,
    onAction: (AppError.ActionType) -> Unit
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val sessionState by viewModel.sessionState.collectAsState()
    val controllerMode by viewModel.controllerMode.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val currentError by viewModel.currentError.collectAsState()
    val isDragLocked by viewModel.isDragLocked.collectAsState()
    val connectedDeviceName by viewModel.connectedDeviceName.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        containerColor = GraphiteBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.SETUP -> {
                    SetupScreen(
                        hasBluetoothPermission = hasBluetoothPermission,
                        isBluetoothEnabled = isBluetoothEnabled,
                        onRequestPermission = onRequestPermission,
                        onContinue = { viewModel.navigateTo(AppScreen.HOME) }
                    )
                }

                AppScreen.HOME -> {
                    HomeScreen(
                        sessionState = sessionState,
                        phoneStatusLabel = viewModel.getPhoneStatusLabel(),
                        selectedEffectId = userSettings.selectedEffect,
                        effectIntensity = userSettings.effectIntensity,
                        reducedMotion = userSettings.reducedMotion,
                        onStartSession = { viewModel.startSession(context) },
                        onStopSession = { viewModel.stopSession(context) },
                        onNavigateToController = { viewModel.navigateTo(AppScreen.CONTROLLER) },
                        onNavigateToSettings = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        onNavigateToSetup = { viewModel.navigateTo(AppScreen.SETUP) },
                        onOpenPocketModeDialog = onOpenPocketModeDialog,
                        onSelectEffect = { viewModel.selectEffect(it) },
                        onUpdateIntensity = { viewModel.updateEffectIntensity(it) },
                        onUpdateReducedMotion = { viewModel.updateReducedMotion(it) }
                    )
                }

                AppScreen.CONTROLLER -> {
                    ControllerScreen(
                        sessionState = sessionState,
                        phoneStatusLabel = viewModel.getPhoneStatusLabel(),
                        connectedDeviceName = connectedDeviceName,
                        controllerMode = controllerMode,
                        selectedEffectId = userSettings.selectedEffect,
                        isDragLocked = isDragLocked,
                        gestureProcessor = viewModel.gestureProcessor,
                        onSetControllerMode = { viewModel.setControllerMode(it) },
                        onSelectEffect = { viewModel.selectEffect(it) },
                        onToggleDragLock = { viewModel.toggleDragLock() },
                        onSendLeftClick = {
                            viewModel.inputController.sendButtonDown(AndroidMouseButton.LEFT)
                            viewModel.inputController.sendButtonUp(AndroidMouseButton.LEFT)
                        },
                        onSendRightClick = {
                            viewModel.inputController.sendButtonDown(AndroidMouseButton.RIGHT)
                            viewModel.inputController.sendButtonUp(AndroidMouseButton.RIGHT)
                        },
                        onNextSlide = { viewModel.presentationController.nextSlide() },
                        onPreviousSlide = { viewModel.presentationController.previousSlide() },
                        onNavigateToHome = { viewModel.navigateTo(AppScreen.HOME) },
                        onNavigateToSettings = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        onOpenPocketMode = onOpenPocketModeDialog
                    )
                }

                AppScreen.SETTINGS -> {
                    SettingsScreen(
                        settings = userSettings,
                        onUpdateSensitivity = { viewModel.updateTouchpadSensitivity(it) },
                        onUpdateReverseScroll = { viewModel.updateReverseScroll(it) },
                        onUpdateHaptics = { viewModel.updateHaptics(it) },
                        onBack = { viewModel.navigateTo(AppScreen.HOME) }
                    )
                }

                AppScreen.POCKET_MODE_PROMPT -> {
                    // Rendered via dialog
                }
            }

            // Error Banner (docs/app_flow.md §4)
            currentError?.let { err ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    ErrorBanner(
                        error = err,
                        onAction = { actionType ->
                            viewModel.dismissError()
                            onAction(actionType)
                        },
                        onDismiss = { viewModel.dismissError() }
                    )
                }
            }

            // Pocket Mode Dialog (Screen H)
            if (showPocketModeDialog) {
                PocketModeDialog(
                    onDismiss = onDismissPocketModeDialog,
                    onConfirmPocketMode = {
                        viewModel.setPocketMode(true)
                    }
                )
            }
        }
    }
}
