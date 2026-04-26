package com.github.michaelanisimov.whattheblame.llm

import kotlinx.coroutines.flow.Flow

interface AnthropicClient {

    fun stream(
        apiKey: String,
        model: String,
        system: List<SystemBlock>,
        userMessage: String,
        maxTokens: Int = 1500,
    ): Flow<TextDelta>

    data class SystemBlock(val text: String, val cacheable: Boolean)
    data class TextDelta(val text: String)
}
