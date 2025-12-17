package com.github.crunchwrap89.awebstormplugin.featureorchestrator.core

import com.github.crunchwrap89.awebstormplugin.featureorchestrator.backlog.BacklogService
import com.github.crunchwrap89.awebstormplugin.featureorchestrator.model.BacklogFeature
import com.github.crunchwrap89.awebstormplugin.featureorchestrator.model.ExecutionSession
import com.github.crunchwrap89.awebstormplugin.featureorchestrator.model.OrchestratorState
import com.github.crunchwrap89.awebstormplugin.featureorchestrator.settings.OrchestratorSettings
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
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

class OrchestratorController(private val project: Project, private val listener: Listener) : Disposable {
    private val log = Logger.getInstance(OrchestratorController::class.java)
    private val settings = project.service<OrchestratorSettings>()
    private val backlogService = project.service<BacklogService>()

    var state: OrchestratorState = OrchestratorState.IDLE
        private set

    private var session: ExecutionSession? = null
    private var vfsConnection: MessageBusConnection? = null

    interface Listener {
        fun onStateChanged(state: OrchestratorState)
        fun onLog(message: String)
        fun onClearLog()
        fun onFeaturePreview(feature: BacklogFeature?)
        fun onChangeCountChanged(count: Int)
        fun onPromptGenerated(prompt: String)
        fun onCompletion(success: Boolean)
    }

    fun runNextFeature() {
        require(state == OrchestratorState.IDLE || state == OrchestratorState.FAILED || state == OrchestratorState.COMPLETED) { "Invalid state" }
        val backlog = backlogService.parseBacklog()
        if (backlog == null) {
            warn("backlog.md not found in project root.")
            setState(OrchestratorState.FAILED)
            return
        }
        backlog.warnings.forEach { warn(it) }
        val feature = backlogService.firstUncheckedFeature(backlog)
        if (feature == null) {
            info("No unchecked features found. You're all caught up!")
            setState(OrchestratorState.IDLE)
            listener.onFeaturePreview(null)
            return
        }
        listener.onFeaturePreview(feature)
        val prompt = PromptGenerator.generate(feature)
        listener.onPromptGenerated(prompt)
        if (settings.copyPromptToClipboard) {
            CopyPasteManager.getInstance().setContents(StringSelection(prompt))
            info("Prompt copied to clipboard.")
        }
        if (settings.showNotificationAfterHandoff) {
            Messages.showInfoMessage(project, "Prompt prepared. Paste it into your AI tool (Copilot Chat or JetBrains AI Assistant).", "Feature Orchestrator")
        }
        setState(OrchestratorState.HANDOFF)
        startMonitoring(feature)
        setState(OrchestratorState.RUNNING)
        info("Monitoring file changes. Run your AI now.")
    }

    fun verifyNow() {
        val s = session ?: return
        setState(OrchestratorState.VERIFYING)
        if (s.feature.acceptanceCriteria.isEmpty()) {
            val ok = Messages.showYesNoDialog(project, "No Acceptance Criteria found. Mark feature as completed?", "Feature Orchestrator", null) == Messages.YES
            if (ok) completeSuccess() else setFailed("User cancelled completion without criteria.")
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Verifying Feature", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val result = AcceptanceVerifier.verify(project, s.feature.acceptanceCriteria)
                    ApplicationManager.getApplication().invokeLater {
                        result.details.forEach { log(it) }
                        if (result.success) completeSuccess() else setFailed("One or more criteria failed.")
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
        listener.onClearLog()
        info("Feature '${s.feature.name}' marked completed (${behavior.name}).")
        listener.onCompletion(true)
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
                    val idx = project.basePath
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
