package com.github.crunchwrap89.awebstormplugin.featureorchestrator.backlog

import com.github.crunchwrap89.awebstormplugin.featureorchestrator.model.Backlog
import com.github.crunchwrap89.awebstormplugin.featureorchestrator.model.BacklogFeature
import com.github.crunchwrap89.awebstormplugin.featureorchestrator.settings.CompletionBehavior
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile

@Service(Service.Level.PROJECT)
class BacklogService(private val project: Project) {
    private val log = Logger.getInstance(BacklogService::class.java)

    fun backlogFile(): VirtualFile? {
        val roots = ProjectRootManager.getInstance(project).contentRoots
        for (root in roots) {
            val file = root.findChild("backlog.md")
            if (file != null) return file
        }

        // Fallback: check project base path directly
        val basePath = project.basePath
        if (basePath != null) {
            val baseDir = LocalFileSystem.getInstance().findFileByPath(basePath)
            val file = baseDir?.findChild("backlog.md")
            if (file != null) return file
        }

        return null
    }

    fun readBacklogText(): String? = backlogFile()?.let { String(it.contentsToByteArray(), Charsets.UTF_8) }

    fun parseBacklog(): Backlog? = readBacklogText()?.let { BacklogParser.parse(it) }

    fun firstUncheckedFeature(backlog: Backlog): BacklogFeature? = backlog.features.firstOrNull { !it.checked }

    fun markCompletedOrRemove(feature: BacklogFeature, behavior: CompletionBehavior): Boolean {
        val vf = backlogFile() ?: return false
        val doc = FileDocumentManager.getInstance().getDocument(vf) ?: return false
        return try {
            WriteCommandAction.runWriteCommandAction(project, "Update Backlog", null, Runnable {
                val text = doc.text
                val parsed = BacklogParser.parse(text)
                val target = parsed.features.firstOrNull { !it.checked && it.name == feature.name }
                if (target == null) {
                    log.warn("Feature '${feature.name}' not found or already checked. Skipping update.")
                    return@Runnable
                }
                when (behavior) {
                    CompletionBehavior.CHECK_OFF -> {
                        // Try to find line with checkbox first
                        var oldLineRegex = Regex("""^- \[ ] +${Regex.escape(feature.name)}\s*$""", RegexOption.MULTILINE)
                        if (!oldLineRegex.containsMatchIn(text)) {
                            // Try without checkbox
                            oldLineRegex = Regex("""^- +${Regex.escape(feature.name)}\s*$""", RegexOption.MULTILINE)
                        }
                        val newText = text.replace(oldLineRegex, "- [x] ${feature.name}")
                        doc.setText(newText)
                    }
                    CompletionBehavior.REMOVE_FEATURE -> {
                        // Remove the entire block from start to just before the next ## Feature or EOF
                        val blockText = target.rawBlock
                        val startIdx = text.indexOf(blockText)
                        if (startIdx >= 0) {
                            var endIdx = startIdx + blockText.length
                            // remove trailing newlines too
                            while (endIdx < text.length && (text[endIdx] == '\n' || text[endIdx] == '\r')) endIdx++
                            val newText = text.removeRange(startIdx, endIdx)
                            doc.setText(newText)
                        }
                    }
                    CompletionBehavior.MOVE_TO_COMPLETED -> {
                        // 1. Append to completed.md
                        val parentDir = vf.parent
                        var completedFile = parentDir.findChild("completed.md")
                        if (completedFile == null) {
                            completedFile = parentDir.createChildData(this, "completed.md")
                            val d = FileDocumentManager.getInstance().getDocument(completedFile)
                            d?.setText("# Completed Features\n\n---\n")
                        }
                        val completedDoc = FileDocumentManager.getInstance().getDocument(completedFile)
                        if (completedDoc != null) {
                            val currentText = completedDoc.text
                            var prefix = ""
                            if (currentText.isNotEmpty() && !currentText.endsWith("\n")) prefix += "\n"

                            // Mark as checked in the moved block
                            var checkedBlock = target.rawBlock
                            if (checkedBlock.contains("- [ ]")) {
                                checkedBlock = checkedBlock.replaceFirst("- [ ]", "- [x]")
                            } else {
                                // If no checkbox, add one
                                checkedBlock = checkedBlock.replaceFirst("- ${feature.name}", "- [x] ${feature.name}")
                            }
                            completedDoc.setText(currentText + prefix + checkedBlock + "\n\n---")
                            FileDocumentManager.getInstance().saveDocument(completedDoc)
                        }

                        // 2. Remove from backlog.md
                        val blockText = target.rawBlock
                        val startIdx = text.indexOf(blockText)
                        if (startIdx >= 0) {
                            var endIdx = startIdx + blockText.length
                            // remove trailing newlines too
                            while (endIdx < text.length && (text[endIdx] == '\n' || text[endIdx] == '\r')) endIdx++
                            val newText = text.removeRange(startIdx, endIdx)
                            doc.setText(newText)
                        }
                    }
                }
                FileDocumentManager.getInstance().saveDocument(doc)
            })
            true
        } catch (e: Exception) {
            log.warn("Failed to update backlog.md", e)
            false
        }
    }
}
