package com.example.echo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.echo.viewmodels.NearbyDevicesViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Message
@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
constructor(
    val text: String,
    val sender: Uuid,
    val timestamp: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
)

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
@Composable
fun Screen(modifier: Modifier, devices: Set<Uuid>, connection: NearbyDevicesViewModel.ConnectionState, uuid: Uuid) {
    var metersValue by remember { mutableStateOf(0f) }
    var secondsValue by remember { mutableStateOf(0f) }
    var messages by remember { mutableStateOf(listOf<Message>()) }
    var messageText by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Text(
                text = "Meters: ${metersValue.toInt()}m",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
            Slider(
                value = metersValue,
                onValueChange = { metersValue = it },
                valueRange = 0f..1000f,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.padding(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Text(
                text = "Time: ${secondsValue.toInt()}s",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
            Slider(
                value = secondsValue,
                onValueChange = { secondsValue = it },
                valueRange = 0f..60f,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Messages
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
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
                    MessageItem(message = message, uuid)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Write a message...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
            )

            Spacer(modifier = Modifier.width(8.dp))

            FloatingActionButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        val newMessage = Message(
                            text = messageText,
                            sender = uuid,
                        )
                        messages = messages + newMessage
                        messageText = ""

                        scope.launch {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                    // TODO: send messages to other devices
                },
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Message",
                )
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
@Composable
fun MessageItem(message: Message, uuid: Uuid) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.sender == uuid) Arrangement.End else Arrangement.Start,
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.sender == uuid) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                },
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.sender == uuid) 16.dp else 4.dp,
                bottomEnd = if (message.sender == uuid) 4.dp else 16.dp,
            ),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
            ) {
                Text(
                    text = uuid.toString(),
                    color = if (message.sender == uuid) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontSize = 14.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.text,
                    color = if (message.sender == uuid) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontSize = 14.sp,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.timestamp.toString(),
                    color = if (message.sender == uuid) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    },
                    fontSize = 11.sp,
                )
            }
        }
    }
}
