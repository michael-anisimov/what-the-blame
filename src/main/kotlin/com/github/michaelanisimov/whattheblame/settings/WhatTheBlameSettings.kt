package com.github.michaelanisimov.whattheblame.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.APP)
@State(name = "WhatTheBlameSettings", storages = [Storage("whatTheBlame.xml")])
class WhatTheBlameSettings : PersistentStateComponent<WhatTheBlameSettings.State> {

    data class State(
        var model: String = DEFAULT_MODEL,
        var maxCommits: Int = 20,
        var includeDiffHunks: Boolean = true,
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(s: State) {
        XmlSerializerUtil.copyBean(s, state)
    }

    companion object {
        const val DEFAULT_MODEL = "claude-sonnet-4-6"

        val AVAILABLE_MODELS = listOf(
            "claude-sonnet-4-6",
            "claude-opus-4-7",
            "claude-haiku-4-5",
        )

        fun get(): WhatTheBlameSettings = service()
    }
}
