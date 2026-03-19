package it.unibo.collektive.echo.viewmodels

import com.diamondedge.logging.logging
import it.unibo.collektive.Collektive
import it.unibo.collektive.aggregate.Field
import it.unibo.collektive.aggregate.api.neighboring
import it.unibo.collektive.aggregate.ids
import it.unibo.collektive.aggregate.toMap
import it.unibo.collektive.echo.MQTT_HOST
import it.unibo.collektive.echo.gossip.chatMultipleSources
import it.unibo.collektive.echo.location.LocationService
import it.unibo.collektive.echo.models.ChatMessage
import it.unibo.collektive.echo.network.mqtt.MqttMailbox
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * ViewModel for managing nearby devices, location tracking [locationService], and message sending.
 *
 */
@OptIn(ExperimentalUuidApi::class)
class NearbyDevicesViewModel(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val locationService: LocationService,
) {
    /** Logger instance for ViewModel diagnostics. */
    private val log = logging("VIEWMODEL")

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    /** Randomly-generated unique identifier for this device instance. */
    @OptIn(ExperimentalUuidApi::class)
    val deviceId = Uuid.random()
    private val _uiState = MutableStateFlow(NearbyDevicesUiState(deviceId = deviceId))

    /** Aggregate state consumed by the nearby-devices UI. */
    val uiState: StateFlow<NearbyDevicesUiState> = _uiState.asStateFlow()

    private val messageTransmission = MessageTransmissionController(
        scope = scope,
        onStateChanged = { isSending, sendingCounter, parameters ->
            updateUiState {
                copy(
                    isSending = isSending,
                    sendingCounter = sendingCounter,
                    messageParameters = parameters,
                )
            }
        },
        onConnectionStateChanged = { connectionState ->
            updateUiState { copy(connection = connectionState) }
        },
    )

    private val locationTracker = LocationTracker(
        scope = scope,
        locationService = locationService,
        onLocationChanged = { location ->
            updateUiState {
                copy(
                    currentLocation = location,
                    locationError = null,
                )
            }
        },
        onLocationError = { error ->
            updateUiState { copy(locationError = error) }
        },
        onMailboxLocationUpdate = { location ->
            mqttMailbox?.updateCurrentLocation(location)
        },
    )

    private var mqttMailbox: MqttMailbox? = null
    private var collektiveJob: Job? = null

    /**
     * Collektive program using MQTT mailbox and GPS location for proximity-based messaging.
     */
    @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
    private suspend fun collektiveProgram(): Collektive<Uuid, Pair<Set<Uuid>, List<ChatMessage>>> {
        // Wait for GPS location to be available before creating MQTT mailbox
        val initialLocation = checkNotNull(uiState.value.currentLocation) {
            "GPS location is required but not available for MQTT initialization"
        }

        return Collektive(
            deviceId,
            MqttMailbox(
                deviceId = deviceId,
                initialLocation = initialLocation,
                host = MQTT_HOST,
                dispatcher = dispatcher,
            ).also { mqttMailbox = it },
        ) {
            // Get neighboring devices
            val neighborMap = neighboring(localId)
            val neighbors = neighborMap.neighbors.ids.set

            // Get current time
            val currentTime = Clock.System.now().epochSeconds.toDouble()
            val activeSession = messageTransmission.activeSession(currentTime)
            val messageParameters = activeSession?.parameters ?: messageTransmission.parameters

            // Determine if this device is a source (has a message to send)
            val isSource = activeSession != null

            log.i { isSource }

            activeSession?.let { outgoingMessage ->
                log.i {
                    "Device is source: sending message '${outgoingMessage.text}' " +
                        "(${currentTime - outgoingMessage.startedAtEpochSeconds}s elapsed)"
                }
            }

            // Calculate distances to neighboring devices using GPS coordinates
            val distances = calculateNeighborDistances(neighborMap)
            val currentLocation = checkNotNull(uiState.value.currentLocation) {
                "GPS location is required but not available"
            } // GPS is mandatory, should never be null

            log.i { "Current GPS location: ${currentLocation.latitude}, ${currentLocation.longitude}" }

            log.i { "GPS-based distances calculated: ${distances.all.toMap()}" }

            // Collect all messages from all potential sources using chatMultipleSources
            val allSourceMessages = chatMultipleSources(
                distances = distances,
                isSource = isSource,
                currentTime = activeSession?.let { currentTime - it.startedAtEpochSeconds } ?: 0.0,
                content = activeSession?.text.orEmpty(),
                messageId = activeSession?.messageId ?: deviceId,
                lifeTime = messageParameters.lifeTimeSeconds,
                maxDistance = messageParameters.maxDistanceMeters,
            )

            // Convert messages to ChatMessage objects
            val chatMessages = allSourceMessages.mapNotNull { (senderId, message) ->
                if (message.content.isNotEmpty()) {
                    log.i {
                        "Received gossip message from $senderId: " +
                            "'${message.content}' at distance ${message.distanceFromSource}"
                    }
                    ChatMessage(
                        text = message.content,
                        sender = senderId,
                        messageId = message.messageId,
                        distanceFromSource = message.distanceFromSource,
                    )
                } else {
                    null
                }
            }

            log.i { "Final messages: ${chatMessages.size} total, senders: ${chatMessages.map { it.sender }}" }

            Pair(neighbors, chatMessages)
        }
    }

    /**
     * Starts the Collektive program after ensuring GPS location is available.
     */
    @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
    fun startCollektiveProgram() {
        if (collektiveJob?.isActive == true) {
            return
        }

        collektiveJob = scope.launch {
            log.i { "Starting Collektive program..." }

            // Wait for GPS location to be available - GPS is mandatory
            while (uiState.value.currentLocation == null) {
                log.i { "Waiting for GPS location before starting Collektive program..." }
                delay(GpsPollIntervalMs)
            }

            updateUiState { copy(connection = ConnectionState.CONNECTED) }
            val program = collektiveProgram()
            log.i { "Collektive program started with GPS location: ${uiState.value.currentLocation}" }

            while (true) {
                val (newDevices, newMessages) = program.cycle()
                updateUiState { copy(discoveredDevices = newDevices) }
                appendUniqueMessages(newMessages)
                delay(1.seconds)
            }
        }
    }

    /**
     * Send message with content [message], a [lifeTime] and a maximum distance propagation [maxDistanceMeters].
     *
     */
    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
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

    /**
     * Update message parameters for future messages with new [lifeTimeSeconds] and [maxDistanceMeters].
     */
    fun updateMessageParameters(lifeTimeSeconds: Double, maxDistanceMeters: Double) {
        messageTransmission.updateParameters(lifeTimeSeconds, maxDistanceMeters)
    }

    /**
     * Start GPS location tracking, updating location flow and MQTT mailbox.
     */
    fun startLocationTracking() {
        locationTracker.start()
    }

    /**
     * Calculate distances to all neighboring devices [neighborMap] using GPS coordinates shared via MQTT.
     * Excludes the device itself and only processes neighbors with GPS data available.
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun calculateNeighborDistances(neighborMap: Field<Uuid, *>): Field<Uuid, Double> {
        val currentLocation = checkNotNull(uiState.value.currentLocation) {
            "GPS location is required but not available"
        }
        val mailbox = checkNotNull(mqttMailbox) {
            "MQTT mailbox is required but not available"
        }
        val distancesMap = NeighborDistanceCalculator.calculateDistances(
            currentLocation = currentLocation,
            selfId = deviceId,
            neighborIds = neighborMap.neighbors.ids.list,
            neighborLocationProvider = mailbox::getNeighborLocation,
            onDiagnostic = { diagnostic -> log.d { diagnostic } },
        )

        log.i {
            "Processing ${distancesMap.size} actual neighbors with GPS data" +
                " out of ${neighborMap.neighbors.size} total discovered devices"
        }

        // Map all neighbors to their distances, using a very large distance for invalid neighbors
        // The gossip algorithm will naturally ignore neighbors beyond maxDistance
        return neighborMap.map { neighborId ->
            distancesMap[neighborId.id] ?: Double.MAX_VALUE
        }
    }

    /**
     * Cleanup resources when ViewModel is no longer needed.
     */
    fun cleanup() {
        collektiveJob?.cancel()
        collektiveJob = null
        messageTransmission.cleanup()
        val mailbox = mqttMailbox
        mqttMailbox = null
        scope.launch {
            mailbox?.close()
        }
        stopLocationTracking()
    }

    /**
     * Stop GPS location tracking.
     */
    fun stopLocationTracking() {
        locationTracker.stop()
    }

    private fun appendUniqueMessages(newMessages: List<ChatMessage>) {
        if (newMessages.isEmpty()) {
            return
        }

        val currentMessages = uiState.value.messages
        val knownMessageIds = currentMessages.map { it.messageId }.toMutableSet()
        val uniqueMessages = newMessages.filter { message ->
            val isNewMessage = knownMessageIds.add(message.messageId)
            if (isNewMessage) {
                log.i {
                    "Adding NEW message to UI: '${message.text}' " +
                        "from ${message.sender} (ID: ${message.messageId})"
                }
            } else {
                log.i {
                    "Skipping duplicate message: '${message.text}' " +
                        "from ${message.sender} (ID: ${message.messageId})"
                }
            }
            isNewMessage
        }

        if (uniqueMessages.isNotEmpty()) {
            updateUiState { copy(messages = messages + uniqueMessages) }
        }
    }

    private fun updateUiState(transform: NearbyDevicesUiState.() -> NearbyDevicesUiState) {
        _uiState.value = _uiState.value.transform()
    }

    private companion object {
        const val GpsPollIntervalMs = 500L
    }
}
