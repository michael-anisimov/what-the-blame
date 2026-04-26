package com.github.michaelanisimov.whattheblame.llm

import com.github.michaelanisimov.whattheblame.BlameFailure

sealed interface NarrationEvent {
    data class TextDelta(val text: String) : NarrationEvent
    data class Done(val totalChars: Int) : NarrationEvent
    data class Failed(val failure: BlameFailure) : NarrationEvent
}
