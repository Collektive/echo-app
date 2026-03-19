package it.unibo.collektive.echo.viewmodels

import com.diamondedge.logging.logging
import it.unibo.collektive.Collektive
import it.unibo.collektive.aggregate.Field
import it.unibo.collektive.aggregate.api.neighboring
import it.unibo.collektive.aggregate.ids
import it.unibo.collektive.aggregate.toMap
import it.unibo.collektive.echo.MQTT_HOST
import it.unibo.collektive.echo.gossip.Message
import it.unibo.collektive.echo.gossip.chatMultipleSources
import it.unibo.collektive.echo.location.Location
import it.unibo.collektive.echo.models.ChatMessage
import it.unibo.collektive.echo.network.mqtt.MqttMailbox
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Owns the Collektive runtime loop, mailbox lifecycle, and gossip-to-UI translation. */
@OptIn(ExperimentalUuidApi::class)
class NearbyDevicesRuntime(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val deviceId: Uuid,
    private val messageTransmission: MessageTransmissionController,
    private val currentLocationProvider: () -> Location?,
    private val onConnectionStateChanged: (ConnectionState) -> Unit,
    private val onDevicesDiscovered: (Set<Uuid>) -> Unit,
    private val onMessagesReceived: (List<ChatMessage>) -> Unit,
) {
    private val log = logging("NEARBY-RUNTIME")
    private var collektiveJob: Job? = null
    private var mqttMailbox: MqttMailbox? = null

    @OptIn(ExperimentalTime::class)
    fun start() {
        if (collektiveJob?.isActive == true) {
            return
        }

        collektiveJob = scope.launch {
            log.i { "Starting Collektive program..." }

            while (currentLocationProvider() == null) {
                log.i { "Waiting for GPS location before starting Collektive program..." }
                delay(GPS_POLL_INTERVAL_MS)
            }

            onConnectionStateChanged(ConnectionState.CONNECTED)
            val program = createProgram()
            log.i { "Collektive program started with GPS location: ${currentLocationProvider()}" }

            while (currentCoroutineContext().isActive) {
                val (newDevices, newMessages) = program.cycle()
                onDevicesDiscovered(newDevices)
                onMessagesReceived(newMessages)
                delay(1.seconds)
            }
        }
    }

    fun stop() {
        collektiveJob?.cancel()
        collektiveJob = null

        val mailbox = mqttMailbox
        mqttMailbox = null
        scope.launch {
            mailbox?.close()
        }
    }

    fun updateCurrentLocation(location: Location) {
        mqttMailbox?.updateCurrentLocation(location)
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun createProgram(): Collektive<Uuid, Pair<Set<Uuid>, List<ChatMessage>>> {
        val initialLocation = checkNotNull(currentLocationProvider()) {
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
            val neighborMap = neighboring(localId)
            val neighbors = neighborMap.neighbors.ids.set
            val currentTime = Clock.System.now().epochSeconds.toDouble()
            val activeSession = messageTransmission.activeSession(currentTime)
            val messageParameters = activeSession?.parameters ?: messageTransmission.parameters

            log.i { activeSession != null }

            activeSession?.let { outgoingMessage ->
                log.i {
                    "Device is source: sending message '${outgoingMessage.text}' " +
                        "(${currentTime - outgoingMessage.startedAtEpochSeconds}s elapsed)"
                }
            }

            val distances = calculateNeighborDistances(neighborMap)
            val currentLocation = checkNotNull(currentLocationProvider()) {
                "GPS location is required but not available"
            }

            log.i { "Current GPS location: ${currentLocation.latitude}, ${currentLocation.longitude}" }
            log.i { "GPS-based distances calculated: ${distances.all.toMap()}" }

            val allSourceMessages = chatMultipleSources(
                distances = distances,
                isSource = activeSession != null,
                currentTime = activeSession?.let { currentTime - it.startedAtEpochSeconds } ?: 0.0,
                content = activeSession?.text.orEmpty(),
                messageId = activeSession?.messageId ?: deviceId,
                lifeTime = messageParameters.lifeTimeSeconds,
                maxDistance = messageParameters.maxDistanceMeters,
            )

            Pair(neighbors, allSourceMessages.toChatMessages())
        }
    }

    private fun calculateNeighborDistances(neighborMap: Field<Uuid, *>): Field<Uuid, Double> {
        val currentLocation = checkNotNull(currentLocationProvider()) {
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

        return neighborMap.map { neighborId ->
            distancesMap[neighborId.id] ?: Double.MAX_VALUE
        }
    }

    private fun Map<Uuid, Message>.toChatMessages(): List<ChatMessage> = mapNotNull { (senderId, message) ->
        if (message.content.isEmpty()) {
            null
        } else {
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
        }
    }.also { chatMessages ->
        log.i { "Final messages: ${chatMessages.size} total, senders: ${chatMessages.map { it.sender }}" }
    }

    private companion object {
        const val GPS_POLL_INTERVAL_MS = 500L
    }
}
