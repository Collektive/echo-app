package it.unibo.collektive.echo.viewmodels

import it.unibo.collektive.echo.models.ChatMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class MessageStoreTest {

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun `appendUnique keeps first instance of each message id`() {
        val messageId = Uuid.random()
        val messageStore = MessageStore()
        val firstMessage = ChatMessage(
            text = "hello",
            sender = Uuid.random(),
            messageId = messageId,
        )
        val duplicateMessage = firstMessage.copy(text = "updated text")

        val result = messageStore.appendUnique(listOf(firstMessage, duplicateMessage))

        assertEquals(listOf(firstMessage), result.allMessages)
        assertEquals(listOf(firstMessage), result.appendedMessages)
    }
}
