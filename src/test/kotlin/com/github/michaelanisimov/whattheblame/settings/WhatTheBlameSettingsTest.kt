package com.github.michaelanisimov.whattheblame.settings

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.xmlb.XmlSerializer

class WhatTheBlameSettingsTest : BasePlatformTestCase() {

    fun `test default state values`() {
        val state = WhatTheBlameSettings.State()
        assertEquals(WhatTheBlameSettings.DEFAULT_MODEL, state.model)
        assertEquals(20, state.maxCommits)
        assertTrue(state.includeDiffHunks)
    }

    fun `test xml serialization round trips`() {
        val original = WhatTheBlameSettings.State(
            model = "claude-opus-4-7",
            maxCommits = 7,
            includeDiffHunks = false,
        )
        val element = XmlSerializer.serialize(original)
        val restored = XmlSerializer.deserialize(element, WhatTheBlameSettings.State::class.java)

        assertEquals(original.model, restored.model)
        assertEquals(original.maxCommits, restored.maxCommits)
        assertEquals(original.includeDiffHunks, restored.includeDiffHunks)
    }

    fun `test loadState replaces persisted values`() {
        val settings = WhatTheBlameSettings()
        settings.loadState(
            WhatTheBlameSettings.State(model = "claude-haiku-4-5", maxCommits = 3, includeDiffHunks = false),
        )
        assertEquals("claude-haiku-4-5", settings.state.model)
        assertEquals(3, settings.state.maxCommits)
        assertFalse(settings.state.includeDiffHunks)
    }
}
