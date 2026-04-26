package com.github.michaelanisimov.whattheblame.git

import com.github.michaelanisimov.whattheblame.BlameFailure
import com.github.michaelanisimov.whattheblame.git.model.LineHistory

sealed interface LineHistoryResult {
    data class Ok(val history: LineHistory) : LineHistoryResult
    data class Failure(val failure: BlameFailure) : LineHistoryResult
}
