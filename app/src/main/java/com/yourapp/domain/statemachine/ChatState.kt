package com.yourapp.domain.statemachine

import kotlinx.coroutines.Job

sealed class ChatState {
    data object Idle : ChatState()

    data object Typing : ChatState()

    data object Validating : ChatState()

    data class Processing(val job: Job) : ChatState()

    data class Responding(val content: String) : ChatState()

    data class Error(val message: String) : ChatState()
}
