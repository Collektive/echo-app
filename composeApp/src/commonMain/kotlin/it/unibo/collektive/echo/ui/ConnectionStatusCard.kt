package it.unibo.collektive.echo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.unibo.collektive.echo.location.Location
import it.unibo.collektive.echo.viewmodels.ConnectionState

private val ConnectedColor = Color(0xFF4CAF50)
private val SendingColor = Color(0xFFFF9800)
private val DisconnectedColor = Color(0xFFF44336)
private const val BackgroundAlpha = 0.1f

/** Card displaying the current MQTT connection state, device count, and GPS coordinates. */
@Composable
fun ConnectionStatusCard(
    connection: ConnectionState,
    isSending: Boolean,
    sendingCounter: Int = 0,
    discoveredDevicesCount: Int = 0,
    currentLocation: Location? = null,
) {
    val stateColor = connection.color()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = stateColor.copy(alpha = BackgroundAlpha),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                modifier = Modifier.size(12.dp),
                shape = CircleShape,
                color = stateColor,
            ) {}

            Column {
                Text(
                    text = connection.label(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = stateColor,
                )

                Text(
                    text = "Discovered devices: $discoveredDevicesCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )

                currentLocation?.let { location ->
                    Text(
                        text = "GPS: ${location.latitude}, ${location.longitude}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ConnectedColor,
                        maxLines = 1,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (isSending) {
                Text(
                    text = "${sendingCounter}s",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = SendingColor,
                )
            }
        }
    }
}

private fun ConnectionState.color(): Color = when (this) {
    ConnectionState.CONNECTED -> ConnectedColor
    ConnectionState.SENDING -> SendingColor
    ConnectionState.DISCONNECTED -> DisconnectedColor
}

private fun ConnectionState.label(): String = when (this) {
    ConnectionState.CONNECTED -> "Connected"
    ConnectionState.SENDING -> "Propagating message..."
    ConnectionState.DISCONNECTED -> "Disconnected"
}
