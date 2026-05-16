package com.yourapp.domain.statemachine

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ChatStateMachineModule {
    @Binds
    abstract fun bindChatMessageProcessor(
        processor: GeminiChatMessageProcessor,
    ): ChatMessageProcessor

    @Binds
    abstract fun bindChatStateLogger(
        logger: AndroidChatStateLogger,
    ): ChatStateLogger
}
