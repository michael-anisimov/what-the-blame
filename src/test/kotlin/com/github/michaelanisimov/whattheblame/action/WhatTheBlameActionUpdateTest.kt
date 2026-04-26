package com.github.michaelanisimov.whattheblame.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class WhatTheBlameActionUpdateTest : BasePlatformTestCase() {

    fun `test action is disabled when there is no selection`() {
        myFixture.configureByText("Foo.kt", "fun bar() = 1")
        val action = WhatTheBlameAction()

        val event = newEvent(action)
        action.update(event)
        assertFalse("must be disabled without selection", event.presentation.isEnabledAndVisible)
    }

    fun `test action is disabled when file is not under git`() {
        myFixture.configureByText("Foo.kt", "fun bar() = 1\nfun baz() = 2\n")
        myFixture.editor.selectionModel.setSelection(0, 14)
        val action = WhatTheBlameAction()

        val event = newEvent(action)
        action.update(event)
        // The fixture's tmp dir is not a git repo
        assertFalse("must be disabled outside a git repo", event.presentation.isEnabledAndVisible)
    }

    private fun newEvent(action: WhatTheBlameAction): AnActionEvent {
        val dc = SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, project)
            .add(CommonDataKeys.EDITOR, myFixture.editor)
            .add(CommonDataKeys.VIRTUAL_FILE, myFixture.file?.virtualFile)
            .build()
        return TestActionEvent.createTestEvent(action, dc)
    }
}
