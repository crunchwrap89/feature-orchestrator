package com.github.crunchwrap89.featureorchestrator.featureorchestrator.core

import com.github.crunchwrap89.featureorchestrator.featureorchestrator.backlog.BacklogService
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.BacklogFeature
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.ExecutionSession
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.OrchestratorState
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.settings.OrchestratorSettings
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.settings.PromptHandoffBehavior
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.messages.MessageBusConnection
import java.awt.datatransfer.StringSelection
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.wm.ToolWindowManager

import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.BacklogStatus
import com.intellij.openapi.fileEditor.FileEditorManager
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.Skill
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.skills.SkillService

class OrchestratorController(private val project: Project, private val listener: Listener) : Disposable {
    private val settings = project.service<OrchestratorSettings>()
    private val backlogService = project.service<BacklogService>()

    var state: OrchestratorState = OrchestratorState.IDLE
        private set

    private var session: ExecutionSession? = null
    private var vfsConnection: MessageBusConnection? = null

    private var availableFeatures: List<BacklogFeature> = emptyList()
    private var currentFeatureIndex: Int = 0
    private val selectedSkillPaths = mutableSetOf<String>()

    fun getAvailableSkills(): List<Skill> = project.service<SkillService>().loadSkills()

    fun addSkill() {
        val name = Messages.showInputDialog(project, "Enter skill name:", "Add New Skill", null)
        if (!name.isNullOrBlank()) {
            val skillFile = project.service<SkillService>().addManualSkill(name)
            if (skillFile != null) {
                val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(skillFile)
                if (virtualFile != null) {
                    FileEditorManager.getInstance(project).openFile(virtualFile, true)
                }
            } else {
                Messages.showErrorDialog(project, "Skill with name '$name' already exists.", "Error")
            }
        }
    }

