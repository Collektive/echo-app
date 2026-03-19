package it.unibo.collektive.echo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.unibo.collektive.echo.models.ChatMessage
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val TimestampDisplayLength = 16

/** Scrollable list of chat messages. */
@OptIn(ExperimentalUuidApi::class)
@Composable
fun MessageList(
    messages: List<ChatMessage>,
    deviceId: Uuid,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(messages) { message ->
                MessageItem(message = message, deviceId = deviceId)
            }
        }
    }
}

/** Displays a single chat message bubble, aligned right for sent messages and left for received ones. */
@OptIn(ExperimentalUuidApi::class)
@Composable
fun MessageItem(message: ChatMessage, deviceId: Uuid) {
    val isOwnMessage = message.sender == deviceId

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start,
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isOwnMessage) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                },
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isOwnMessage) 16.dp else 4.dp,
                bottomEnd = if (isOwnMessage) 4.dp else 16.dp,
            ),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
            ) {
                Text(
                    text = if (isOwnMessage) "You" else "Id: ${message.sender}",
                    color = if (isOwnMessage) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    },
                    fontSize = 12.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.text,
                    color = if (isOwnMessage) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontSize = 14.sp,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = message.timestamp.toString().take(TimestampDisplayLength),
                        color = if (isOwnMessage) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        },
                        fontSize = 10.sp,
                    )

                    if (message.distanceFromSource > 0) {
                        Text(
                            text = "${message.distanceFromSource.toInt()}m",
                            color = if (isOwnMessage) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}
