package it.unibo.collektive.echo.viewmodels

import com.diamondedge.logging.logging
import it.unibo.collektive.echo.models.ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private data class MessageTransmissionState(
    val isSending: Boolean = false,
    val sendingCounter: Int = 0,
    val parameters: MessageParameters = MessageParameters(),
)

@OptIn(ExperimentalUuidApi::class)
data class OutgoingMessageSession(
    val text: String,
    val messageId: Uuid,
    val startedAtEpochSeconds: Long,
    val parameters: MessageParameters,
) {
    fun isActive(currentEpochSeconds: Double): Boolean =
        text.isNotEmpty() && (currentEpochSeconds - startedAtEpochSeconds) <= parameters.lifeTimeSeconds
}

/** Owns the lifecycle of the currently broadcast message and its UI-facing countdown state. */
@OptIn(ExperimentalUuidApi::class)
class MessageTransmissionController(
    private val scope: CoroutineScope,
    private val onStateChanged: (isSending: Boolean, sendingCounter: Int, parameters: MessageParameters) -> Unit,
    private val onConnectionStateChanged: (ConnectionState) -> Unit,
) {
    private val log = logging("MESSAGE-TRANSMISSION")
    private var counterJob: Job? = null
    private var state = MessageTransmissionState()

    var currentSession: OutgoingMessageSession? = null
        private set

    val parameters: MessageParameters
        get() = state.parameters

    fun updateParameters(lifeTimeSeconds: Double, maxDistanceMeters: Double) {
        val newParameters = MessageParameters(
            lifeTimeSeconds = lifeTimeSeconds,
            maxDistanceMeters = maxDistanceMeters,
        )

        state = state.copy(parameters = newParameters)
        currentSession = currentSession?.copy(parameters = newParameters)
        publishState()
    }

    @OptIn(ExperimentalTime::class)
    fun activeSession(
        currentEpochSeconds: Double = Clock.System.now().epochSeconds.toDouble(),
    ): OutgoingMessageSession? {
        val activeSession = currentSession ?: return null
        return if (activeSession.isActive(currentEpochSeconds)) {
            activeSession
        } else {
            stopSending()
            null
        }
    }

    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    fun sendMessage(
        message: String,
        deviceId: Uuid,
        lifeTime: Double = state.parameters.lifeTimeSeconds,
        maxDistanceMeters: Double = state.parameters.maxDistanceMeters,
    ): ChatMessage {
        log.i { "Message: '$message'" }

        val newParameters = MessageParameters(
            lifeTimeSeconds = lifeTime,
            maxDistanceMeters = maxDistanceMeters,
        )
        val newSession = OutgoingMessageSession(
            text = message,
            messageId = Uuid.random(),
            startedAtEpochSeconds = Clock.System.now().epochSeconds,
            parameters = newParameters,
        )

        currentSession = newSession
        state = state.copy(
            isSending = true,
            sendingCounter = newParameters.lifeTimeSeconds.toInt(),
            parameters = newParameters,
        )
        onConnectionStateChanged(ConnectionState.SENDING)
        publishState()
        startSendingCounter(newParameters.lifeTimeSeconds.toInt())

        return ChatMessage(
            text = message,
            sender = deviceId,
            messageId = newSession.messageId,
            distanceFromSource = 0.0,
        )
    }

    fun stopSending() {
        if (!state.isSending && currentSession == null) {
            return
        }

        log.i { "Message lifetime expired, stopping transmission" }
        counterJob?.cancel()
        counterJob = null
        currentSession = null
        state = state.copy(isSending = false, sendingCounter = 0)
        onConnectionStateChanged(ConnectionState.CONNECTED)
        publishState()
    }

    fun cleanup() {
        counterJob?.cancel()
        counterJob = null
        currentSession = null
    }

    private fun startSendingCounter(durationSeconds: Int) {
        counterJob?.cancel()
        counterJob = scope.launch {
            for (remainingSeconds in durationSeconds downTo 0) {
                if (!state.isSending) {
                    break
                }

                state = state.copy(sendingCounter = remainingSeconds)
                publishState()

                if (remainingSeconds > 0) {
                    delay(1000)
                }
            }
        }
    }

    private fun publishState() {
        onStateChanged(state.isSending, state.sendingCounter, state.parameters)
    }
}
