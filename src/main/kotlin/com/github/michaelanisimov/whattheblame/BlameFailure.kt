package com.github.michaelanisimov.whattheblame

sealed interface BlameFailure {
    data object NoApiKey : BlameFailure
    data object InvalidApiKey : BlameFailure
    data object RateLimited : BlameFailure
    data object FileNotInGit : BlameFailure
    data object NoCommitsForRange : BlameFailure
    data class Network(val cause: Throwable) : BlameFailure
    data class Other(val cause: Throwable) : BlameFailure
}
