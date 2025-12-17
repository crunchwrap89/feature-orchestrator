package com.github.crunchwrap89.awebstormplugin.featureorchestrator.backlog

import com.github.crunchwrap89.awebstormplugin.featureorchestrator.model.Backlog
import com.github.crunchwrap89.awebstormplugin.featureorchestrator.model.BacklogFeature
import com.github.crunchwrap89.awebstormplugin.featureorchestrator.settings.CompletionBehavior
import com.github.crunchwrap89.awebstormplugin.featureorchestrator.settings.OrchestratorSettings
import com.intellij.openapi.application.WriteAction
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
                        val oldLineRegex = Regex("""^- \[ ] +${Regex.escape(feature.name)}\s*$""", RegexOption.MULTILINE)
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
