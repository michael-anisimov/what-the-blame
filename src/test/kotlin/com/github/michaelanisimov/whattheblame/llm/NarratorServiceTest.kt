package com.github.michaelanisimov.whattheblame.llm

import com.github.michaelanisimov.whattheblame.BlameFailure
import com.github.michaelanisimov.whattheblame.git.BlameTarget
import com.github.michaelanisimov.whattheblame.git.model.LineHistory
import com.github.michaelanisimov.whattheblame.settings.ApiKeyService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.replaceService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class NarratorServiceTest : BasePlatformTestCase() {

    private fun fakeFile(): VirtualFile {
        val f = mock<VirtualFile>()
        whenever(f.path).thenReturn("/repo/Foo.kt")
        whenever(f.name).thenReturn("Foo.kt")
        return f
    }

    private fun fakeHistory() = LineHistory(
        target = BlameTarget(fakeFile(), 1, 1, "x", "Kotlin"),
        commits = emptyList(),
        blame = emptyList(),
    )

    override fun tearDown() {
        try {
            ApiKeyService.getInstance().set(null)
        } finally {
            super.tearDown()
        }
    }

    fun `test missing api key surfaces NoApiKey failure`(): Unit = runBlocking {
        val app = ApplicationManager.getApplication()
        ApiKeyService.getInstance().set(null)
        app.replaceService(AnthropicClient::class.java, FakeAnthropicClient(emptyList()), testRootDisposable)

        val events = project.getService(NarratorService::class.java).narrate(fakeHistory()).toList()

        assertEquals(1, events.size)
        assertEquals(NarrationEvent.Failed(BlameFailure.NoApiKey), events.single())
    }

    fun `test happy path emits deltas then Done`(): Unit = runBlocking {
        val app = ApplicationManager.getApplication()
        ApiKeyService.getInstance().set("sk-test")
        app.replaceService(
            AnthropicClient::class.java,
            FakeAnthropicClient(listOf("Hello, ", "world!")),
            testRootDisposable,
        )

        val events = project.getService(NarratorService::class.java).narrate(fakeHistory()).toList()

        assertEquals(3, events.size)
        assertEquals(NarrationEvent.TextDelta("Hello, "), events[0])
        assertEquals(NarrationEvent.TextDelta("world!"), events[1])
        assertEquals(NarrationEvent.Done(13), events[2])
    }

    fun `test sdk failure becomes Failed event`(): Unit = runBlocking {
        val app = ApplicationManager.getApplication()
        ApiKeyService.getInstance().set("sk-test")
        app.replaceService(
            AnthropicClient::class.java,
            FakeAnthropicClient(emptyList(), failWith = BlameFailure.RateLimited),
            testRootDisposable,
        )

        val events = project.getService(NarratorService::class.java).narrate(fakeHistory()).toList()

        assertEquals(1, events.size)
        assertEquals(NarrationEvent.Failed(BlameFailure.RateLimited), events.single())
    }

    private class FakeAnthropicClient(
        private val deltas: List<String>,
        private val failWith: BlameFailure? = null,
    ) : AnthropicClient {
        override fun stream(
            apiKey: String,
            model: String,
            system: List<AnthropicClient.SystemBlock>,
            userMessage: String,
            maxTokens: Int,
        ): Flow<AnthropicClient.TextDelta> = flow {
            if (failWith != null) throw BlameApiException(failWith, RuntimeException("fake"))
            deltas.forEach { emit(AnthropicClient.TextDelta(it)) }
        }
    }
}
