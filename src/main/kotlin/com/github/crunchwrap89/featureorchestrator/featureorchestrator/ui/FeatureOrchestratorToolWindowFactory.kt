package com.github.crunchwrap89.featureorchestrator.featureorchestrator.ui

import com.github.crunchwrap89.featureorchestrator.featureorchestrator.core.OrchestratorController
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.BacklogFeature
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.BacklogStatus
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.OrchestratorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JTextArea

class FeatureOrchestratorToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = FeatureOrchestratorPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, null, false)
        toolWindow.contentManager.addContent(content)
    }
}

private class FeatureOrchestratorPanel(private val project: Project) : JBPanel<FeatureOrchestratorPanel>(BorderLayout()), OrchestratorController.Listener {
    private val statusLabel = JBLabel("Status: Idle")
    private val statusIndicator = JBLabel("●").apply { foreground = JBColor.GRAY }
    private val featureName = JBLabel("")
    private val featureDesc = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        background = null
        border = null
        rows = 5
    }
    private val runButton = JButton("▶ Implement Feature")
    private val editBacklogButton = JButton("Edit Backlog").apply { isVisible = false }
    private val verifyButton = JButton("Verify implementation").apply { isEnabled = false }
    private val logArea = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        preferredSize = Dimension(100, 200)
    }
    private val changesLabel = JBLabel("Changed files: 0")
    private val promptPreview = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        rows = 8
    }

    private val controller = OrchestratorController(project, this)
    private var lastStatus: BacklogStatus = BacklogStatus.OK

    init {
        val header = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(JBPanel<JBPanel<*>>().apply {
                add(statusIndicator)
                add(statusLabel)
            }, BorderLayout.WEST)
        }

        val featureCard = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = javax.swing.BorderFactory.createTitledBorder("Feature Preview")
            add(JBScrollPane(featureDesc), BorderLayout.CENTER)
        }

        val buttons = JBPanel<JBPanel<*>>().apply {
            add(runButton)
            add(editBacklogButton)
            add(verifyButton)
        }

        val logPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = javax.swing.BorderFactory.createTitledBorder("Execution Log")
            add(JBScrollPane(logArea), BorderLayout.CENTER)
        }

        val promptPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = javax.swing.BorderFactory.createTitledBorder("AI Prompt")
            add(JBScrollPane(promptPreview), BorderLayout.CENTER)
        }

        add(header, BorderLayout.NORTH)
        add(JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(featureCard, BorderLayout.NORTH)
            add(buttons, BorderLayout.CENTER)
        }, BorderLayout.CENTER)
        add(logPanel, BorderLayout.SOUTH)

        runButton.addActionListener { controller.runNextFeature() }
        editBacklogButton.addActionListener { controller.createOrUpdateBacklog() }
        verifyButton.addActionListener { controller.verifyNow() }
    }

    // Listener implementation
    override fun onStateChanged(state: OrchestratorState) {
        statusLabel.text = "Status: ${state.name.lowercase().replaceFirstChar { it.titlecase() }}"
        when (state) {
            OrchestratorState.IDLE -> statusIndicator.foreground = JBColor.GRAY
            OrchestratorState.HANDOFF -> statusIndicator.foreground = JBColor(0xFFA500, 0xFFA500)
            OrchestratorState.RUNNING -> statusIndicator.foreground = JBColor.BLUE
            OrchestratorState.VERIFYING -> statusIndicator.foreground = JBColor(0x6A0DAD, 0x6A0DAD)
            OrchestratorState.COMPLETED -> statusIndicator.foreground = JBColor.GREEN
            OrchestratorState.FAILED -> statusIndicator.foreground = JBColor.RED
        }
        verifyButton.isEnabled = (state == OrchestratorState.RUNNING || state == OrchestratorState.HANDOFF || state == OrchestratorState.FAILED)
        runButton.isEnabled = (state == OrchestratorState.IDLE || state == OrchestratorState.FAILED || state == OrchestratorState.COMPLETED)
    }

    override fun onLog(message: String) {
        logArea.append(message + "\n")
        logArea.caretPosition = logArea.document.length
    }

    override fun onClearLog() {
        logArea.text = ""
    }

    override fun onClearPrompt() {
        promptPreview.text = ""
    }

    override fun onBacklogStatusChanged(status: BacklogStatus) {
        lastStatus = status
        when (status) {
            BacklogStatus.MISSING -> {
                featureName.text = "Backlog Missing"
                featureDesc.text = "No backlog.md found in project root."
                runButton.isVisible = false
                editBacklogButton.isVisible = true
                editBacklogButton.text = "Create Backlog"
            }
            BacklogStatus.NO_FEATURES -> {
                featureName.text = "No Features"
                featureDesc.text = "No unchecked features found in backlog.md."
                runButton.isVisible = false
                editBacklogButton.isVisible = true
                editBacklogButton.text = "Add Feature"
            }
            BacklogStatus.OK -> {
                runButton.isVisible = true
                editBacklogButton.isVisible = false
            }
        }
    }

    override fun onFeaturePreview(feature: BacklogFeature?) {
        if (lastStatus != BacklogStatus.OK) return
        featureName.text = feature?.let { "${it.name}" } ?: "No feature selected"
        featureDesc.text = feature?.description?.let { truncate(it, 600) } ?: ""
    }

    override fun onChangeCountChanged(count: Int) {
        changesLabel.text = "Changed files: $count"
    }

    override fun onPromptGenerated(prompt: String) {
        onLog("Prompt prepared and handed off.")
        promptPreview.text = prompt
    }

    override fun onCompletion(success: Boolean) {
        onLog(if (success) "Verification succeeded." else "Verification failed.")
    }

    private fun truncate(text: String, max: Int): String = if (text.length <= max) text else text.substring(0, max) + "…"
}
