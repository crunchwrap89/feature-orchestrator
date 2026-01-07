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
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.AcceptanceCriterion
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class OrchestratorController(private val project: Project, private val listener: Listener) : Disposable {
    private val settings = project.service<OrchestratorSettings>()
    private val backlogService = project.service<BacklogService>()

    var state: OrchestratorState = OrchestratorState.IDLE
        private set

    private var session: ExecutionSession? = null
    private var vfsConnection: MessageBusConnection? = null

    private var availableFeatures: List<BacklogFeature> = emptyList()
    private var currentFeatureIndex: Int = 0

    interface Listener {
        fun onStateChanged(state: OrchestratorState)
        fun onLog(message: String)
        fun onClearLog()
        fun onFeaturePreview(feature: BacklogFeature?)
        fun onChangeCountChanged(count: Int)
        fun onPromptGenerated(prompt: String)
        fun onClearPrompt()
        fun onCompletion(success: Boolean)
        fun onBacklogStatusChanged(status: BacklogStatus)
        fun onNavigationStateChanged(hasPrevious: Boolean, hasNext: Boolean)
    }

    init {
        // Initial check
        ApplicationManager.getApplication().invokeLater {
            validateBacklog()
        }
        setupBacklogListener()
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
        val backlogFile = backlogService.backlogFile()
        if (backlogFile == null) {
            setState(OrchestratorState.IDLE)
            listener.onBacklogStatusChanged(BacklogStatus.MISSING)
            listener.onFeaturePreview(null)
            listener.onNavigationStateChanged(false, false)
            return
        }
        val backlog = backlogService.parseBacklog()
        if (backlog == null) {
            setState(OrchestratorState.IDLE)
            listener.onBacklogStatusChanged(BacklogStatus.NO_FEATURES)
            listener.onFeaturePreview(null)
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
            listener.onFeaturePreview(null)
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
        updateFeaturePreview()
    }

    fun nextFeature() {
        if (currentFeatureIndex < availableFeatures.size - 1) {
            currentFeatureIndex++
            updateFeaturePreview()
        }
    }

    fun previousFeature() {
        if (currentFeatureIndex > 0) {
            currentFeatureIndex--
            updateFeaturePreview()
        }
    }

    fun addFeature() {
        backlogService.appendTemplateToBacklog()
        val file = backlogService.backlogFile()
        if (file != null) {
            val editors = FileEditorManager.getInstance(project).openFile(file, true)
            val textEditor = editors.firstOrNull { it is com.intellij.openapi.fileEditor.TextEditor } as? com.intellij.openapi.fileEditor.TextEditor
            textEditor?.editor?.let { editor ->
                editor.scrollingModel.scrollTo(com.intellij.openapi.editor.LogicalPosition(editor.document.lineCount - 1, 0), com.intellij.openapi.editor.ScrollType.CENTER)
            }
        }
        validateBacklog()
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
                    validateBacklog()
                } else {
                    warn("Failed to update backlog.md")
                }
            }
        }
    }

    private fun updateFeaturePreview() {
        if (availableFeatures.isNotEmpty() && currentFeatureIndex in availableFeatures.indices) {
            listener.onFeaturePreview(availableFeatures[currentFeatureIndex])
            listener.onNavigationStateChanged(
                hasPrevious = currentFeatureIndex > 0,
                hasNext = currentFeatureIndex < availableFeatures.size - 1
            )
        } else {
            listener.onFeaturePreview(null)
            listener.onNavigationStateChanged(false, false)
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
                updateFeaturePreview()
            }
        }

        val feature = availableFeatures[currentFeatureIndex]

        val prompt = PromptGenerator.generate(feature)
        listener.onPromptGenerated(prompt)
        handoffPrompt(prompt)

        if (settings.showNotificationAfterHandoff) {
            Messages.showInfoMessage(project, "Prompt prepared. Paste it into your AI tool (Copilot Chat or JetBrains AI Assistant).", "Feature Orchestrator")
        }

        // If we are already awaiting AI for the same feature, just re-handoff and return
        if (state == OrchestratorState.AWAITING_AI && session?.feature?.name == feature.name) {
            return
        }

        setState(OrchestratorState.HANDOFF)
        startMonitoring(feature)
        setState(OrchestratorState.AWAITING_AI)
    }

    fun verifyNow() {
        // Ensure we have the latest backlog state
        validateBacklog()

        var s = session ?: return

        // Refresh feature from availableFeatures to get latest acceptance criteria
        val currentFeature = availableFeatures.find { it.name == s.feature.name }
        if (currentFeature != null && currentFeature != s.feature) {
            s = s.copy(feature = currentFeature)
            session = s
        }

        setState(OrchestratorState.VERIFYING)
        if (s.feature.acceptanceCriteria.isEmpty()) {
            val ok = Messages.showYesNoDialog(project, "No Acceptance Criteria found. Mark feature as completed?", "Feature Orchestrator", null) == Messages.YES
            if (ok) completeSuccess() else setFailed("User cancelled completion without criteria.")
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Verifying feature", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    ApplicationManager.getApplication().invokeLater { info("Verifying implementation...") }
                    val result = AcceptanceVerifier.verify(project, s.feature.acceptanceCriteria)
                    ApplicationManager.getApplication().invokeLater {
                        result.details.forEach { log(it) }
                        if (result.success) {
                            if (result.manualVerifications.isNotEmpty()) {
                                val dialog = ManualVerificationDialog(project, result.manualVerifications)
                                if (dialog.showAndGet()) {
                                    val unfulfilled = dialog.getUnfulfilledCriteria()
                                    if (unfulfilled.isEmpty()) {
                                        info("Manual verification confirmed.")
                                        info("Verification successfully completed.")
                                        completeSuccess()
                                    } else {
                                        info("Manual verification incomplete.")
                                        val manualFailures = unfulfilled.map {
                                            FailureDetail(it, "User indicated this manual verification was not fulfilled.")
                                        }
                                        handleVerificationFailure(s.feature, result.failures + manualFailures)
                                    }
                                } else {
                                    info("Manual verification cancelled.")
                                    setState(OrchestratorState.AWAITING_AI)
                                }
                            } else {
                                info("Verification successfully completed.")
                                completeSuccess()
                            }
                        } else {
                            handleVerificationFailure(s.feature, result.failures)
                        }
                    }
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        log("ERROR: Verification failed with exception: ${e.message}")
                        setFailed("Verification exception: ${e.message}")
                    }
                }
            }
        })
    }

    private fun handleVerificationFailure(feature: BacklogFeature, failures: List<FailureDetail>) {
        info("Verification failed.")
        val prompt = PromptGenerator.generateFailurePrompt(feature, failures)
        listener.onPromptGenerated(prompt)
        handoffPrompt(prompt)

        if (settings.showNotificationAfterHandoff) {
            Messages.showInfoMessage(project, "Verification failed. Failure prompt prepared. Paste it into your AI tool to fix the issues.", "Feature Orchestrator")
        }
        setState(OrchestratorState.AWAITING_AI)
    }

    private fun handoffPrompt(prompt: String) {
        // Always copy to clipboard as a fallback/convenience
        CopyPasteManager.getInstance().setContents(StringSelection(prompt))

        when (settings.promptHandoffBehavior) {
            PromptHandoffBehavior.COPY_TO_CLIPBOARD -> {
                info("Prompt copied to clipboard.")
            }
            PromptHandoffBehavior.AUTO_COPILOT -> {
                info("Prompt copied to clipboard. Opening Copilot...")
                // Try common IDs for Copilot Chat
                openToolWindow("Github Copilot Chat") || openToolWindow("Copilot Chat")
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
        listener.onFeaturePreview(null)
        listener.onChangeCountChanged(0)
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
        listener.onFeaturePreview(null)
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
                    listener.onChangeCountChanged(newSession.changedFiles.size)
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

private class ManualVerificationDialog(project: Project, val criteria: List<AcceptanceCriterion.ManualVerification>) : DialogWrapper(project) {
    private val checkboxes = criteria.map { JBCheckBox(it.description) }

    init {
        title = "Manual Verification"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.add(JLabel("Please confirm the following criteria are fulfilled:"))
        checkboxes.forEach { panel.add(it) }
        return panel
    }

    fun getUnfulfilledCriteria(): List<AcceptanceCriterion.ManualVerification> {
        return criteria.zip(checkboxes).filter { !it.second.isSelected }.map { it.first }
    }
}
