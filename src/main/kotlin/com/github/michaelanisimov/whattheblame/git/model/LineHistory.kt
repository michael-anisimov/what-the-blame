package com.github.michaelanisimov.whattheblame.git.model

import com.github.michaelanisimov.whattheblame.git.BlameTarget

data class LineHistory(
    val target: BlameTarget,
    val commits: List<CommitTouch>,
    val blame: List<BlameLine>,
)
