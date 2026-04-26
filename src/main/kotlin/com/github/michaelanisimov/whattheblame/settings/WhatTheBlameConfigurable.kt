package com.github.michaelanisimov.whattheblame.settings

import com.github.michaelanisimov.whattheblame.WhatTheBlameBundle
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindIntValue
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toNullableProperty

class WhatTheBlameConfigurable :
    BoundConfigurable(WhatTheBlameBundle.message("settings.title")),
    SearchableConfigurable {

    private val settings = WhatTheBlameSettings.get()
    private val apiKeys = ApiKeyService.getInstance()
    private lateinit var keyField: JBPasswordField

    override fun getId(): String = "com.github.michaelanisimov.whattheblame.settings"

    override fun createPanel(): DialogPanel {
        val state = settings.state
        return panel {
            row(WhatTheBlameBundle.message("settings.apiKey")) {
                keyField = JBPasswordField()
                keyField.text = apiKeys.get().orEmpty()
                cell(keyField).columns(40).align(AlignX.FILL)
            }
            row("") {
                comment(WhatTheBlameBundle.message("settings.apiKey.help"))
            }
            row(WhatTheBlameBundle.message("settings.model")) {
                comboBox(WhatTheBlameSettings.AVAILABLE_MODELS)
                    .bindItem(state::model.toNullableProperty())
            }
            row(WhatTheBlameBundle.message("settings.maxCommits")) {
                spinner(1..100, 1).bindIntValue(state::maxCommits)
            }
            row {
                checkBox(WhatTheBlameBundle.message("settings.includeHunks"))
                    .bindSelected(state::includeDiffHunks)
            }
        }
    }

    override fun apply() {
        super<BoundConfigurable>.apply()
        apiKeys.set(String(keyField.password))
    }

    override fun reset() {
        super<BoundConfigurable>.reset()
        keyField.text = apiKeys.get().orEmpty()
    }

    override fun isModified(): Boolean =
        super<BoundConfigurable>.isModified() || String(keyField.password) != apiKeys.get().orEmpty()
}
