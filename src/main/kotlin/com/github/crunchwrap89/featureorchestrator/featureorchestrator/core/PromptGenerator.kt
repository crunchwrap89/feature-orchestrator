package com.github.crunchwrap89.featureorchestrator.featureorchestrator.core

import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.AcceptanceCriterion
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.BacklogFeature
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.Section

object PromptGenerator {
    fun generate(feature: BacklogFeature): String {
        val sb = StringBuilder()
        sb.appendLine("Implement Feature: ${feature.name}")
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

        return sb.toString().trimEnd()
    }

    fun generateFailurePrompt(feature: BacklogFeature, failures: List<FailureDetail>): String {
        val sb = StringBuilder()
        sb.appendLine("The implementation of feature '${feature.name}' failed verification.")
        sb.appendLine()
        sb.appendLine("The following acceptance criteria failed:")
        failures.forEach { f ->
            sb.appendLine("- Criterion: ${describe(f.criterion)}")
            sb.appendLine("  Error Details:")
            sb.appendLine(f.message.prependIndent("    "))
            sb.appendLine()
        }
        sb.appendLine("Please fix the implementation to satisfy these criteria.")
        return sb.toString()
    }

    private fun describe(c: AcceptanceCriterion): String = when (c) {
        is AcceptanceCriterion.FileExists -> "File exists: ${c.relativePath}"
        is AcceptanceCriterion.CommandSucceeds -> "Command succeeds: ${c.command}"
    }
}
