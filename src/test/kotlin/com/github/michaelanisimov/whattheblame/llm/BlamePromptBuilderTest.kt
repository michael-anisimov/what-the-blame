package com.github.michaelanisimov.whattheblame.llm

import com.github.michaelanisimov.whattheblame.git.BlameTarget
import com.github.michaelanisimov.whattheblame.git.model.BlameLine
import com.github.michaelanisimov.whattheblame.git.model.CommitTouch
import com.github.michaelanisimov.whattheblame.git.model.LineHistory
import com.intellij.openapi.vfs.VirtualFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant

class BlamePromptBuilderTest {

    private fun fakeFile(path: String, name: String = path.substringAfterLast('/')): VirtualFile {
        val f = mock<VirtualFile>()
        whenever(f.path).thenReturn(path)
        whenever(f.name).thenReturn(name)
        return f
    }

    @Test
    fun `system prompt is a single cacheable block`() {
        val system = BlamePromptBuilder.buildSystem()
        assertEquals(1, system.size)
        assertTrue(system[0].cacheable)
        assertTrue(system[0].text.contains("software archaeologist"))
        assertTrue(system[0].text.contains("## Summary"))
        assertTrue(system[0].text.contains("## Timeline"))
        assertTrue(system[0].text.contains("## Current Authors"))
    }

    @Test
    fun `user message embeds file path, range, language, code, blame, and commits`() {
        val target = BlameTarget(
            file = fakeFile("/repo/src/Foo.kt", "Foo.kt"),
            startLine = 10,
            endLine = 12,
            selectedText = "fun bar() {\n    println(\"hi\")\n}",
            language = "Kotlin",
        )
        val history = LineHistory(
            target = target,
            commits = listOf(
                CommitTouch(
                    hash = "abc1234def",
                    shortHash = "abc1234",
                    author = "Alice",
                    authorEmail = "alice@example.com",
                    authorDate = Instant.parse("2025-01-15T12:00:00Z"),
                    subject = "Add bar()",
                    body = "Add bar()\n\nLong-form rationale.",
                    hunkForRange = "@@ -1,1 +1,3 @@\n+fun bar() {\n+    println(\"hi\")\n+}",
                ),
            ),
            blame = listOf(
                BlameLine(
                    lineNumber = 10,
                    author = "Alice",
                    authorEmail = "alice@example.com",
                    date = Instant.parse("2025-01-15T12:00:00Z"),
                    commitHash = "abc1234def",
                ),
            ),
        )

        val msg = BlamePromptBuilder.buildUserMessage(history)

        assertTrue("contains file path", msg.contains("/repo/src/Foo.kt"))
        assertTrue("contains line range", msg.contains("10–12"))
        assertTrue("contains language fence", msg.contains("```kotlin"))
        assertTrue("contains selected code", msg.contains("println(\"hi\")"))
        assertTrue("contains blame line", msg.contains("10: Alice <alice@example.com>"))
        assertTrue("contains commit subject", msg.contains("Add bar()"))
        assertTrue("contains body", msg.contains("Long-form rationale."))
        assertTrue("contains hunk fence", msg.contains("```diff"))
        assertFalse("body is not duplicated subject", msg.indexOf("Add bar()") == msg.lastIndexOf("Add bar()"))
    }

    @Test
    fun `empty commits list yields explicit fallback marker`() {
        val target = BlameTarget(
            file = fakeFile("/repo/Foo.kt", "Foo.kt"),
            startLine = 1,
            endLine = 1,
            selectedText = "x",
            language = "Kotlin",
        )
        val msg = BlamePromptBuilder.buildUserMessage(LineHistory(target, emptyList(), emptyList()))
        assertTrue(msg.contains("(No commits found that touched these lines.)"))
    }
}
