package com.github.michaelanisimov.whattheblame.git.model

import java.time.Instant

data class BlameLine(
    val lineNumber: Int,
    val author: String,
    val authorEmail: String,
    val date: Instant?,
    val commitHash: String,
)
