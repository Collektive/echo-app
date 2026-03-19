package it.unibo.collektive.echo.viewmodels

import com.diamondedge.logging.logging
import it.unibo.collektive.echo.location.LocationService
import it.unibo.collektive.echo.models.ChatMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Thin coordinator between the UI and the nearby-devices runtime.
 */
@OptIn(ExperimentalUuidApi::class)
class NearbyDevicesViewModel(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val locationService: LocationService,
) {
    private val log = logging("VIEWMODEL")
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    val deviceId = Uuid.random()

    private val stateStore = NearbyDevicesStateStore(deviceId = deviceId)
    private val messageStore = MessageStore()

    val uiState: StateFlow<NearbyDevicesUiState> = stateStore.uiState

    private val messageTransmission = MessageTransmissionController(
        scope = scope,
        onStateChanged = { isSending, sendingCounter, parameters ->
            stateStore.dispatch(
                NearbyDevicesAction.MessageTransmissionChanged(
                    isSending = isSending,
                    sendingCounter = sendingCounter,
                    parameters = parameters,
                ),
            )
        },
        onConnectionStateChanged = { connectionState ->
            stateStore.dispatch(NearbyDevicesAction.ConnectionChanged(connectionState))
        },
    )

    private val runtime = NearbyDevicesRuntime(
        scope = scope,
        dispatcher = dispatcher,
        deviceId = deviceId,
        messageTransmission = messageTransmission,
        currentLocationProvider = { uiState.value.currentLocation },
        onConnectionStateChanged = { connectionState ->
            stateStore.dispatch(NearbyDevicesAction.ConnectionChanged(connectionState))
        },
        onDevicesDiscovered = { devices ->
            stateStore.dispatch(NearbyDevicesAction.DevicesDiscovered(devices))
        },
        onMessagesReceived = ::appendUniqueMessages,
    )

    private val locationTracker = LocationTracker(
        scope = scope,
        locationService = locationService,
        onLocationChanged = { location ->
            stateStore.dispatch(NearbyDevicesAction.LocationUpdated(location))
        },
        onLocationError = { error ->
            stateStore.dispatch(NearbyDevicesAction.LocationFailed(error))
        },
        onMailboxLocationUpdate = runtime::updateCurrentLocation,
    )

    fun startCollektiveProgram() {
        runtime.start()
    }

    fun sendMessage(
        message: String,
        lifeTime: Double = messageTransmission.parameters.lifeTimeSeconds,
        maxDistanceMeters: Double = messageTransmission.parameters.maxDistanceMeters,
    ) {
        val localMessage = messageTransmission.sendMessage(
            message = message,
            deviceId = deviceId,
            lifeTime = lifeTime,
            maxDistanceMeters = maxDistanceMeters,
        )
        appendUniqueMessages(listOf(localMessage))
    }

    fun updateMessageParameters(lifeTimeSeconds: Double, maxDistanceMeters: Double) {
        messageTransmission.updateParameters(lifeTimeSeconds, maxDistanceMeters)
    }

    fun startLocationTracking() {
        locationTracker.start()
    }

    fun cleanup() {
        runtime.stop()
        messageTransmission.cleanup()
        messageStore.clear()
        stopLocationTracking()
    }

    fun stopLocationTracking() {
        locationTracker.stop()
    }

    private fun appendUniqueMessages(newMessages: List<ChatMessage>) {
        val result = messageStore.appendUnique(newMessages)
        if (newMessages.isEmpty()) {
            return
        }

        result.appendedMessages.forEach { message ->
            log.i {
                "Adding NEW message to UI: '${message.text}' " +
                    "from ${message.sender} (ID: ${message.messageId})"
            }
        }

        val appendedIds = result.appendedMessages.map { it.messageId }.toSet()
        newMessages
            .filterNot { it.messageId in appendedIds }
            .forEach { message ->
                log.i {
                    "Skipping duplicate message: '${message.text}' " +
                        "from ${message.sender} (ID: ${message.messageId})"
                }
            }

        if (result.appendedMessages.isNotEmpty()) {
            stateStore.dispatch(NearbyDevicesAction.MessagesChanged(result.allMessages))
        }
    }
}
