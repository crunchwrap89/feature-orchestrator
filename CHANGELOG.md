# Feature Orchestrator Changelog

## [Unreleased]

### Features
- Add a button for mark as completed
- Add ability to toggle preview acceptance criteria
- Add ability to add empty feature or configured template feature.
- Add ability to preview and cycle through features that are missing information. (for example empty templates)
- Start at Feature name with cursor when adding new feature

### Fixed
- Fixed bug were some buttons were not visible at smaller widths.

## [1.1.8] - 2025-12-24

### Added

- Improved default feature template
- add the ability to add, edit and remove feature from the backlog.

## [1.1.7] - 2025-12-20

### Fixed

- Fixed issue with copilot chat window not opening

## [1.1.6] - 2025-12-19

### Fixed

- Fixed readme

## [1.1.5] - 2025-12-19

### Fixed

- Resolved deprecated API usage warnings
- Added tests
- Fixed copilot auto open chat window

## [1.1.4] - 2025-12-19

### Added

- Added logo to Marketplace listing
- Updated readme

## [1.1.3] - 2025-12-19

### Fixed

- Fixed issue with settings not being saved properly

## [1.1.2] - 2025-12-18

### Fixed

- Fixed build failure related to PluginManagerConfigurable
- Fixed initial state issue when backlog is missing

## [1.1.1] - 2025-12-18

### Fixed

- Status initially "Awaiting AI Agent"

## [1.1.0] - 2025-12-18

### Added

- Enchanced UI
- Improved execution log
- Improved automatic validation
- Added more settings options
- Fixed minor bugs

## [1.0.0] - 2025-12-17

### Added

- MVP of Feature Orchestrator plugin for JetBrains IDEs.
- Automatically creates a BACKLOG.md file if not present in the project.
- Cycle through features in BACKLOG.md and press the Generate prompt button to generate AI prompts.
- Verify feature completion based on acceptance criteria using tests, build commands, or file presence.
- Mark features as completed in BACKLOG.md when acceptance criteria are met.
- Generate follow-up prompts for incomplete features to guide further AI work.
- Plugin settings to customize behavior and preferences.
- Have your features tracked in Git and easily check which changes are related to which feature.

[Unreleased]: https://github.com/crunchwrap89/feature-orchestrator/compare/v1.1.8...HEAD
[1.1.8]: https://github.com/crunchwrap89/feature-orchestrator/compare/v1.1.7...v1.1.8
[1.1.7]: https://github.com/crunchwrap89/feature-orchestrator/compare/v1.1.6...v1.1.7
[1.1.6]: https://github.com/crunchwrap89/feature-orchestrator/compare/v1.1.5...v1.1.6
[1.1.5]: https://github.com/crunchwrap89/feature-orchestrator/compare/v1.1.4...v1.1.5
[1.1.4]: https://github.com/crunchwrap89/feature-orchestrator/compare/v1.1.3...v1.1.4
[1.1.3]: https://github.com/crunchwrap89/feature-orchestrator/compare/v1.1.2...v1.1.3
[1.1.2]: https://github.com/crunchwrap89/feature-orchestrator/compare/v1.1.1...v1.1.2
[1.1.1]: https://github.com/crunchwrap89/feature-orchestrator/compare/v1.1.0...v1.1.1
[1.1.0]: https://github.com/crunchwrap89/feature-orchestrator/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/crunchwrap89/feature-orchestrator/commits/v1.0.0
