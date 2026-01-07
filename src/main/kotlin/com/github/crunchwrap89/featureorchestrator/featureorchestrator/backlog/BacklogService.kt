package com.github.crunchwrap89.featureorchestrator.featureorchestrator.backlog

import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.Backlog
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.BacklogFeature
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.settings.CompletionBehavior
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.settings.OrchestratorSettings
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile

@Service(Service.Level.PROJECT)
class BacklogService(private val project: Project) {
    private val log = Logger.getInstance(BacklogService::class.java)

    companion object {
        private val BACKLOG_HEADER = """
# Backlog
""".trimIndent()

        private val TEMPLATE_FEATURE = """
---
## Feature name
New Landing Page

### Description
Add a new "Landing" page for: nordicdroneops.com
The main purpose of the website is to inform visitors about our consulting services where eyes are needed below the water surface or from the sky.

We target industries such as aquaculture, environmental monitoring, infrastructure inspection, and maritime research.

The landing page should effectively communicate our value proposition, showcase our expertise, and provide clear calls to action for potential clients to get in touch or learn more about our services.
We also sell drones and underwater ROVs, so there should be a section highlighting our products and strategic partnerships.

### Requirements
- The page should be accessible at `/`.
- Include the following sections:
  - Header with site title, tagline, and navigation menu  
  - Hero section to capture users’ interest with primary CTAs  
  - About section  
  - Product highlights with a link to the product catalog  
  - Services overview  
  - Client testimonials  
  - Contact form or contact information  
  - Footer with links to the privacy policy, terms of service, and social media profiles  
- Add SEO meta-tags for title, description, and keywords.
- Ensure fast load times and optimized performance.
- Must be responsive on both mobile and desktop devices.
- Use a modern design with a clean layout, consistent color scheme, and readable typography.
- Use only the latest version and styling patterns for Tailwind CSS.
- Define base styling in `main.css` for common elements (headings, paragraphs, etc.).
- Ensure full responsiveness across devices.

### Acceptance Criteria
- File exists: ./app/pages/index.vue
- Command succeeds: yarn build
- Command succeeds: yarn test:no-watch
- Route `/` renders without errors or warnings in the console
- Visual styling is up to modern standards and matches design requirements
---
""".trimIndent()

        private val EMPTY_FEATURE = """
---
## Feature name


### Description

---
""".trimIndent()
    }

    fun backlogFile(): VirtualFile? {
        val roots = ProjectRootManager.getInstance(project).contentRoots
        for (root in roots) {
            val file = root.findChild("BACKLOG.md") ?: root.findChild("backlog.md")
            if (file != null) return file
        }

        // Fallback: check project base path directly
        val basePath = project.basePath
        if (basePath != null) {
            val baseDir = LocalFileSystem.getInstance().findFileByPath(basePath)
            val file = baseDir?.findChild("BACKLOG.md") ?: baseDir?.findChild("backlog.md")
            if (file != null) return file
        }

        return null
    }

    fun readBacklogText(): String? = backlogFile()?.let { String(it.contentsToByteArray(), Charsets.UTF_8) }

    fun parseBacklog(): Backlog? = readBacklogText()?.let { BacklogParser.parse(it) }

    fun firstUncheckedFeature(backlog: Backlog): BacklogFeature? = backlog.features.firstOrNull { !it.checked }

    fun createTemplateBacklog() {
        val basePath = project.basePath ?: return
        val baseDir = LocalFileSystem.getInstance().findFileByPath(basePath) ?: return
        val template = project.service<OrchestratorSettings>().featureTemplate

        WriteCommandAction.runWriteCommandAction(project, "Create Backlog Template", null, {
            try {
                val file = baseDir.createChildData(this, "BACKLOG.md")
                val doc = FileDocumentManager.getInstance().getDocument(file)
                doc?.setText(BACKLOG_HEADER + "\n\n" + template)
                FileDocumentManager.getInstance().saveDocument(doc!!)
            } catch (e: Exception) {
                log.warn("Failed to create BACKLOG.md", e)
            }
        })
    }

    fun appendTemplateToBacklog() {
        val template = project.service<OrchestratorSettings>().featureTemplate
        appendContentToBacklog(template)
    }

    fun appendEmptyFeatureToBacklog() {
        appendContentToBacklog(EMPTY_FEATURE)
    }

    private fun appendContentToBacklog(content: String) {
        val file = backlogFile() ?: return
        val doc = FileDocumentManager.getInstance().getDocument(file) ?: return

        WriteCommandAction.runWriteCommandAction(project, "Append Backlog Template", null, {
            try {
                val text = doc.text
                val prefix = if (text.isNotEmpty() && !text.endsWith("\n")) "\n\n" else if (text.isNotEmpty()) "\n" else ""
                doc.setText(text + prefix + content)
                FileDocumentManager.getInstance().saveDocument(doc)
            } catch (e: Exception) {
                log.warn("Failed to append to backlog.md", e)
            }
        })
    }

