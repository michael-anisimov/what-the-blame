package com.github.michaelanisimov.whattheblame.llm

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.models.messages.CacheControlEphemeral
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.TextBlockParam
import com.github.michaelanisimov.whattheblame.BlameFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import java.io.IOException

class AnthropicSdkClient : AnthropicClient {

    override fun stream(
        apiKey: String,
        model: String,
        system: List<AnthropicClient.SystemBlock>,
        userMessage: String,
        maxTokens: Int,
    ): Flow<AnthropicClient.TextDelta> = callbackFlow {
        val client = AnthropicOkHttpClient.builder().apiKey(apiKey).build()
        val params = MessageCreateParams.builder()
            .model(model)
            .maxTokens(maxTokens.toLong())
            .systemOfTextBlockParams(
                system.map { block ->
                    TextBlockParam.builder()
                        .text(block.text)
                        .apply {
                            if (block.cacheable) cacheControl(CacheControlEphemeral.builder().build())
                        }
                        .build()
                }
            )
            .addUserMessage(userMessage)
            .build()

        val response = try {
            client.messages().createStreaming(params)
        } catch (e: UnauthorizedException) {
            close(BlameApiException(BlameFailure.InvalidApiKey, e))
            return@callbackFlow
        } catch (e: RateLimitException) {
            close(BlameApiException(BlameFailure.RateLimited, e))
            return@callbackFlow
        } catch (e: IOException) {
            close(BlameApiException(BlameFailure.Network(e), e))
            return@callbackFlow
        } catch (e: RuntimeException) {
            close(BlameApiException(BlameFailure.Other(e), e))
            return@callbackFlow
        }

        try {
            response.use { stream ->
                stream.stream().forEach { event ->
                    event.contentBlockDelta().ifPresent { contentDelta ->
                        contentDelta.delta().text().ifPresent { textDelta ->
                            trySend(AnthropicClient.TextDelta(textDelta.text()))
                        }
                    }
                }
            }
            close()
        } catch (e: UnauthorizedException) {
            close(BlameApiException(BlameFailure.InvalidApiKey, e))
        } catch (e: RateLimitException) {
            close(BlameApiException(BlameFailure.RateLimited, e))
        } catch (e: IOException) {
            close(BlameApiException(BlameFailure.Network(e), e))
        } catch (e: RuntimeException) {
            close(BlameApiException(BlameFailure.Other(e), e))
        }

        awaitClose {
            runCatching { client.close() }
        }
    }.flowOn(Dispatchers.IO)
}

class BlameApiException(val failure: BlameFailure, cause: Throwable) : RuntimeException(cause)
