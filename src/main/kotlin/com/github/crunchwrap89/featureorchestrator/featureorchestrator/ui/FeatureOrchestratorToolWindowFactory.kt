package com.github.crunchwrap89.featureorchestrator.featureorchestrator.ui

import com.github.crunchwrap89.featureorchestrator.featureorchestrator.core.OrchestratorController
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.BacklogFeature
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.BacklogStatus
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.OrchestratorState
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.AcceptanceCriterion
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.IdeBorderFactory
import com.intellij.util.ui.JBUI
import com.intellij.ui.OnePixelSplitter
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
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
    private val featureName = JBLabel("", javax.swing.SwingConstants.CENTER)
    private val featureDesc = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        background = null
        border = null
        rows = 10
    }
    private val prevButton = JButton("<").apply { isEnabled = false }
    private val nextButton = JButton(">").apply { isEnabled = false }
    private val runButton = JButton("▶ Generate prompt")
    private val editBacklogButton = JButton("Edit Backlog").apply { isVisible = false }
    private val verifyButton = JButton("Verify implementation").apply { isEnabled = false }
    private val logArea = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        rows = 10
    }
    private val changesLabel = JBLabel("Changed files: 0")
    private val acceptanceCriteriaArea = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        rows = 10
    }

    private val controller = OrchestratorController(project, this)
    private var lastStatus: BacklogStatus = BacklogStatus.OK

    init {
        val featureCard = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = IdeBorderFactory.createRoundedBorder()

            val titlePanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                border = JBUI.Borders.empty(5)
                add(JBLabel("Feature").apply { font = JBUI.Fonts.label().asBold() }, BorderLayout.WEST)
            }
            add(titlePanel, BorderLayout.NORTH)

            val contentPanel = JBPanel<JBPanel<*>>(BorderLayout())
            val navPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                add(prevButton, BorderLayout.WEST)
                add(featureName, BorderLayout.CENTER)
                add(nextButton, BorderLayout.EAST)
            }
            contentPanel.add(navPanel, BorderLayout.NORTH)
            val scrollPane = JBScrollPane(featureDesc)
            contentPanel.add(scrollPane, BorderLayout.CENTER)

            add(contentPanel, BorderLayout.CENTER)
        }

        val buttons = object : JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.CENTER)) {
            override fun getPreferredSize(): Dimension {
                val d = super.getPreferredSize()
                val parent = parent
                if (parent != null) {
                    val width = parent.width
                    if (width > 0) {
                        // Calculate height required for this width
                        val layout = layout as FlowLayout
                        var rowHeight = 0
                        var totalHeight = layout.vgap
                        var rowWidth = layout.hgap

                        for (i in 0 until componentCount) {
                            val comp = getComponent(i)
                            if (comp.isVisible) {
                                val dComp = comp.preferredSize
                                if (rowWidth + dComp.width > width) {
                                    totalHeight += rowHeight + layout.vgap
                                    rowWidth = layout.hgap
                                    rowHeight = 0
                                }
                                rowWidth += dComp.width + layout.hgap
                                rowHeight = maxOf(rowHeight, dComp.height)
                            }
                        }
                        totalHeight += rowHeight + layout.vgap
                        d.height = totalHeight
                        d.width = width // Constrain width to parent
                    }
                }
                return d
            }
        }.apply {
            add(runButton)
            add(editBacklogButton)
            add(verifyButton)
        }

        val criteriaPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = IdeBorderFactory.createRoundedBorder()
            val titlePanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                border = JBUI.Borders.empty(5)
                add(JBLabel("Acceptance Criteria").apply { font = JBUI.Fonts.label().asBold() }, BorderLayout.WEST)
            }
            add(titlePanel, BorderLayout.NORTH)
            add(JBScrollPane(acceptanceCriteriaArea), BorderLayout.CENTER)
        }

        val logPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = IdeBorderFactory.createRoundedBorder()

            val logHeader = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                border = JBUI.Borders.empty(5)
                add(JBLabel("Execution Log").apply { font = JBUI.Fonts.label().asBold() }, BorderLayout.WEST)
                add(JBPanel<JBPanel<*>>().apply {
                    add(statusIndicator)
                    add(statusLabel)
                }, BorderLayout.EAST)
            }

            add(logHeader, BorderLayout.NORTH)
            add(JBScrollPane(logArea), BorderLayout.CENTER)
        }

        val topPanel = JBPanel<JBPanel<*>>(BorderLayout())
        topPanel.add(featureCard, BorderLayout.NORTH)
        topPanel.add(criteriaPanel, BorderLayout.CENTER)
        topPanel.add(buttons, BorderLayout.SOUTH)

        val splitter = OnePixelSplitter(true)
        splitter.proportion = 0.8f
        splitter.firstComponent = topPanel
        splitter.secondComponent = logPanel

        add(splitter, BorderLayout.CENTER)

        prevButton.addActionListener { controller.previousFeature() }
        nextButton.addActionListener { controller.nextFeature() }
        runButton.addActionListener { controller.runNextFeature() }
        editBacklogButton.addActionListener { controller.createOrUpdateBacklog() }
        verifyButton.addActionListener { controller.verifyNow() }
    }

    // Listener implementation
    override fun onStateChanged(state: OrchestratorState) {
        val statusText = when (state) {
            OrchestratorState.AWAITING_AI -> "Awaiting AI Agent"
            else -> state.name.lowercase().replaceFirstChar { it.titlecase() }
        }
        statusLabel.text = "Status: $statusText"
        when (state) {
            OrchestratorState.IDLE -> statusIndicator.foreground = JBColor.GRAY
            OrchestratorState.HANDOFF -> statusIndicator.foreground = JBColor(0xFFA500, 0xFFA500)
            OrchestratorState.AWAITING_AI -> statusIndicator.foreground = JBColor.BLUE
            OrchestratorState.VERIFYING -> statusIndicator.foreground = JBColor(0x6A0DAD, 0x6A0DAD)
            OrchestratorState.COMPLETED -> statusIndicator.foreground = JBColor.GREEN
            OrchestratorState.FAILED -> statusIndicator.foreground = JBColor.RED
        }
        verifyButton.isEnabled = (state == OrchestratorState.AWAITING_AI || state == OrchestratorState.HANDOFF || state == OrchestratorState.FAILED)
        runButton.isEnabled = (state == OrchestratorState.IDLE || state == OrchestratorState.FAILED || state == OrchestratorState.COMPLETED || state == OrchestratorState.AWAITING_AI)
    }

    override fun onLog(message: String) {
        logArea.append(message + "\n")
        logArea.caretPosition = logArea.document.length
    }

    override fun onClearLog() {
        logArea.text = ""
    }

    override fun onClearPrompt() {
        // No-op
    }

    override fun onBacklogStatusChanged(status: BacklogStatus) {
        lastStatus = status
        when (status) {
            BacklogStatus.MISSING -> {
                featureName.text = "Backlog Missing"
                featureDesc.text = "No backlog.md found in project root."
                acceptanceCriteriaArea.text = ""
                runButton.isVisible = false
                editBacklogButton.isVisible = true
                editBacklogButton.text = "Create Backlog"
                prevButton.isEnabled = false
                nextButton.isEnabled = false
            }
            BacklogStatus.NO_FEATURES -> {
                featureName.text = "No Features"
                featureDesc.text = "No unchecked features found in backlog.md."
                acceptanceCriteriaArea.text = ""
                runButton.isVisible = false
                editBacklogButton.isVisible = true
                editBacklogButton.text = "Add Feature"
                prevButton.isEnabled = false
                nextButton.isEnabled = false
            }
            BacklogStatus.OK -> {
                runButton.isVisible = true
                editBacklogButton.isVisible = false
            }
        }
    }

    override fun onNavigationStateChanged(hasPrevious: Boolean, hasNext: Boolean) {
        prevButton.isEnabled = hasPrevious
        nextButton.isEnabled = hasNext
    }

    override fun onFeaturePreview(feature: BacklogFeature?) {
        if (lastStatus != BacklogStatus.OK) return
        featureName.text = feature?.let { "${it.name}" } ?: "No feature selected"
        featureDesc.text = feature?.description?.let { truncate(it, 600) } ?: ""

        if (feature == null) {
            acceptanceCriteriaArea.text = ""
            return
        }

        val automatic = mutableListOf<String>()
        val manual = mutableListOf<String>()

        feature.acceptanceCriteria.forEach { c ->
            when (c) {
                is AcceptanceCriterion.FileExists ->
                    automatic.add("- File exists: ${c.relativePath}")
                is AcceptanceCriterion.CommandSucceeds ->
                    automatic.add("- Command succeeds: ${c.command}")
                is AcceptanceCriterion.NoTestsFail ->
                    automatic.add("- No tests fail")
                is AcceptanceCriterion.ManualVerification ->
                    manual.add("- ${c.description}")
            }
        }

        val sb = StringBuilder()
        if (automatic.isNotEmpty()) {
            sb.append("Automatic checks:\n")
            automatic.forEach { sb.append(it).append("\n") }
        }
        if (manual.isNotEmpty()) {
            if (sb.isNotEmpty()) sb.append("\n")
            sb.append("Manual checks:\n")
            manual.forEach { sb.append(it).append("\n") }
        }

        acceptanceCriteriaArea.text = sb.toString().trimEnd()
    }

    override fun onChangeCountChanged(count: Int) {
        changesLabel.text = "Changed files: $count"
    }

    override fun onPromptGenerated(prompt: String) {

    }

    override fun onCompletion(success: Boolean) {
        onLog(if (success) "Verification succeeded." else "Verification failed.")
    }

    private fun truncate(text: String, max: Int): String = if (text.length <= max) text else text.substring(0, max) + "…"
}
