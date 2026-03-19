@file:Suppress("MagicNumber")

package it.unibo.collektive.echo.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.unibo.collektive.echo.MAX_DISTANCE
import it.unibo.collektive.echo.MAX_TIME
import it.unibo.collektive.echo.MIN_DISTANCE
import it.unibo.collektive.echo.MIN_TIME
import it.unibo.collektive.echo.viewmodels.NearbyDevicesUiState
import kotlin.uuid.ExperimentalUuidApi

/** Pure screen content for the nearby-devices experience. */
@OptIn(ExperimentalUuidApi::class)
@Composable
fun NearbyDevicesScreen(
    uiState: NearbyDevicesUiState,
    onSendMessage: (message: String, lifeTime: Double, maxDistanceMeters: Double) -> Unit,
    onMessageParametersChange: (lifeTimeSeconds: Double, maxDistanceMeters: Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        ConnectionStatusCard(
            connection = uiState.connection,
            isSending = uiState.isSending,
            sendingCounter = uiState.sendingCounter,
            discoveredDevicesCount = uiState.discoveredDevices.size,
            currentLocation = uiState.currentLocation,
        )

        Spacer(modifier = Modifier.height(8.dp))

        MessageSettingsCard(
            title = "Max Distance",
            value = uiState.messageParameters.maxDistanceMeters.toFloat(),
            valueRange = MIN_DISTANCE.toFloat()..MAX_DISTANCE.toFloat(),
            valueSuffix = "m",
            onValueChange = { newDistance ->
                onMessageParametersChange(
                    uiState.messageParameters.lifeTimeSeconds,
                    newDistance.toDouble(),
                )
            },
        )

        Spacer(modifier = Modifier.height(8.dp))

        MessageSettingsCard(
            title = "Message Lifetime",
            value = uiState.messageParameters.lifeTimeSeconds.toFloat(),
            valueRange = MIN_TIME.toFloat()..MAX_TIME.toFloat(),
            valueSuffix = "s",
            onValueChange = { newLifetime ->
                onMessageParametersChange(
                    newLifetime.toDouble(),
                    uiState.messageParameters.maxDistanceMeters,
                )
            },
        )

        Spacer(modifier = Modifier.height(8.dp))

        MessageList(
            modifier = Modifier.weight(1f),
            messages = uiState.messages,
            deviceId = uiState.deviceId,
            listState = listState,
        )

        Spacer(modifier = Modifier.height(8.dp))

        MessageComposer(
            messageText = messageText,
            isSending = uiState.isSending,
            sendingCounter = uiState.sendingCounter,
            onMessageTextChange = { messageText = it },
            onSendMessage = {
                if (messageText.isBlank() || uiState.isSending) {
                    return@MessageComposer
                }

                onSendMessage(
                    messageText,
                    uiState.messageParameters.lifeTimeSeconds,
                    uiState.messageParameters.maxDistanceMeters,
                )
                messageText = ""
            },
        )
    }
}
