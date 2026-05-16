package com.yourapp.domain.statemachine

import android.util.Log
import javax.inject.Inject

interface ChatStateLogger {
    fun logTransition(previousState: ChatState, nextState: ChatState)
}

class AndroidChatStateLogger @Inject constructor() : ChatStateLogger {
    override fun logTransition(previousState: ChatState, nextState: ChatState) {
        Log.d(TAG, "ChatState transition: $previousState -> $nextState")
    }

    private companion object {
        private const val TAG = "ChatStateMachine"
    }
}

object NoOpChatStateLogger : ChatStateLogger {
    override fun logTransition(previousState: ChatState, nextState: ChatState) = Unit
}
