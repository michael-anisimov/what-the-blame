package com.github.michaelanisimov.whattheblame.llm

import com.github.michaelanisimov.whattheblame.BlameFailure
import com.github.michaelanisimov.whattheblame.git.model.LineHistory
import com.github.michaelanisimov.whattheblame.settings.ApiKeyService
import com.github.michaelanisimov.whattheblame.settings.WhatTheBlameSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Service(Service.Level.PROJECT)
class NarratorService(@Suppress("unused") private val project: Project) {

    private val log = thisLogger()

    fun narrate(history: LineHistory): Flow<NarrationEvent> = flow {
        try {
            val key = ApiKeyService.getInstance().get()
            if (key.isNullOrBlank()) {
                emit(NarrationEvent.Failed(BlameFailure.NoApiKey))
                return@flow
            }

            val client: AnthropicClient = service()
            val settings = WhatTheBlameSettings.get().state
            val system = BlamePromptBuilder.buildSystem()
            val user = BlamePromptBuilder.buildUserMessage(history)

            var totalChars = 0
            client.stream(
                apiKey = key,
                model = settings.model,
                system = system,
                userMessage = user,
            ).collect { delta ->
                totalChars += delta.text.length
                emit(NarrationEvent.TextDelta(delta.text))
            }
            emit(NarrationEvent.Done(totalChars))
        } catch (e: BlameApiException) {
            emit(NarrationEvent.Failed(e.failure))
        } catch (t: Throwable) {
            log.warn("Narration failed", t)
            emit(NarrationEvent.Failed(BlameFailure.Other(t)))
        }
    }
}
