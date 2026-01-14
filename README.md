# Feature Orchestrator IntelliJ Platform Plugin

![Build](https://github.com/crunchwrap89/feature-orchestrator/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/29407.svg)](https://plugins.jetbrains.com/plugin/29407)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/29407.svg)](https://plugins.jetbrains.com/plugin/29407)

<!-- Plugin description -->
Feature Orchestrator simplifies your development workflow by combining backlog management, Agent Skill selection, and AI prompt generation into a single, repeatable process.

- **Combines prompt writing and backlog refinement** into a single workflow.
- **Keeps the feature backlog inside the IDE**, versioned alongside the codebase (no external tools required).
- **Lets you select AI Agent skills** and documentation from a predefined in-IDE list.
- **Quickly download the latest AI Agent skills** from the Claude Code skills repository.
- **Preview, Add, Edit, Delete and Mark Features as Completed** with ease.
- **Works seamlessly with JetBrains AI Assistant**.
- **Configurable feature templates** for consistent planning.

> ** Plan in IDE → Select Agent Skills → Generate prompt → AI Assistant → Mark as completed **

---

## How it works

Feature Orchestrator uses a `BACKLOG.md` file in your project root where you can plan your features using simple Markdown.

- **Manage Backlog**: Add, edit, and navigate through your features directly within the IDE tool window.
- **Select Agent Skills**: Choose from available Agent Skills (documentation, style guides, or specialized instructions) to include in your prompt.
- **Sync Skills**: Download the latest Agent Skills from the Claude Code repository to keep your AI prompts optimized.
- **Generate Prompt**: Create a structured AI prompt that includes the feature description and selected skills.
- **AI Handoff**: Automatically copy the prompt to your clipboard or open the AI Assistant with the prompt ready to go.
- **Mark as Completed**: Once the work is done, mark the feature as completed. The plugin handles checking off the feature or moving it to a `COMPLETED.md` file.
- **Configurable Templates**: Customize the default template for new features in settings to fit your team's needs.

---
## Designed for developers

- **IDE Integrated**: Works with all IntelliJ-based IDEs.
- **AI Ready**: Deeply integrated with JetBrains AI Assistant.
- **Transparent**: Fully transparent execution logs.
- **Manual Control**: Explicit user actions only—no autonomous changes to your code.

Feature Orchestrator never runs on its own and never modifies your code or planning files via AI without your command.

---

## Who it’s for

- Solo developers and small teams shipping fast.
- Developers who want to keep their planning close to their code.
- Teams using JetBrains AI Assistant.
- Anyone tired of manually keeping backlogs in sync with reality.
- Anyone who wants to save mental energy by using a repeatable prompt generation workflow.

---

## What it is *not*

- Not an autonomous agent.
- Not a background task runner.
- Not another AI chat interface.

It’s a **Feature orchestrator**. 

---
<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Feature Orchestrator"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/crunchwrap89/feature-orchestrator/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
