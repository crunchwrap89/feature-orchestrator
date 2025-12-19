# Feature Orchestrator IntelliJ Platform Plugin

![Build](https://github.com/crunchwrap89/feature-orchestrator/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/29407.svg)](https://plugins.jetbrains.com/plugin/29407)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/29407.svg)](https://plugins.jetbrains.com/plugin/29407)

<!-- Plugin description -->
> ** Plan → Generate prompt → AI Work → Verify implementation → Mark as completed **

A repeatable and mostly automatic workflow to plan, generate prompts, verify implementations and manage your backlog without leaving your IDE.

- Today you write prompts and refine your backlog as two different work tasks, why not combine the two?
- Today you go to a separate tool, such as jira or trello to find out what feature to work on next. Why not keep track of it in your IDE and in your codebase?
- Today you feed your AI and verify the result with no fixed strategy, why not have a repeatable workflow for it?
- Today you write follow-up prompts manually if something fails, why not automate that prompt generation?
- Today you return to your ticket management tool to manually mark your feature as done. Why not automate this? 

Feature Orchestrator fixes all of this by combining feature planning, AI prompt generation, implementation verification and backlog management into a single, repeatable workflow so you can spend less time writing AI Prompts while also spending less time in a bloated ticket management system.


---

## How it works

Feature Orchestrator generates a `BACKLOG.md` where you can plan your features with **clear acceptance criterias**.

- Cycle through your features and choose which one to implement next.
- Press Generate prompt to create a **clear, structured AI prompt** for the selected feature.
- Depending on your plugin settings, it will either just copy the prompt to your clipboard or open your AI assistant with the prompt pre-filled.
- Let your AI Agent complete the feature.
- Press **Verify implementation** and the plugin will check if all Acceptance Criteria are fulfilled (by running tests, build commands or file checks.)
- If acceptance criteria are NOT fulfilled, it will generate a new prompt for you to hand to the AI Agent.
- If acceptance criteria are fulfilled, it will mark the feature as completed.


Works seamless with your current AI Tools.  
Simple backlog management with Markdown and git versioning.  
100% Free and Open Source.

---
## Designed for developers

- Works entirely with all IntelliJ-based IDEs, including Webstorm.
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
- Anyone who wants to save time and mental energy when working with AI

---

## What it is *not*

- Not an autonomous agent
- Not a background task runner
- Not another AI chat interface

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
