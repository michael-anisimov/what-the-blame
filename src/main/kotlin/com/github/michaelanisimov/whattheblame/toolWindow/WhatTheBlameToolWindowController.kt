package com.github.michaelanisimov.whattheblame.toolwindow

import com.github.michaelanisimov.whattheblame.BlameFailure
import com.github.michaelanisimov.whattheblame.git.BlameTarget
import com.github.michaelanisimov.whattheblame.git.LineHistoryResult
import com.github.michaelanisimov.whattheblame.git.LineHistoryService
import com.github.michaelanisimov.whattheblame.llm.NarrationEvent
import com.github.michaelanisimov.whattheblame.llm.NarratorService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class WhatTheBlameToolWindowController(
    private val project: Project,
    private val scope: CoroutineScope,
) {
    private val log = thisLogger()
    private var panel: WhatTheBlamePanel? = null
    private var currentJob: Job? = null

    fun attach(panel: WhatTheBlamePanel) {
        this.panel = panel
    }

    fun analyze(target: BlameTarget) {
        val tw = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID) ?: return
        tw.show()
        val p = panel ?: return

        currentJob?.cancel()
        p.beginAnalysis(target) { analyze(target) }

        currentJob = scope.launch {
            try {
                val historyService = project.service<LineHistoryService>()
                val narrator = project.service<NarratorService>()
                when (val result = historyService.load(target)) {
                    is LineHistoryResult.Failure -> p.onEvent(NarrationEvent.Failed(result.failure))
                    is LineHistoryResult.Ok -> {
                        p.showHistoryLoaded(result.history)
                        narrator.narrate(result.history).collect { event -> p.onEvent(event) }
                    }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                log.warn("Analysis failed", t)
                p.onEvent(NarrationEvent.Failed(BlameFailure.Other(t)))
            }
        }
    }

    fun dispose() {
        scope.cancel()
    }

    companion object {
        const val TOOL_WINDOW_ID = "WhatTheBlame"
    }
}
