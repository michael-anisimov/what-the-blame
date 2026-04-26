package com.github.michaelanisimov.whattheblame.git.model

import java.time.Instant

data class CommitTouch(
    val hash: String,
    val shortHash: String,
    val author: String,
    val authorEmail: String,
    val authorDate: Instant,
    val subject: String,
    val body: String,
    val hunkForRange: String?,
)
