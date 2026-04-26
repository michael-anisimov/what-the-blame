package com.github.michaelanisimov.whattheblame.llm

import com.github.michaelanisimov.whattheblame.git.model.BlameLine
import com.github.michaelanisimov.whattheblame.git.model.LineHistory
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object BlamePromptBuilder {

    private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)

    fun buildSystem(): List<AnthropicClient.SystemBlock> = listOf(
        AnthropicClient.SystemBlock(text = SYSTEM_PROMPT, cacheable = true),
    )

    fun buildUserMessage(history: LineHistory): String = buildString {
        val t = history.target
        appendLine("File: ${t.file.path}")
        appendLine("Lines: ${t.startLine}–${t.endLine}")
        appendLine("Language: ${t.language}")
        appendLine()
        appendLine("--- SELECTED CODE ---")
        appendLine("```${t.language.lowercase()}")
        append(t.selectedText)
        if (!t.selectedText.endsWith("\n")) appendLine()
        appendLine("```")
        appendLine()
        if (history.blame.isNotEmpty()) {
            appendLine("--- BLAME (line → author, date, commit) ---")
            history.blame.forEach { appendLine(formatBlameLine(it)) }
            appendLine()
        }
        if (history.commits.isEmpty()) {
            appendLine("(No commits found that touched these lines.)")
        } else {
            appendLine("--- COMMITS TOUCHING THESE LINES (newest first) ---")
            history.commits.forEachIndexed { idx, c ->
                appendLine()
                appendLine("[${idx + 1}] ${c.shortHash} — ${c.author} <${c.authorEmail}> — ${DATE_FORMAT.format(c.authorDate)}")
                appendLine("    Subject: ${c.subject}")
                if (c.body.isNotBlank()) {
                    appendLine("    Message:")
                    c.body.lineSequence().forEach { line -> appendLine("      $line") }
                }
                if (!c.hunkForRange.isNullOrBlank()) {
                    appendLine("    Hunk:")
                    appendLine("      ```diff")
                    c.hunkForRange.lineSequence().forEach { line -> appendLine("      $line") }
                    appendLine("      ```")
                }
            }
        }
    }

    private fun formatBlameLine(b: BlameLine): String {
        val date = b.date?.let { DATE_FORMAT.format(it) } ?: "????-??-??"
        val short = b.commitHash.take(7).ifBlank { "(unknown)" }
        val email = if (b.authorEmail.isNotBlank()) " <${b.authorEmail}>" else ""
        return "${b.lineNumber}: ${b.author}${email} ${date} ${short}"
    }

    private const val SYSTEM_PROMPT = """You are a senior software archaeologist embedded in an IDE. You will be given:
  • a snippet of source code the user has selected,
  • per-line `git blame` data for that selection,
  • the list of commits whose changes overlap that selection (newest first),
    each including the commit subject, full message, and (when available) the
    diff hunk confined to the selected line range.

Your job: explain the *story* of those lines so the user immediately understands
what they are looking at and why. Reason about *intent*, not just mechanics —
connect commits to a likely purpose, even when commit messages are sparse. Be
honest about uncertainty.

Output strictly the following Markdown structure, nothing before or after:

## Summary
A 2–4 sentence narrative paragraph: when the block was introduced, the most
consequential change since, and what these lines do *now*.

## Timeline
A bulleted list, oldest → newest, of the commits that meaningfully shaped this
region. Each bullet:
- **YYYY-MM-DD — Author** — `<short hash>` <commit subject>
  Why (your inference): one sentence. If the commit message is uninformative
  ("fix", "wip", typos, mechanical refactor), say so explicitly: "Commit message
  uninformative; judging by the diff, …".

## Current Authors
One sentence naming who owns these lines now per blame, with rough percentages
if multiple authors.

## Caveats
Optional. Include only if commit messages were unusually thin, the range
contains only a partial change, or the inference is shaky.

Rules:
- Never invent commits, authors, or dates not present in the input.
- Prefer the diff hunk over the commit subject when they conflict.
- Keep the whole response under ~350 words unless the history is genuinely complex.
- Quote at most 2 short code fragments (≤3 lines each) from the hunks.
- Do not wrap the entire response in a code block.
"""
}
