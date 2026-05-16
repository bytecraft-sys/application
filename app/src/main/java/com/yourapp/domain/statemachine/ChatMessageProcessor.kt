package com.yourapp.domain.statemachine

import javax.inject.Inject
import kotlinx.coroutines.delay

interface ChatMessageProcessor {
    suspend fun process(message: String): String
}

class DefaultChatMessageProcessor @Inject constructor() : ChatMessageProcessor {
    override suspend fun process(message: String): String {
        val simulatedProcessingMs = (message.length * MILLIS_PER_CHARACTER)
            .coerceIn(MIN_PROCESSING_MS, MAX_PROCESSING_MS)
        delay(simulatedProcessingMs)
        return "This is a simulated response to: $message"
    }

    private companion object {
        private const val MIN_PROCESSING_MS = 250L
        private const val MAX_PROCESSING_MS = 1_500L
        private const val MILLIS_PER_CHARACTER = 20L
    }
}
