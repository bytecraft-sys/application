package com.yourapp.domain.statemachine

import io.mockk.coEvery
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class ChatStateMachineTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var stateMachine: ChatStateMachine

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        stateMachine = ChatStateMachine(testDispatcher)
    }

    @AfterEach
    fun teardown() {
        stateMachine.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `happy path follows exact pipeline`() = runTest(testDispatcher) {
        stateMachine.close()
        val logger = RecordingChatStateLogger()
        stateMachine = ChatStateMachine(testDispatcher, DelayedProcessor(), logger)

        stateMachine.sendMessage("Hello")
        testDispatcher.scheduler.advanceUntilIdle()

        val states = logger.transitions.map { it.second }
        assertEquals(5, states.size)
        assertTrue(states[0] is ChatState.Typing)
        assertTrue(states[1] is ChatState.Validating)
        assertTrue(states[2] is ChatState.Processing)
        assertTrue(states[3] is ChatState.Responding)
        assertEquals(ChatState.Idle, states[4])
    }

    @Test
    fun `second message cancels first processing job`() = runTest(testDispatcher) {
        stateMachine.sendMessage("Message A")
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(2L)
        testDispatcher.scheduler.runCurrent()

        val firstJob = (stateMachine.state.value as ChatState.Processing).job

        stateMachine.sendMessage("Message B")
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(2L)
        testDispatcher.scheduler.runCurrent()

        assertTrue(firstJob.isCancelled)
        assertTrue(stateMachine.state.value is ChatState.Processing)
    }

    @Test
    fun `eight second timeout emits error state`() = runTest(testDispatcher) {
        val processor = mockk<ChatMessageProcessor>()
        coEvery { processor.process(any()) } coAnswers { awaitCancellation() }
        stateMachine.close()
        stateMachine = ChatStateMachine(testDispatcher, processor)

        stateMachine.sendMessage("Slow message")
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(2L)
        testDispatcher.scheduler.runCurrent()

        assertTrue(stateMachine.state.value is ChatState.Processing)

        testDispatcher.scheduler.advanceTimeBy(8_001L)
        testDispatcher.scheduler.runCurrent()

        val state = stateMachine.state.value
        assertTrue(state is ChatState.Error)
        assertTrue((state as ChatState.Error).message.contains("timed out"))
    }

    private class DelayedProcessor : ChatMessageProcessor {
        override suspend fun process(message: String): String {
            delay(100L)
            return "Response to $message"
        }
    }

    private class RecordingChatStateLogger : ChatStateLogger {
        val transitions = mutableListOf<Pair<ChatState, ChatState>>()

        override fun logTransition(previousState: ChatState, nextState: ChatState) {
            transitions += previousState to nextState
        }
    }
}
