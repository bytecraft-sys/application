package com.yourapp.domain.statemachine

import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

@ViewModelScoped
class ChatStateMachine @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val processor: ChatMessageProcessor = DefaultChatMessageProcessor(),
    private val logger: ChatStateLogger = NoOpChatStateLogger,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val mutex = Mutex()
    private val _state = MutableStateFlow<ChatState>(ChatState.Idle)

    val state: StateFlow<ChatState> = _state.asStateFlow()

    fun sendMessage(content: String) {
        scope.launch {
            mutex.withLock {
                val currentState = _state.value
                if (currentState is ChatState.Processing) {
                    currentState.job.cancelAndJoin()
                }

                transitionTo(ChatState.Typing)
                transitionTo(ChatState.Validating)

                if (content.isBlank()) {
                    transitionTo(ChatState.Error("Message cannot be empty"))
                    return@withLock
                }

                val processingJob = launchProcessingJob(content.trim())
                transitionTo(ChatState.Processing(processingJob))
            }
        }
    }

    fun close() {
        scope.cancel()
    }

    private fun launchProcessingJob(message: String): Job = scope.launch {
        try {
            withTimeout(8_000L) {
                val response = processor.process(message)
                transitionTo(ChatState.Responding(response))
            }
            delay(RESPONDING_STATE_MS)
            transitionTo(ChatState.Idle)
        } catch (exception: TimeoutCancellationException) {
            transitionTo(ChatState.Error("Request timed out"))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            transitionTo(ChatState.Error(exception.message ?: "Unable to process request"))
        }
    }

    private fun transitionTo(nextState: ChatState) {
        val previousState = _state.value
        logger.logTransition(previousState, nextState)
        _state.value = nextState
    }

    private companion object {
        private const val RESPONDING_STATE_MS = 250L
    }
}
