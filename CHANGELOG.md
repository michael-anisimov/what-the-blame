<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# what-the-blame Changelog

## [Unreleased]

### Added

- **What the Blame?** action: select code in the editor, right-click, and Claude streams a narrative explaining what those lines do, who shaped them, and an inferred *why* — based on `git log -L`, `git blame`, and the commit messages for the selected line range.
- Action contributed to the editor popup, console popup, and `Vcs.Operations.Popup`.
- `WhatTheBlame` tool window (right anchor) renders the streaming markdown response via flexmark into a `JEditorPane`.
- Settings panel under <kbd>Tools</kbd> → <kbd>What the Blame</kbd>: Anthropic API key (stored in `PasswordSafe`), model selection (`claude-sonnet-4-6` / `claude-opus-4-7` / `claude-haiku-4-5`), max commits to include, and an option to attach per-commit diff hunks.
- Cacheable system prompt so repeated narrations on the same project benefit from Anthropic's prompt cache.
- Graceful failure rendering for missing API key, invalid key, rate limiting, files outside a git repo, empty histories, and network errors — including a Settings link inline in the panel when an API key is needed.
- Notification group `WhatTheBlame` for non-blocking feedback (e.g. missing API key).
- Bundled `Git4Idea` dependency for VCS access.

### Changed

- Replaced the IntelliJ Platform Plugin Template scaffold (`MyBundle`, `MyProjectService`, `MyProjectActivity`) with the plugin's own modules under `com.github.michaelanisimov.whattheblame.{action,git,llm,settings,toolwindow}`.
