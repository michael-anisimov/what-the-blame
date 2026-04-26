package com.github.michaelanisimov.whattheblame.git

import com.github.michaelanisimov.whattheblame.BlameFailure
import com.github.michaelanisimov.whattheblame.git.model.BlameLine
import com.github.michaelanisimov.whattheblame.git.model.CommitTouch
import com.github.michaelanisimov.whattheblame.git.model.LineHistory
import com.github.michaelanisimov.whattheblame.settings.WhatTheBlameSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.annotate.FileAnnotation
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import git4idea.GitVcs
import git4idea.annotate.GitFileAnnotation
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

@Service(Service.Level.PROJECT)
class LineHistoryService(private val project: Project) {

    private val log = thisLogger()

    suspend fun load(target: BlameTarget): LineHistoryResult = withContext(Dispatchers.IO) {
        try {
            val repo = GitRepositoryManager.getInstance(project).getRepositoryForFile(target.file)
                ?: return@withContext LineHistoryResult.Failure(BlameFailure.FileNotInGit)
            val relPath = VfsUtilCore.getRelativePath(target.file, repo.root, '/')
                ?: return@withContext LineHistoryResult.Failure(BlameFailure.FileNotInGit)

            val settings = WhatTheBlameSettings.get().state
            val commits = loadCommits(repo, relPath, target.startLine, target.endLine, settings.maxCommits)
            if (commits.isEmpty()) {
                return@withContext LineHistoryResult.Failure(BlameFailure.NoCommitsForRange)
            }

            val withHunks = if (settings.includeDiffHunks) {
                commits.map { it.copy(hunkForRange = loadHunk(repo, relPath, target.startLine, target.endLine, it.hash)) }
            } else commits

            val blame = loadBlame(target.file, target.startLine, target.endLine)

            LineHistoryResult.Ok(LineHistory(target = target, commits = withHunks, blame = blame))
        } catch (t: Throwable) {
            log.warn("Failed to load line history", t)
            LineHistoryResult.Failure(BlameFailure.Other(t))
        }
    }

    private fun loadCommits(
        repo: GitRepository,
        relPath: String,
        startLine: Int,
        endLine: Int,
        maxCommits: Int,
    ): List<CommitTouch> {
        val handler = GitLineHandler(repo.project, repo.root, GitCommand.LOG).apply {
            addParameters(
                "-L${startLine},${endLine}:${relPath}",
                "--no-patch",
                "--format=$FORMAT",
                "-n", maxCommits.toString(),
            )
            setSilent(true)
        }
        val output = Git.getInstance().runCommand(handler).getOutputOrThrow()
        return parseRecords(output)
    }

    private fun loadHunk(
        repo: GitRepository,
        relPath: String,
        startLine: Int,
        endLine: Int,
        hash: String,
    ): String? {
        val handler = GitLineHandler(repo.project, repo.root, GitCommand.LOG).apply {
            addParameters(
                "-L${startLine},${endLine}:${relPath}",
                "-1",
                hash,
                "--format=",
            )
            setSilent(true)
        }
        return runCatching {
            Git.getInstance().runCommand(handler).getOutputOrThrow().trim().takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun loadBlame(file: VirtualFile, startLine: Int, endLine: Int): List<BlameLine> {
        val vcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(file) ?: return emptyList()
        if (vcs.keyInstanceMethod != GitVcs.getKey()) return emptyList()
        val provider = vcs.annotationProvider ?: return emptyList()

        val annotation: FileAnnotation = ApplicationManager.getApplication().runReadAction<FileAnnotation> {
            provider.annotate(file)
        }
        try {
            val gitAnnotation = annotation as? GitFileAnnotation
            val maxLine = annotation.lineCount - 1
            val range = (startLine - 1)..minOf(endLine - 1, maxLine)
            return range.map { i ->
                val info = gitAnnotation?.getLineInfo(i)
                BlameLine(
                    lineNumber = i + 1,
                    author = info?.author ?: "unknown",
                    authorEmail = info?.authorUser?.email.orEmpty(),
                    date = annotation.getLineDate(i)?.toInstant(),
                    commitHash = annotation.getLineRevisionNumber(i)?.asString().orEmpty(),
                )
            }
        } finally {
            annotation.dispose()
        }
    }

    private fun parseRecords(output: String): List<CommitTouch> {
        if (output.isBlank()) return emptyList()
        return output.split(REC_SEP)
            .asSequence()
            .map { it.trim('\n', '\r', ' ', '\t') }
            .filter { it.isNotBlank() }
            .mapNotNull { record ->
                val f = record.split(FIELD_SEP)
                if (f.size < 7) return@mapNotNull null
                val instant = parseInstant(f[4]) ?: return@mapNotNull null
                val subject = f[5]
                val fullBody = f[6]
                val body = fullBody.removePrefix(subject).trimStart('\n', '\r')
                CommitTouch(
                    hash = f[0].trim(),
                    shortHash = f[1].trim(),
                    author = f[2],
                    authorEmail = f[3],
                    authorDate = instant,
                    subject = subject,
                    body = body,
                    hunkForRange = null,
                )
            }
            .toList()
    }

    private fun parseInstant(value: String): Instant? = try {
        OffsetDateTime.parse(value.trim()).toInstant()
    } catch (_: DateTimeParseException) {
        null
    }

    companion object {
        private const val FIELD_SEP = ""
        private const val REC_SEP = ""
        private const val FORMAT =
            "%H${FIELD_SEP}%h${FIELD_SEP}%an${FIELD_SEP}%ae${FIELD_SEP}%aI${FIELD_SEP}%s${FIELD_SEP}%B${REC_SEP}"
    }
}
