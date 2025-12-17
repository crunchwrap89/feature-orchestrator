package com.github.crunchwrap89.awebstormplugin.featureorchestrator.backlog

import com.github.crunchwrap89.awebstormplugin.featureorchestrator.model.*
import com.intellij.openapi.diagnostic.Logger

object BacklogParser {
    private val log = Logger.getInstance(BacklogParser::class.java)

    private val featureHeaderRegex = Regex("""^## +Feature\s*$""")
    private val nameRegex = Regex("""^- \[( |x|X)] +(.+)$""")
    private val sectionHeaderRegex = Regex("""^### +(.+)$""")

    fun parse(text: String): Backlog {
        val lines = text.lines()
        val features = mutableListOf<BacklogFeature>()
        val warnings = mutableListOf<String>()

        var i = 0
        while (i < lines.size) {
            if (!featureHeaderRegex.matches(lines[i])) { i++; continue }
            val blockStart = i
            i++
            if (i >= lines.size || !nameRegex.matches(lines[i])) {
                warnings += warning(blockStart, "Missing feature checkbox line after '## Feature'. Skipping block.")
                i++
                continue
            }
            val nameMatch = nameRegex.matchEntire(lines[i])!!
            val checked = nameMatch.groupValues[1].equals("x", ignoreCase = true)
            val name = nameMatch.groupValues[2].trim()
            i++

            // Skip blank lines
            while (i < lines.size && lines[i].isBlank()) {
                i++
            }

            // Expect Description header
            if (i >= lines.size || !sectionHeaderRegex.matches(lines[i]) || !lines[i].trim().equals("### Description", ignoreCase = true)) {
                warnings += warning(blockStart, "Missing '### Description' section. Skipping feature '$name'.")
                // skip till next feature
                i = skipToNextFeature(lines, i)
                continue
            }
            i++
            val descriptionBuilder = StringBuilder()
            while (i < lines.size && !featureHeaderRegex.matches(lines[i]) && !(sectionHeaderRegex.matches(lines[i]))) {
                descriptionBuilder.appendLine(lines[i])
                i++
            }
            val description = descriptionBuilder.toString().trim()
            if (description.isBlank()) {
                warnings += warning(blockStart, "Empty description for feature '$name'. Skipping.")
                i = skipToNextFeature(lines, i)
                continue
            }

            val optionalSections = mutableMapOf<Section, String>()
            val acceptanceCriteria = mutableListOf<AcceptanceCriterion>()

            while (i < lines.size && !featureHeaderRegex.matches(lines[i])) {
                if (!sectionHeaderRegex.matches(lines[i])) { i++; continue }
                val secTitle = lines[i].removePrefix("### ").trim()
                i++
                val content = StringBuilder()
                while (i < lines.size && !featureHeaderRegex.matches(lines[i]) && !sectionHeaderRegex.matches(lines[i])) {
                    content.appendLine(lines[i])
                    i++
                }
                val body = content.toString().trim()
                when (secTitle.lowercase()) {
                    "requirements" -> optionalSections[Section.REQUIREMENTS] = body
                    "out of scope", "out-of-scope", "out_of_scope" -> optionalSections[Section.OUT_OF_SCOPE] = body
                    "acceptance criteria", "acceptance-criteria", "acceptance_criteria" -> {
                        optionalSections[Section.ACCEPTANCE_CRITERIA] = body
                        acceptanceCriteria += parseCriteria(body, warnings)
                    }
                    "notes" -> optionalSections[Section.NOTES] = body
                    "context" -> optionalSections[Section.CONTEXT] = body
                    else -> {
                        warnings += warning(blockStart, "Unknown section '### $secTitle' in feature '$name'. Ignored.")
                    }
                }
            }

            // Compute raw block boundaries
            val blockEnd = (i - 1).coerceAtLeast(blockStart)
            val rawBlock = lines.subList(blockStart, blockEnd + 1).joinToString("\n")
            features += BacklogFeature(
                name = name,
                checked = checked,
                description = description,
                optionalSections = optionalSections.toMap(),
                acceptanceCriteria = acceptanceCriteria.toList(),
                rawBlock = rawBlock,
                blockStartOffset = startOffset(text, blockStart),
                blockEndOffset = endOffset(text, blockEnd),
            )
        }

        return Backlog(features, warnings)
    }

    private fun parseCriteria(body: String, warnings: MutableList<String>): List<AcceptanceCriterion> {
        val list = mutableListOf<AcceptanceCriterion>()
        body.lines().forEachIndexed { idx, line ->
            val trimmed = line.trim()
            if (trimmed.isBlank()) return@forEachIndexed
            if (trimmed.startsWith("---")) return@forEachIndexed // Ignore separators
            when {
                trimmed.startsWith("• ") || trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val spec = trimmed.drop(2).trim()
                    when {
                        spec.startsWith("File exists:", ignoreCase = true) -> {
                            list += AcceptanceCriterion.FileExists(spec.substringAfter(":").trim())
                        }
                        spec.startsWith("Command succeeds:", ignoreCase = true) -> {
                            list += AcceptanceCriterion.CommandSucceeds(spec.substringAfter(":").trim())
                        }
                        // Ignore other criteria without warning
                    }
                }
                else -> {
                    // Ignore other lines without warning
                }
            }
        }
        return list
    }

    private fun startOffset(text: String, lineIndex: Int): Int {
        if (lineIndex <= 0) return 0
        var idx = 0
        var i = 0
        while (i < lineIndex && idx < text.length) {
            val nl = text.indexOf('\n', idx)
            if (nl == -1) return text.length
            idx = nl + 1
            i++
        }
        return idx
    }

    private fun endOffset(text: String, lineIndex: Int): Int {
        if (lineIndex < 0) return 0
        var idx = 0
        var i = 0
        while (i <= lineIndex && idx < text.length) {
            val nl = text.indexOf('\n', idx)
            if (nl == -1) return text.length
            idx = nl + 1
            i++
        }
        return (idx - 1).coerceAtLeast(0)
    }

    private fun skipToNextFeature(lines: List<String>, start: Int): Int {
        var i = start
        while (i < lines.size && !featureHeaderRegex.matches(lines[i])) i++
        return i
    }

    private fun warning(location: Int, msg: String): String = "[backlog.md:${location + 1}] $msg"
}