    fun removeFeature(feature: BacklogFeature) {
        val vf = backlogFile() ?: return
        val doc = FileDocumentManager.getInstance().getDocument(vf) ?: return
        WriteCommandAction.runWriteCommandAction(project, "Remove Feature", null, {
            removeFeatureBlock(doc.text, feature.rawBlock, doc)
            FileDocumentManager.getInstance().saveDocument(doc)
        })
    }

    fun markCompletedOrRemove(feature: BacklogFeature, behavior: CompletionBehavior): Boolean {
        val vf = backlogFile() ?: return false
        val doc = FileDocumentManager.getInstance().getDocument(vf) ?: return false
        return try {
            WriteCommandAction.runWriteCommandAction(project, "Update Backlog", null, {
                val text = doc.text
                val parsed = BacklogParser.parse(text)
                val target = parsed.features.firstOrNull { !it.checked && it.name == feature.name }
                if (target == null) {
                    log.warn("Feature '${feature.name}' not found or already checked. Skipping update.")
                } else {
                    when (behavior) {
                        CompletionBehavior.CHECK_OFF -> {
                            // Try to find line with checkbox first
                            var oldLineRegex = Regex("""^\[ ] +${Regex.escape(feature.name)}\s*$""", RegexOption.MULTILINE)
                            if (!oldLineRegex.containsMatchIn(text)) {
                                // Try without checkbox
                                oldLineRegex = Regex("""^${Regex.escape(feature.name)}\s*$""", RegexOption.MULTILINE)
                            }
                            val newText = text.replace(oldLineRegex, "[x] ${feature.name}")
                            doc.setText(newText)
                        }
                        CompletionBehavior.REMOVE_FEATURE -> {
                            removeFeatureBlock(text, target.rawBlock, doc)
                        }
                        CompletionBehavior.MOVE_TO_COMPLETED -> {
                            // 1. Append to completed.md
                            val parentDir = vf.parent
                            var completedFile = parentDir.findChild("COMPLETED.md") ?: parentDir.findChild("completed.md")
                            if (completedFile == null) {
                                completedFile = parentDir.createChildData(this, "COMPLETED.md")
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
                                if (checkedBlock.contains("[ ]")) {
                                    checkedBlock = checkedBlock.replaceFirst("[ ]", "[x]")
                                } else {
                                    // If no checkbox, add one
                                    checkedBlock = checkedBlock.replaceFirst(feature.name, "[x] ${feature.name}")
                                }
                                completedDoc.setText(currentText + prefix + checkedBlock + "\n\n---")
                                FileDocumentManager.getInstance().saveDocument(completedDoc)
                            }

                            // 2. Remove from backlog.md
                            removeFeatureBlock(text, target.rawBlock, doc)
                        }
                    }
                    FileDocumentManager.getInstance().saveDocument(doc)
                }
            })
            true
        } catch (e: Exception) {
            log.warn("Failed to update backlog.md", e)
            false
        }
    }

    private fun removeFeatureBlock(text: String, blockText: String, doc: com.intellij.openapi.editor.Document) {
        val startIdx = text.indexOf(blockText)
        if (startIdx >= 0) {
            var removalStart = startIdx
            var removalEnd = startIdx + blockText.length

            // Look backwards for preceding separator
            var lookBack = removalStart - 1
            while (lookBack >= 0 && text[lookBack].isWhitespace()) {
                lookBack--
            }
            if (lookBack >= 2 && text.substring(lookBack - 2, lookBack + 1) == "---") {
                removalStart = lookBack - 2
                // Include preceding newline if present
                if (removalStart > 0 && text[removalStart - 1] == '\n') {
                    removalStart--
                }
            }

            // Look forwards for following separator
            var lookForward = removalEnd
            while (lookForward < text.length && text[lookForward].isWhitespace()) {
                lookForward++
            }
            if (lookForward + 3 <= text.length && text.substring(lookForward, lookForward + 3) == "---") {
                removalEnd = lookForward + 3
            } else {
                // If no separator, just consume whitespace
                removalEnd = lookForward
            }

            // Consume trailing newlines
            while (removalEnd < text.length && (text[removalEnd] == '\n' || text[removalEnd] == '\r')) {
                removalEnd++
            }

            val newText = text.removeRange(removalStart, removalEnd)
            doc.setText(newText)
        }
    }
}
