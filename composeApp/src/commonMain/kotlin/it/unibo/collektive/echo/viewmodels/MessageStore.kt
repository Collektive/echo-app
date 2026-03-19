package it.unibo.collektive.echo.viewmodels

import it.unibo.collektive.echo.models.ChatMessage
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class MessageStoreResult(val allMessages: List<ChatMessage>, val appendedMessages: List<ChatMessage>)

/** Maintains a deduplicated chat timeline keyed by message id. */
@OptIn(ExperimentalUuidApi::class)
class MessageStore {
    private val knownMessageIds = mutableSetOf<Uuid>()
    private val messages = mutableListOf<ChatMessage>()

    fun appendUnique(newMessages: List<ChatMessage>): MessageStoreResult {
        if (newMessages.isEmpty()) {
            return MessageStoreResult(
                allMessages = messages.toList(),
                appendedMessages = emptyList(),
            )
        }

        val appendedMessages = newMessages.filter { message ->
            knownMessageIds.add(message.messageId)
        }

        messages += appendedMessages

        return MessageStoreResult(
            allMessages = messages.toList(),
            appendedMessages = appendedMessages,
        )
    }

    fun clear() {
        knownMessageIds.clear()
        messages.clear()
    }
}
