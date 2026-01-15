package com.github.crunchwrap89.featureorchestrator.featureorchestrator.ui

import com.github.crunchwrap89.featureorchestrator.featureorchestrator.core.OrchestratorController
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.BacklogFeature
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.BacklogStatus
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.OrchestratorState
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.Skill
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.components.*
import com.intellij.ui.*
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import java.awt.*
import javax.swing.*
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep

class FeatureOrchestratorToolWindowFactory : ToolWindowFactory {
    override suspend fun isApplicableAsync(project: Project): Boolean {
        return true
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = FeatureOrchestratorPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, null, false)
        content.setDisposer(panel)
        toolWindow.contentManager.addContent(content)
    }
}

private class FeatureOrchestratorPanel(private val project: Project) : JBPanel<FeatureOrchestratorPanel>(BorderLayout()), OrchestratorController.Listener, Disposable {
    private val controller = OrchestratorController(project, this)

    override fun dispose() {
        com.intellij.openapi.util.Disposer.dispose(controller)
    }
    private var lastStatus: BacklogStatus = BacklogStatus.OK

    private val featureName = JBLabel("").apply {
        font = JBUI.Fonts.label().asBold().deriveFont(14f)
    }
    private val emptyBacklogLabel = JBLabel("Backlog is empty", SwingConstants.CENTER)
    private val featureDesc = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        background = UIUtil.getPanelBackground()
        border = JBUI.Borders.empty(10)
        font = JBUI.Fonts.label()
    }

    private val logArea = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        font = JBUI.Fonts.label()
    }

    private val skillsPanel = JBPanel<JBPanel<*>>(BorderLayout())

    private val runButton = JButton("▶ Generate Prompt").apply {
        font = JBUI.Fonts.label().asBold()
    }

    private val cardLayout = CardLayout()
    private val centerNavPanel = JBPanel<JBPanel<*>>(cardLayout).apply {
        add(featureName, "LABEL")
        add(JButton("Create BACKLOG.md").apply {
            addActionListener { controller.createOrUpdateBacklog() }
        }, "BUTTON")
        add(emptyBacklogLabel, "EMPTY")
    }

    init {
        val toolbarGroup = DefaultActionGroup().apply {
            add(object : AnAction("Previous Feature", "Go to previous feature", AllIcons.Actions.Back) {
                override fun actionPerformed(e: AnActionEvent) = controller.previousFeature()
                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = prevEnabled
                }
            })
            add(object : AnAction("Next Feature", "Go to next feature", AllIcons.Actions.Forward) {
                override fun actionPerformed(e: AnActionEvent) = controller.nextFeature()
                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = nextEnabled
                }
            })
            addSeparator()
            add(object : AnAction("Add Feature", "Add a new feature to backlog", AllIcons.General.Add) {
                override fun actionPerformed(e: AnActionEvent) {
                    val popup = JBPopupFactory.getInstance().createListPopup(
                        object : BaseListPopupStep<String>("Add Feature", listOf("Empty Feature", "Template Feature")) {
                            override fun onChosen(selectedValue: String?, finalChoice: Boolean): PopupStep<*>? {
                                if (finalChoice) {
                                    when (selectedValue) {
                                        "Empty Feature" -> controller.addEmptyFeature()
                                        "Template Feature" -> controller.addTemplateFeature()
                                    }
                                }
                                return PopupStep.FINAL_CHOICE
                            }
                        }
                    )
                    val component = e.inputEvent?.component
                    if (component != null) {
                        popup.showUnderneathOf(component)
                    } else {
                        popup.showInFocusCenter()
                    }
                }
            })
            add(object : AnAction("Edit Feature", "Edit selected feature", AllIcons.Actions.Edit) {
                override fun actionPerformed(e: AnActionEvent) = controller.editFeature()
                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = featureSelected
                }
            })
            add(object : AnAction("Remove Feature", "Remove selected feature", AllIcons.Actions.GC) {
                override fun actionPerformed(e: AnActionEvent) = controller.removeFeature()
                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = featureSelected
                }
            })
            add(object : AnAction("Mark Completed", "Mark feature as completed", AllIcons.Actions.Checked) {
                override fun actionPerformed(e: AnActionEvent) = controller.completeFeature()
                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = featureSelected
                }
            })
        }

        val actionToolbar = ActionManager.getInstance().createActionToolbar("FeatureOrchestratorToolbar", toolbarGroup, true)
        actionToolbar.targetComponent = this

        val headerPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0),
                JBUI.Borders.empty(5)
            )
            add(centerNavPanel, BorderLayout.WEST)
            add(actionToolbar.component, BorderLayout.EAST)
        }

        val mainContent = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            val scrollPane = JBScrollPane(featureDesc).apply {
                border = JBUI.Borders.empty()
            }
            add(scrollPane, BorderLayout.CENTER)
        }

        val skillsHeader = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.border(), 1, 0, 1, 0),
                JBUI.Borders.empty(2, 5)
            )
            background = UIUtil.getPanelBackground()
            add(JBLabel("Agent Skills").apply { font = JBUI.Fonts.smallFont() }, BorderLayout.WEST)
            
            val actionsPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.RIGHT, 0, 0)).apply {
                isOpaque = false
                add(ActionButton(object : AnAction("Add Skill", "Add a new manual skill", AllIcons.General.Add) {
                    override fun actionPerformed(e: AnActionEvent) = controller.addSkill()
                }, null, "FeatureOrchestratorAddSkill", ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE))
                add(ActionButton(object : AnAction("Refresh Skills", "Refresh available skills", AllIcons.Actions.Refresh) {
                    override fun actionPerformed(e: AnActionEvent) = controller.downloadSkills()
                }, null, "FeatureOrchestratorSkills", ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE))
            }
            add(actionsPanel, BorderLayout.EAST)
        }

        val skillsContainer = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(skillsHeader, BorderLayout.NORTH)
            add(skillsPanel, BorderLayout.CENTER)
            
            val runButtonPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                border = JBUI.Borders.empty(5, 10, 10, 10)
                add(runButton, BorderLayout.CENTER)
            }
            add(runButtonPanel, BorderLayout.SOUTH)
        }

        val leftSplitter = OnePixelSplitter(true, 0.5f).apply {
            firstComponent = mainContent
            secondComponent = skillsContainer
        }

        val logPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            val logHeader = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                border = JBUI.Borders.compound(
                    JBUI.Borders.customLine(JBColor.border(), 1, 0, 1, 0),
                    JBUI.Borders.empty(2, 5)
                )
                background = UIUtil.getPanelBackground()
                add(JBLabel("Execution Log").apply { font = JBUI.Fonts.smallFont() }, BorderLayout.WEST)
                add(ActionButton(object : AnAction("Clear Log", "Clear execution log", AllIcons.Actions.GC) {
                    override fun actionPerformed(e: AnActionEvent) { logArea.text = "" }
                }, null, "FeatureOrchestratorLog", ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE), BorderLayout.EAST)
            }
            add(logHeader, BorderLayout.NORTH)
            add(JBScrollPane(logArea).apply { border = JBUI.Borders.empty() }, BorderLayout.CENTER)
        }

        val mainSplitter = OnePixelSplitter(true, 0.7f).apply {
            firstComponent = leftSplitter
            secondComponent = logPanel
        }

        add(headerPanel, BorderLayout.NORTH)
        add(mainSplitter, BorderLayout.CENTER)

        runButton.addActionListener { controller.runNextFeature() }

        ApplicationManager.getApplication().executeOnPooledThread {
            val skills = controller.getAvailableSkills()
            ApplicationManager.getApplication().invokeLater {
                refreshSkills(skills)
            }
        }
    }

    private var prevEnabled = false
    private var nextEnabled = false
    private var featureSelected = false

    // Listener implementation
    override fun onStateChanged(state: OrchestratorState) {
        val rb = runButton ?: return
        val canRun = (state == OrchestratorState.IDLE || state == OrchestratorState.FAILED || state == OrchestratorState.COMPLETED || state == OrchestratorState.AWAITING_AI)
        rb.isEnabled = canRun && lastStatus == BacklogStatus.OK
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
        ApplicationManager.getApplication().executeOnPooledThread {
            val skills = try { controller.getAvailableSkills() } catch (e: Exception) { emptyList() }
            ApplicationManager.getApplication().invokeLater {
                if (!com.intellij.openapi.util.Disposer.isDisposed(this)) {
                    refreshSkills(skills)
                }
            }
        }
        val rb = runButton ?: return
        when (status) {
            BacklogStatus.MISSING -> {
                featureDesc.text = "No BACKLOG.md found in project root. Press Create BACKLOG.md to generate a template."
                rb.isVisible = true
                rb.isEnabled = false
                
                prevEnabled = false
                nextEnabled = false
                featureSelected = false

                cardLayout.show(centerNavPanel, "BUTTON")
            }
            BacklogStatus.NO_FEATURES -> {
                featureName.text = "No Features"
                featureDesc.text = "No features found in BACKLOG.md. Use the toolbar to add a new feature."
                rb.isVisible = true
                rb.isEnabled = false
                
                prevEnabled = false
                nextEnabled = false
                featureSelected = false

                cardLayout.show(centerNavPanel, "EMPTY")
            }
            BacklogStatus.OK -> {
                rb.isVisible = true
                val state = controller.state
                val canRun = (state == OrchestratorState.IDLE || state == OrchestratorState.FAILED || state == OrchestratorState.COMPLETED || state == OrchestratorState.AWAITING_AI)
                rb.isEnabled = canRun

                cardLayout.show(centerNavPanel, "LABEL")
            }
        }
    }

    override fun onNavigationStateChanged(hasPrevious: Boolean, hasNext: Boolean) {
        prevEnabled = hasPrevious
        nextEnabled = hasNext
    }

    override fun onSkillsUpdated() {
        ApplicationManager.getApplication().executeOnPooledThread {
            val skills = controller.getAvailableSkills()
            ApplicationManager.getApplication().invokeLater {
                if (!com.intellij.openapi.util.Disposer.isDisposed(this)) {
                    refreshSkills(skills)
                }
            }
        }
    }

    override fun onFeatureSelected(feature: BacklogFeature?) {
        if (runButton == null || lastStatus != BacklogStatus.OK) return
        featureName.text = feature?.let { "${it.name}" } ?: "No feature selected"
        featureDesc.text = feature?.description?.let { truncate(it) } ?: ""
        featureSelected = feature != null
    }

    override fun onPromptGenerated(prompt: String) {

    }

    override fun onCompletion(success: Boolean) {

    }

    private fun truncate(text: String, max: Int = 600): String = if (text.length <= max) text else text.substring(0, max) + "…"

    private fun refreshSkills(skills: List<Skill> = emptyList()) {
        skillsPanel.removeAll()
        if (skills.isEmpty()) {
            val emptyPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                add(JBLabel("<html><center>No skills found.<br>Press the refresh button above to download the latest Claude Code AI Agent Skills.</center></html>").apply {
                    horizontalAlignment = SwingConstants.CENTER
                    border = JBUI.Borders.empty(10)
                }, BorderLayout.CENTER)
            }
            skillsPanel.add(emptyPanel, BorderLayout.NORTH)
        } else {
            val listPanel = JBPanel<JBPanel<*>>().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
            }
            skills.forEach { skill ->
                val nameLabel = JBLabel(skill.name).apply {
                    toolTipText = skill.description
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    addMouseListener(object : java.awt.event.MouseAdapter() {
                        override fun mouseClicked(e: java.awt.event.MouseEvent) {
                            controller.openSkillFile(skill)
                        }
                    })
                }

                val checkbox = JBCheckBox("", controller.isSkillSelected(skill)).apply {
                    addActionListener {
                        controller.toggleSkill(skill)
                    }
                }
                
                val itemPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                    add(checkbox, BorderLayout.WEST)
                    add(nameLabel, BorderLayout.CENTER)
                    border = JBUI.Borders.empty(2, 5)
                }
                
                listPanel.add(itemPanel)
            }
            skillsPanel.add(JBScrollPane(listPanel).apply { border = JBUI.Borders.empty() }, BorderLayout.CENTER)
        }
        skillsPanel.revalidate()
        skillsPanel.repaint()
    }
}
