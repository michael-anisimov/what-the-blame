package com.github.michaelanisimov.whattheblame.toolwindow

import com.github.michaelanisimov.whattheblame.BlameFailure
import com.github.michaelanisimov.whattheblame.WhatTheBlameBundle
import com.github.michaelanisimov.whattheblame.git.BlameTarget
import com.github.michaelanisimov.whattheblame.git.model.LineHistory
import com.github.michaelanisimov.whattheblame.llm.NarrationEvent
import com.github.michaelanisimov.whattheblame.settings.WhatTheBlameConfigurable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.datatransfer.StringSelection
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JEditorPane

class WhatTheBlamePanel(private val project: Project) : JBPanel<WhatTheBlamePanel>(BorderLayout()) {

    private val headerLabel = JBLabel(WhatTheBlameBundle.message("toolWindow.empty"))
    private val warningLabel = JBLabel("").apply { isVisible = false }
    private val statusLabel = JBLabel(WhatTheBlameBundle.message("toolWindow.status.idle"))
    private val errorLink = HyperlinkLabel().apply { isVisible = false }
    private val refreshButton = JButton(WhatTheBlameBundle.message("toolWindow.refresh")).apply { isEnabled = false }
    private val copyButton = JButton(WhatTheBlameBundle.message("toolWindow.copy")).apply { isEnabled = false }
    private val settingsButton = JButton(WhatTheBlameBundle.message("toolWindow.openSettings"))

    private val editorPane = JEditorPane().apply {
        contentType = "text/html"
        isEditable = false
        border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
    }
    private val renderer = MarkdownRenderer(editorPane)

    private var refreshAction: (() -> Unit)? = null
    private var lastTarget: BlameTarget? = null

    init {
        border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
        add(buildHeader(), BorderLayout.NORTH)
        add(JBScrollPane(editorPane), BorderLayout.CENTER)
        add(buildFooter(), BorderLayout.SOUTH)

        refreshButton.addActionListener { refreshAction?.invoke() }
        copyButton.addActionListener {
            CopyPasteManager.getInstance().setContents(StringSelection(renderer.markdown))
            statusLabel.text = WhatTheBlameBundle.message("toolWindow.copied")
        }
        settingsButton.addActionListener { openSettings() }
    }

    private fun buildHeader(): JBPanel<*> {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        panel.add(headerLabel, BorderLayout.NORTH)
        panel.add(warningLabel, BorderLayout.SOUTH)
        return panel
    }

    private fun buildFooter(): JBPanel<*> {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        val buttons = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, 4, 0))
        buttons.add(refreshButton)
        buttons.add(copyButton)
        buttons.add(settingsButton)
        panel.add(buttons, BorderLayout.WEST)
        val status = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.RIGHT, 4, 0))
        status.add(errorLink)
        status.add(statusLabel)
        panel.add(status, BorderLayout.EAST)
        return panel
    }

    fun beginAnalysis(target: BlameTarget, refresh: () -> Unit) {
        runOnEdt {
            lastTarget = target
            refreshAction = refresh
            refreshButton.isEnabled = true
            copyButton.isEnabled = false
            errorLink.isVisible = false
            renderer.reset()
            statusLabel.text = WhatTheBlameBundle.message("toolWindow.status.loadingHistory")
            warningLabel.isVisible = FileDocumentManager.getInstance().isFileModified(target.file)
            warningLabel.text = if (warningLabel.isVisible)
                WhatTheBlameBundle.message("toolWindow.modifiedFileWarning")
            else ""
            headerLabel.text = WhatTheBlameBundle.message(
                "toolWindow.header",
                target.file.name,
                target.startLine,
                target.endLine,
                "?",
                "?",
            )
        }
    }

    fun showHistoryLoaded(history: LineHistory) {
        runOnEdt {
            val authors = history.blame.map { it.author }.toSet().size
            headerLabel.text = WhatTheBlameBundle.message(
                "toolWindow.header",
                history.target.file.name,
                history.target.startLine,
                history.target.endLine,
                history.commits.size,
                authors,
            )
            statusLabel.text = WhatTheBlameBundle.message("toolWindow.status.streaming")
        }
    }

    fun onEvent(event: NarrationEvent) {
        runOnEdt {
            when (event) {
                is NarrationEvent.TextDelta -> {
                    renderer.appendDelta(event.text)
                    copyButton.isEnabled = true
                }
                is NarrationEvent.Done -> {
                    statusLabel.text = WhatTheBlameBundle.message("toolWindow.status.done")
                    copyButton.isEnabled = renderer.markdown.isNotBlank()
                }
                is NarrationEvent.Failed -> showFailure(event.failure)
            }
        }
    }

    private fun showFailure(failure: BlameFailure) {
        statusLabel.text = WhatTheBlameBundle.message("toolWindow.status.idle")
        when (failure) {
            BlameFailure.NoApiKey -> {
                renderer.setText("> " + WhatTheBlameBundle.message("failure.noApiKey"))
                showSettingsLink()
            }
            BlameFailure.InvalidApiKey -> {
                renderer.setText("> " + WhatTheBlameBundle.message("failure.invalidApiKey"))
                showSettingsLink()
            }
            BlameFailure.RateLimited ->
                renderer.setText("> " + WhatTheBlameBundle.message("failure.rateLimited"))
            BlameFailure.FileNotInGit ->
                renderer.setText("> " + WhatTheBlameBundle.message("failure.fileNotInGit"))
            BlameFailure.NoCommitsForRange ->
                renderer.setText("_" + WhatTheBlameBundle.message("failure.noCommitsForRange") + "_")
            is BlameFailure.Network ->
                renderer.setText("> " + WhatTheBlameBundle.message("failure.network", failure.cause.message ?: ""))
            is BlameFailure.Other ->
                renderer.setText("> " + WhatTheBlameBundle.message("failure.other", failure.cause.message ?: ""))
        }
    }

    private fun showSettingsLink() {
        errorLink.isVisible = true
        errorLink.setHyperlinkText(WhatTheBlameBundle.message("toolWindow.openSettings"))
        errorLink.addHyperlinkListener { openSettings() }
    }

    private fun openSettings() {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, WhatTheBlameConfigurable::class.java)
    }

    private fun runOnEdt(block: () -> Unit) {
        if (ApplicationManager.getApplication().isDispatchThread) block()
        else ApplicationManager.getApplication().invokeLater(block)
    }
}
