# Feature Orchestrator IntelliJ Platform Plugin

![Build](https://github.com/crunchwrap89/feature-orchestrator/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

## Template ToDo list
- [ ] [Publish a plugin manually](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate) for the first time.
- [ ] Set the `MARKETPLACE_ID` in the above README badges. You can obtain it once the plugin is published to JetBrains Marketplace.
- [ ] Set the [Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html?from=IJPluginTemplate) related [secrets](https://github.com/JetBrains/intellij-platform-plugin-template#environment-variables).
- [ ] Set the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html?from=IJPluginTemplate).
- [ ] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified about releases containing new features and fixes.
- [ ] Configure the [CODECOV_TOKEN](https://docs.codecov.com/docs/quick-start) secret for automated test coverage reports on PRs

<!-- Plugin description -->

# Feature Orchestrator
**Deterministic, feature-driven AI development for all IntelliJ Based IDE's**

AI assistants are great at writing code — but they don’t manage **what comes next**, **when something is actually done**, or **whether your backlog reflects reality**.

**Feature Orchestrator** fills that gap.

---

## What it does

Feature Orchestrator connects your project’s `backlog.md` to your existing AI tools (GitHub Copilot Chat or JetBrains AI Assistant) and lets you:

- Work on **one feature at a time**
- Generate a **clear, structured AI prompt** from your backlog
- Track which files changed while a feature was implemented
- **Verify completion** using acceptance criteria (tests, commands, file checks)
- Automatically **update your backlog** when a feature is done

No new AI to learn.  
No background automation.  
No surprise changes.

---

## Why this matters

Today’s AI tools are **stateless**.

They don’t know:
- Which feature you’re working on
- What “done” actually means
- Whether your backlog is accurate

That leads to:
- Half-finished features
- Stale TODO lists
- Constant manual supervision

Feature Orchestrator introduces a **missing layer**:

> **Feature → AI work → Verification → Backlog state**

This turns AI-assisted coding into a **repeatable, trustworthy workflow**.

---

## How it works

1. Define features in `backlog.md`
2. Click **Run Next Feature**
3. Paste the generated prompt into your AI assistant
4. Let the AI do the work
5. Click **Verify** and Acceptance criterias are checked via tests, build completion, or file presence.
6. Results:
  - If the acceptance criterias are all fulfilled, the feature is marked as completed. 
  - If the acceptance critierias are NOT fulfilled, it will generate a new prompt that you can use to fix what’s missing.
7. Repeat the process for next feature.

You stay in control at every step, but your AI work is now **structured, focused, and verifiable**.

---

## Designed for developers

- Works entirely inside WebStorm and other IntelliJ IDEs
- Uses your existing AI plugins
- Fully transparent execution logs
- Explicit user actions only

Feature Orchestrator never runs on its own and never modifies planning files via AI.

---

## Who it’s for

- Indie hackers and solo developers
- AI-first teams
- Developers shipping fast with Copilot or JetBrains AI
- Anyone tired of manually keeping backlogs in sync with reality

---

## What it is *not*

- Not an autonomous agent
- Not a background task runner
- Not another AI chat interface

It’s a **workflow orchestrator** — and that’s exactly why it works.

---

## Summary

**Feature Orchestrator helps AI actually finish features — and proves when they’re done.**

If you use AI to write code, but still care about correctness, structure, and momentum, this plugin is for you.


<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "a-webstorm-plugin"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/crunchwrap89/a-webstorm-plugin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
