package it.unibo.collektive.echo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Message input row with send action and sending countdown state. */
@Composable
fun MessageComposer(
    messageText: String,
    isSending: Boolean,
    sendingCounter: Int,
    onMessageTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = messageText,
            onValueChange = onMessageTextChange,
            placeholder = { Text("Write a message...") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            enabled = !isSending,
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box {
            FloatingActionButton(
                onClick = onSendMessage,
                modifier = Modifier.size(50.dp),
                containerColor = if (isSending) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.primary
                },
            ) {
                if (isSending) {
                    Text(
                        text = "$sendingCounter",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary,
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                    )
                }
            }
        }
    }
}
