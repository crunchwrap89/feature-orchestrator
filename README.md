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
Today you must keep track of your backlog in a separate tool and write your prompts based on what's in your backlog.
Then you need to verify the results manually, and you need to write new prompts to fix what’s missing if something is wrong. You also need to keep your backlog in sync manually.

**Feature Orchestrator** makes everything in this process easier.

---

## What it does

Feature Orchestrator generates a `BACKLOG.md` file to your project. Here you can plan your features with **clear acceptance criterias**.

- Cycle through your features and choose which one to implement next.
- Press Implement feature to generate a **clear, structured AI prompt** for the selected feature.
- Depending on your plugin settings it will either just copy the prompt to your clipboard or open your AI assistant with the prompt pre-filled.
- Let your AI Agent complete the feature.
- Press **Verify implementation** and the plugin will check if all acceptance criterias are fulfilled (by running tests, build commands or file checks.)
- If acceptance criterias are NOT fulfilled, it will generate a new prompt for you to hand to the AI Agent to fix what’s missing.
- If acceptance criterias are fulfilled, it will mark the feature as completed.

Works seemless with your current AI Tools.  
Simple backlog management with git versioning.  
100% Free and Open Source.

---

### Feature Orchestrator binds it all together:

> **Feature → AI work → Verification → Backlog state**

This turns AI-assisted coding into a **repeatable, trustworthy workflow** with less time in ticket management tools and less time writing prompts.

---

## Designed for developers

- Works entirely with all IntelliJ based IDEs, including Webstorm.
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
- Anyone tired of writing prompts from scratch

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
