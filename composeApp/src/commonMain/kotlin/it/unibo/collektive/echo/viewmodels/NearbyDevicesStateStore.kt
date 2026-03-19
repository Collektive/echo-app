package it.unibo.collektive.echo.viewmodels

import it.unibo.collektive.echo.location.Location
import it.unibo.collektive.echo.location.LocationError
import it.unibo.collektive.echo.models.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
sealed interface NearbyDevicesAction {
    data class ConnectionChanged(val connection: ConnectionState) : NearbyDevicesAction
    data class DevicesDiscovered(val devices: Set<Uuid>) : NearbyDevicesAction
    data class LocationUpdated(val location: Location) : NearbyDevicesAction
    data class LocationFailed(val error: LocationError) : NearbyDevicesAction
    data class MessagesChanged(val messages: List<ChatMessage>) : NearbyDevicesAction
    data class MessageTransmissionChanged(
        val isSending: Boolean,
        val sendingCounter: Int,
        val parameters: MessageParameters,
    ) : NearbyDevicesAction
}

/** Reducer-backed store for the nearby-devices UI state. */
@OptIn(ExperimentalUuidApi::class)
class NearbyDevicesStateStore(deviceId: Uuid) {
    private val _uiState = MutableStateFlow(NearbyDevicesUiState(deviceId = deviceId))

    val uiState: StateFlow<NearbyDevicesUiState> = _uiState.asStateFlow()

    fun dispatch(action: NearbyDevicesAction) {
        _uiState.value = reduce(_uiState.value, action)
    }

    private fun reduce(
        currentState: NearbyDevicesUiState,
        action: NearbyDevicesAction,
    ): NearbyDevicesUiState = when (action) {
        is NearbyDevicesAction.ConnectionChanged -> currentState.copy(connection = action.connection)
        is NearbyDevicesAction.DevicesDiscovered -> currentState.copy(discoveredDevices = action.devices)
        is NearbyDevicesAction.LocationUpdated -> currentState.copy(
            currentLocation = action.location,
            locationError = null,
        )
        is NearbyDevicesAction.LocationFailed -> currentState.copy(locationError = action.error)
        is NearbyDevicesAction.MessagesChanged -> currentState.copy(messages = action.messages)
        is NearbyDevicesAction.MessageTransmissionChanged -> currentState.copy(
            isSending = action.isSending,
            sendingCounter = action.sendingCounter,
            messageParameters = action.parameters,
        )
    }
}
