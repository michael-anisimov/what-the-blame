package com.github.michaelanisimov.whattheblame.action

import com.github.michaelanisimov.whattheblame.WhatTheBlameBundle
import com.github.michaelanisimov.whattheblame.WhatTheBlameIcons
import com.github.michaelanisimov.whattheblame.git.BlameTarget
import com.github.michaelanisimov.whattheblame.toolwindow.WhatTheBlameToolWindowController
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import git4idea.repo.GitRepositoryManager

class WhatTheBlameAction : AnAction(
    WhatTheBlameBundle.messagePointer("action.whatTheBlame.text"),
    WhatTheBlameBundle.messagePointer("action.whatTheBlame.description"),
    WhatTheBlameIcons.Action,
) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val hasSelection = editor?.selectionModel?.hasSelection() == true
        val inGit = project != null && file != null &&
            GitRepositoryManager.getInstance(project).getRepositoryForFileQuick(file) != null
        e.presentation.isEnabledAndVisible = hasSelection && inGit
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val target = BlameTarget.from(editor, file) ?: return
        project.service<WhatTheBlameToolWindowController>().analyze(target)
    }
}
