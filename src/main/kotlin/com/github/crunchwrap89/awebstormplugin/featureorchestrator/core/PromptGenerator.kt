package com.github.crunchwrap89.awebstormplugin.featureorchestrator.core

import com.github.crunchwrap89.awebstormplugin.featureorchestrator.model.AcceptanceCriterion
import com.github.crunchwrap89.awebstormplugin.featureorchestrator.model.BacklogFeature
import com.github.crunchwrap89.awebstormplugin.featureorchestrator.model.Section

object PromptGenerator {
    fun generate(feature: BacklogFeature): String {
        val sb = StringBuilder()
        sb.appendLine("Feature Orchestrator Prompt (WebStorm)")
        sb.appendLine()
        sb.appendLine("Feature: ${feature.name}")
        sb.appendLine()
        sb.appendLine("Description:")
        sb.appendLine(feature.description.trim())
        sb.appendLine()

        fun appendIfPresent(section: Section, title: String) {
            val body = feature.optionalSections[section]?.trim().orEmpty()
            if (body.isNotBlank()) {
                sb.appendLine("$title:")
                sb.appendLine(body)
                sb.appendLine()
            }
        }

        appendIfPresent(Section.REQUIREMENTS, "Requirements")
        appendIfPresent(Section.OUT_OF_SCOPE, "Out of Scope")
        appendIfPresent(Section.NOTES, "Notes")
        appendIfPresent(Section.CONTEXT, "Context")

        if (feature.acceptanceCriteria.isNotEmpty()) {
            sb.appendLine("Acceptance Criteria (DO NOT MODIFY):")
            feature.acceptanceCriteria.forEach { c ->
                when (c) {
                    is AcceptanceCriterion.FileExists -> sb.appendLine("- File exists: ${c.relativePath}")
                    is AcceptanceCriterion.CommandSucceeds -> sb.appendLine("- Command succeeds: ${c.command}")
                }
            }
            sb.appendLine()
        }

        sb.appendLine("Instructions:")
        sb.appendLine("- Only work on THIS feature.")
        sb.appendLine("- Do NOT modify backlog.md.")
        sb.appendLine("- Make minimal, correct changes.")
        sb.appendLine("- Stop when complete.")
        return sb.toString().trimEnd()
    }
}
