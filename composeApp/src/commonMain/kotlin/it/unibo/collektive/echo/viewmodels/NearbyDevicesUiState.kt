package it.unibo.collektive.echo.viewmodels

import it.unibo.collektive.echo.DEFAULT_MAX_DISTANCE
import it.unibo.collektive.echo.DEFAULT_MAX_TIME
import it.unibo.collektive.echo.location.Location
import it.unibo.collektive.echo.location.LocationError
import it.unibo.collektive.echo.models.ChatMessage
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** User-editable parameters for outgoing gossip messages. */
data class MessageParameters(
    val lifeTimeSeconds: Double = DEFAULT_MAX_TIME,
    val maxDistanceMeters: Double = DEFAULT_MAX_DISTANCE,
)

/** Aggregate UI state for the nearby-devices screen. */
@OptIn(ExperimentalUuidApi::class)
data class NearbyDevicesUiState(
    val deviceId: Uuid,
    val connection: ConnectionState = ConnectionState.DISCONNECTED,
    val discoveredDevices: Set<Uuid> = emptySet(),
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false,
    val sendingCounter: Int = 0,
    val currentLocation: Location? = null,
    val locationError: LocationError? = null,
    val messageParameters: MessageParameters = MessageParameters(),
)
