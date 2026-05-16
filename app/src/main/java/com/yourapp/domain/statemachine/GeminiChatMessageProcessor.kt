package com.yourapp.domain.statemachine

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.example.application.BuildConfig
import javax.inject.Inject

class GeminiChatMessageProcessor @Inject constructor() : ChatMessageProcessor {
    private val model = BuildConfig.GEMINI_API_KEY
        .takeIf { it.isNotBlank() }
        ?.let { apiKey ->
            GenerativeModel(
                modelName = "gemini-3-flash-preview",
                apiKey = apiKey,
            )
        }

    override suspend fun process(message: String): String {
        val configuredModel = model
            ?: return "Gemini API key is missing. Add GEMINI_API_KEY to local.properties or your environment."

        return try {
            val response = configuredModel.generateContent(
                content {
                    text(message)
                }
            )
            response.text ?: "I'm sorry, I couldn't generate a response."
        } catch (e: Exception) {
            "Error: ${e.message ?: "Unknown error occurred"}"
        }
    }
}
