package com.yourapp.domain.statemachine

import io.mockk.coEvery
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
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
    fun `typing to idle transition`() = runTest(testDispatcher) {
        stateMachine.sendMessage("Hello")
        testDispatcher.scheduler.runCurrent()

        assertTrue(stateMachine.state.value is ChatState.Processing)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ChatState.Idle, stateMachine.state.value)
    }

    @Test
    fun `second message cancels first processing job`() = runTest(testDispatcher) {
        stateMachine.sendMessage("Message A")
        testDispatcher.scheduler.runCurrent()

        val firstJob = (stateMachine.state.value as ChatState.Processing).job

        stateMachine.sendMessage("Message B")
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

        assertTrue(stateMachine.state.value is ChatState.Processing)

        testDispatcher.scheduler.advanceTimeBy(8_001L)
        testDispatcher.scheduler.runCurrent()

        val state = stateMachine.state.value
        assertTrue(state is ChatState.Error)
        assertTrue((state as ChatState.Error).message.contains("timed out"))
    }
}
