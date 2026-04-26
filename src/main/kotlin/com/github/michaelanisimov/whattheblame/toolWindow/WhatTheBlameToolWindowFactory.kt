package com.github.michaelanisimov.whattheblame.toolwindow

import com.github.michaelanisimov.whattheblame.WhatTheBlameBundle
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class WhatTheBlameToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val controller = project.service<WhatTheBlameToolWindowController>()
        val panel = WhatTheBlamePanel(project)
        controller.attach(panel)
        val content = ContentFactory.getInstance().createContent(
            panel,
            WhatTheBlameBundle.message("toolWindow.title"),
            false,
        )
        toolWindow.contentManager.addContent(content)
    }
}
