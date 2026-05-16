package com.yourapp.domain.statemachine

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import javax.inject.Inject

class GeminiChatMessageProcessor @Inject constructor() : ChatMessageProcessor {
    private val apiKey = "AIzaSyAt34xEaKDukyurxDEc-Aa_koQvCc7pVJw"
    
    private val model = GenerativeModel(
        modelName = "gemini-3-flash-preview",
        apiKey = apiKey
    )

    override suspend fun process(message: String): String {
        return try {
            val response = model.generateContent(
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