    fun downloadSkills() {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Downloading Agent Skills", true) {
            override fun run(indicator: ProgressIndicator) {
                project.service<SkillService>().downloadSkills(indicator)
                ApplicationManager.getApplication().invokeLater {
                    listener.onSkillsUpdated()
                }
            }
        })
    }

    fun toggleSkill(skill: Skill) {
        if (selectedSkillPaths.contains(skill.path)) {
            selectedSkillPaths.remove(skill.path)
        } else {
            selectedSkillPaths.add(skill.path)
        }
    }

    fun isSkillSelected(skill: Skill): Boolean = selectedSkillPaths.contains(skill.path)

    fun openSkillFile(skill: Skill) {
        val file = LocalFileSystem.getInstance().findFileByPath(skill.path)
        if (file != null) {
            FileEditorManager.getInstance(project).openFile(file, true)
        }
    }

    interface Listener {
        fun onStateChanged(state: OrchestratorState)
        fun onLog(message: String)
        fun onClearLog()
        fun onFeatureSelected(feature: BacklogFeature?)
        fun onPromptGenerated(prompt: String)
        fun onClearPrompt()
        fun onCompletion(success: Boolean)
        fun onBacklogStatusChanged(status: BacklogStatus)
        fun onNavigationStateChanged(hasPrevious: Boolean, hasNext: Boolean)
        fun onSkillsUpdated()
    }

    init {
        // Migration: Remove the project-local skills directory if it exists
        project.basePath?.let { basePath ->
            val oldLocalSkillsDir = java.io.File(basePath, ".aiassistant/skills")
            if (oldLocalSkillsDir.exists()) {
                // Before deleting, try to migrate any manual skills to the global directory
                try {
                    val skillService = project.service<SkillService>()
                    val globalSkillsDir = java.io.File(skillService.getSkillsDir()?.path ?: "")
                    if (globalSkillsDir.exists()) {
                        oldLocalSkillsDir.listFiles()?.filter { it.isDirectory }?.forEach { skillDir ->
                            val skillMd = java.io.File(skillDir, "SKILL.md")
                            if (skillMd.exists() && skillMd.readText().contains("manual: true")) {
                                val targetDir = java.io.File(globalSkillsDir, skillDir.name)
                                if (!targetDir.exists()) {
                                    skillDir.copyRecursively(targetDir)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Logger.getInstance(OrchestratorController::class.java).warn("Failed to migrate manual skills from project to global dir", e)
                }

                oldLocalSkillsDir.deleteRecursively()
                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(java.io.File(basePath, ".aiassistant"))?.refresh(false, true)
            }
        }

        // Initial check
        ApplicationManager.getApplication().invokeLater {
            validateBacklog()
        }
        setupBacklogListener()
        setupSkillsListener()
    }

    private fun setupSkillsListener() {
        val connection = project.messageBus.connect(this)
        connection.subscribe(SkillService.SkillsUpdateListener.TOPIC, object : SkillService.SkillsUpdateListener {
            override fun onSkillsUpdated() {
                ApplicationManager.getApplication().invokeLater {
                    listener.onSkillsUpdated()
                }
            }
        })
    }

    private fun setupBacklogListener() {
        val connection = project.messageBus.connect(this)
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: MutableList<out VFileEvent>) {
                val relevant = events.any { event ->
                    val name = event.file?.name ?: event.path.substringAfterLast('/')
                    name.equals("backlog.md", ignoreCase = true)
                }
                if (relevant) {
                    ApplicationManager.getApplication().invokeLater {
                        validateBacklog()
                    }
                }
            }
        })
    }

    fun validateBacklog() {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Validating Backlog", false) {
            override fun run(indicator: ProgressIndicator) {
                val backlogFile = backlogService.backlogFile()
                ApplicationManager.getApplication().invokeLater {
                    if (backlogFile == null) {
                        setState(OrchestratorState.IDLE)
                        listener.onBacklogStatusChanged(BacklogStatus.MISSING)
                        listener.onNavigationStateChanged(false, false)
                    } else {
                        processBacklog()
                    }
                }
            }
        })
    }

    private fun processBacklog() {
        val backlog = backlogService.parseBacklog()
        if (backlog == null) {
            setState(OrchestratorState.IDLE)
            listener.onBacklogStatusChanged(BacklogStatus.NO_FEATURES)
            listener.onNavigationStateChanged(false, false)
            return
        }

        val oldSelection = if (availableFeatures.isNotEmpty() && currentFeatureIndex in availableFeatures.indices) {
            availableFeatures[currentFeatureIndex].name
        } else null

        availableFeatures = backlog.features.filter { !it.checked }

        if (availableFeatures.isEmpty()) {
            setState(OrchestratorState.IDLE)
            currentFeatureIndex = 0
            listener.onBacklogStatusChanged(BacklogStatus.NO_FEATURES)
            listener.onNavigationStateChanged(false, false)
            return
        }

        // Restore selection
        currentFeatureIndex = 0
        if (oldSelection != null) {
            val idx = availableFeatures.indexOfFirst { it.name == oldSelection }
            if (idx != -1) {
                currentFeatureIndex = idx
            }
        }

        listener.onBacklogStatusChanged(BacklogStatus.OK)
        updateNavigation()
    }

    private fun updateNavigation() {
        listener.onNavigationStateChanged(
            hasPrevious = currentFeatureIndex > 0,
            hasNext = currentFeatureIndex < availableFeatures.size - 1
        )
        updateFeatureSelection()
    }

    private fun updateFeatureSelection() {
        if (availableFeatures.isNotEmpty() && currentFeatureIndex in availableFeatures.indices) {
            listener.onFeatureSelected(availableFeatures[currentFeatureIndex])
        } else {
            listener.onFeatureSelected(null)
        }
    }

    fun nextFeature() {
        if (currentFeatureIndex < availableFeatures.size - 1) {
            currentFeatureIndex++
            updateNavigation()
        }
    }

    fun previousFeature() {
        if (currentFeatureIndex > 0) {
            currentFeatureIndex--
            updateNavigation()
        }
    }

    fun addEmptyFeature() {
        backlogService.appendEmptyFeatureToBacklog()
        navigateToNewFeature()
        validateBacklog()
    }

    fun addTemplateFeature() {
        backlogService.appendTemplateToBacklog()
        navigateToNewFeature()
        validateBacklog()
    }

    private fun navigateToNewFeature() {
        val file = backlogService.backlogFile()
        if (file != null) {
            val editors = FileEditorManager.getInstance(project).openFile(file, true)
            val textEditor = editors.firstOrNull { it is com.intellij.openapi.fileEditor.TextEditor } as? com.intellij.openapi.fileEditor.TextEditor
            textEditor?.editor?.let { editor ->
                val text = editor.document.text
                val lastHeaderIndex = text.lastIndexOf("## Feature name")
                if (lastHeaderIndex != -1) {
                    val lineNumber = editor.document.getLineNumber(lastHeaderIndex)
                    // We want to go to the line below ## Feature name.
                    // The feature name line is lineNumber.
                    // If content follows immediately, it might be lineNumber + 1
                    var targetLine = lineNumber + 1
                    if (targetLine < editor.document.lineCount) {
                        editor.caretModel.moveToLogicalPosition(com.intellij.openapi.editor.LogicalPosition(targetLine, 0))
                        editor.scrollingModel.scrollToCaret(com.intellij.openapi.editor.ScrollType.CENTER)
                    }
                } else {
                    // Fallback to end of file
                    editor.scrollingModel.scrollTo(com.intellij.openapi.editor.LogicalPosition(editor.document.lineCount - 1, 0), com.intellij.openapi.editor.ScrollType.CENTER)
                }
            }
        }
    }

    fun editFeature() {
        if (availableFeatures.isNotEmpty() && currentFeatureIndex in availableFeatures.indices) {
            val feature = availableFeatures[currentFeatureIndex]
            val file = backlogService.backlogFile() ?: return
            val editors = FileEditorManager.getInstance(project).openFile(file, true)
            val textEditor = editors.firstOrNull { it is com.intellij.openapi.fileEditor.TextEditor } as? com.intellij.openapi.fileEditor.TextEditor
            textEditor?.editor?.let { editor ->
                editor.caretModel.moveToOffset(feature.blockStartOffset)
                editor.scrollingModel.scrollToCaret(com.intellij.openapi.editor.ScrollType.CENTER)
            }
        }
    }

    fun removeFeature() {
        if (availableFeatures.isNotEmpty() && currentFeatureIndex in availableFeatures.indices) {
            val feature = availableFeatures[currentFeatureIndex]
            val confirm = Messages.showYesNoDialog(project, "Are you sure you want to remove feature '${feature.name}'?", "Remove Feature", Messages.getQuestionIcon())
            if (confirm == Messages.YES) {
                backlogService.removeFeature(feature)
                validateBacklog()
            }
        }
    }

    fun completeFeature() {
        if (availableFeatures.isNotEmpty() && currentFeatureIndex in availableFeatures.indices) {
            val feature = availableFeatures[currentFeatureIndex]
            val confirm = Messages.showYesNoDialog(project, "Are you sure you want to mark feature '${feature.name}' as completed?", "Complete Feature", Messages.getQuestionIcon())
            if (confirm == Messages.YES) {
                val behavior = settings.completionBehavior
                val ok = backlogService.markCompletedOrRemove(feature, behavior)
                if (ok) {
                    info("Feature '${feature.name}' marked as completed.")
                    stopMonitoring()
                    setState(OrchestratorState.COMPLETED)
                    listener.onCompletion(true)
                    listener.onClearPrompt()
                    validateBacklog()
                } else {
                    warn("Failed to update backlog.md")
                }
            }
        }
    }


    fun createOrUpdateBacklog() {
        val backlogFile = backlogService.backlogFile()
        if (backlogFile == null) {
            backlogService.createTemplateBacklog()
        } else {
            backlogService.appendTemplateToBacklog()
        }

        val file = backlogService.backlogFile()
        if (file != null) {
            FileEditorManager.getInstance(project).openFile(file, true)
        }
        validateBacklog()
    }

    fun runNextFeature() {
        require(state == OrchestratorState.IDLE || state == OrchestratorState.FAILED || state == OrchestratorState.COMPLETED || state == OrchestratorState.AWAITING_AI) { "Invalid state" }

        // Re-validate to ensure list is up to date, but try to keep selection if possible
        val oldFeatureName = if (availableFeatures.isNotEmpty()) availableFeatures[currentFeatureIndex].name else null
        validateBacklog()

        if (availableFeatures.isEmpty()) {
            // validateBacklog already handled UI updates
            return
        }

        // Try to restore selection or default to 0
        if (oldFeatureName != null) {
            val newIndex = availableFeatures.indexOfFirst { it.name == oldFeatureName }
            if (newIndex != -1) {
                currentFeatureIndex = newIndex
                updateNavigation()
            }
        }

        val feature = availableFeatures[currentFeatureIndex]

        val selectedSkills = getAvailableSkills().filter { selectedSkillPaths.contains(it.path) }
        val prompt = PromptGenerator.generate(feature, selectedSkills)
        listener.onPromptGenerated(prompt)
        handoffPrompt(prompt)

        // If we are already awaiting AI for the same feature, just re-handoff and return
        if (state == OrchestratorState.AWAITING_AI && session?.feature?.name == feature.name) {
            return
        }

        setState(OrchestratorState.HANDOFF)
        startMonitoring(feature)
        setState(OrchestratorState.AWAITING_AI)
    }


    private fun handoffPrompt(prompt: String) {
        // Always copy to clipboard as a fallback/convenience
        CopyPasteManager.getInstance().setContents(StringSelection(prompt))

        when (settings.promptHandoffBehavior) {
            PromptHandoffBehavior.COPY_TO_CLIPBOARD -> {
                info("Prompt copied to clipboard.")
            }
            PromptHandoffBehavior.AUTO_AI_ASSISTANT -> {
                info("Prompt copied to clipboard. Opening AI Assistant...")
                openToolWindow("AIAssistant")
            }
        }
    }

    private fun openToolWindow(id: String): Boolean {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow(id)
        if (toolWindow != null) {
            toolWindow.activate(null)
            return true
        } else {
            warn("Tool window '$id' not found.")
            return false
        }
    }

    fun reset() {
        stopMonitoring()
        session = null
        setState(OrchestratorState.IDLE)
        listener.onFeatureSelected(null)
    }

    private fun completeSuccess() {
        val s = session ?: return
        val behavior = settings.completionBehavior
        val ok = backlogService.markCompletedOrRemove(s.feature, behavior)
        if (!ok) {
            setFailed("Failed to update backlog.md")
            return
        }
        stopMonitoring()
        setState(OrchestratorState.COMPLETED)
        info("Feature '${s.feature.name}' marked as completed.")
        listener.onCompletion(true)
        listener.onFeatureSelected(null)
        listener.onClearPrompt()
    }

    private fun setFailed(reason: String) {
        stopMonitoring()
        setState(OrchestratorState.FAILED)
        warn(reason)
        listener.onCompletion(false)
    }

    private fun startMonitoring(feature: BacklogFeature) {
        stopMonitoring()
        val newSession = ExecutionSession(feature)
        session = newSession
        vfsConnection = project.messageBus.connect().also { conn ->
            conn.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
                override fun after(events: MutableList<out VFileEvent>) {
                    events.forEach { e ->
                        val vf = e.file ?: return@forEach
                        if (isProjectContent(vf)) {
                            newSession.changedFiles += vf
                        }
                    }
                }
            })
        }
    }

    private fun stopMonitoring() {
        vfsConnection?.disconnect()
        vfsConnection = null
    }

    private fun isProjectContent(file: VirtualFile): Boolean {
        val base = project.basePath ?: return false
        return file.path.startsWith(base)
    }

    private fun setState(new: OrchestratorState) {
        state = new
        listener.onStateChanged(state)
    }

    private fun info(msg: String) = listener.onLog(msg)
    private fun log(msg: String) = listener.onLog(msg)
    private fun warn(msg: String) = listener.onLog("WARN: $msg")

    override fun dispose() {
        stopMonitoring()
    }
}


