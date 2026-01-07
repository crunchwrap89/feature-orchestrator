package com.github.crunchwrap89.featureorchestrator.featureorchestrator.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.components.service
import com.intellij.ui.SimpleListCellRenderer
import javax.swing.JList

class OrchestratorSettingsConfigurable(private val project: Project) : Configurable {

    private var settingsPanel: JPanel? = null
    private val completionBehaviorComboBox = ComboBox(CompletionBehavior.values()).apply {
        renderer = object : SimpleListCellRenderer<CompletionBehavior>() {
            override fun customize(list: JList<out CompletionBehavior>, value: CompletionBehavior?, index: Int, selected: Boolean, hasFocus: Boolean) {
                text = when (value) {
                    CompletionBehavior.CHECK_OFF -> "Check off feature"
                    CompletionBehavior.MOVE_TO_COMPLETED -> "Move to completed"
                    CompletionBehavior.REMOVE_FEATURE -> "Remove from backlog"
                    null -> ""
                }
            }
        }
    }
    private val promptHandoffComboBox = ComboBox(PromptHandoffBehavior.values()).apply {
        renderer = object : SimpleListCellRenderer<PromptHandoffBehavior>() {
            override fun customize(list: JList<out PromptHandoffBehavior>, value: PromptHandoffBehavior?, index: Int, selected: Boolean, hasFocus: Boolean) {
                text = when (value) {
                    PromptHandoffBehavior.COPY_TO_CLIPBOARD -> "Copy prompt to clipboard"
                    PromptHandoffBehavior.AUTO_COPILOT -> "Automatically add prompt to Copilot AI Agent"
                    PromptHandoffBehavior.AUTO_AI_ASSISTANT -> "Automatically add prompt to AI Assistant"
                    null -> ""
                }
            }
        }
    }
    private val showNotificationCheckBox = JBCheckBox("Show notification after prompt generation/verification failure")
    private val showAcceptanceCriteriaCheckBox = JBCheckBox("Show Acceptance Criteria preview window")
    private val commandTimeoutField = JBTextField()
    private val featureTemplateArea = JBTextArea(10, 50).apply {
        lineWrap = true
        wrapStyleWord = true
    }

    override fun getDisplayName(): String = "Feature Orchestrator"

    override fun createComponent(): JComponent {
        settingsPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Completion behavior:"), completionBehaviorComboBox)
            .addLabeledComponent(JBLabel("Prompt handoff:"), promptHandoffComboBox)
            .addComponent(showNotificationCheckBox)
            .addComponent(showAcceptanceCriteriaCheckBox)
            .addLabeledComponent(JBLabel("Command timeout (seconds):"), commandTimeoutField)
            .addSeparator()
            .addLabeledComponent(JBLabel("New feature template:"), JBScrollPane(featureTemplateArea))
            .addComponentFillVertically(JPanel(), 0)
            .panel
        return settingsPanel!!
    }

    override fun isModified(): Boolean {
        val settings = project.service<OrchestratorSettings>()
        var modified = completionBehaviorComboBox.selectedItem != settings.completionBehavior
        modified = modified or (promptHandoffComboBox.selectedItem != settings.promptHandoffBehavior)
        modified = modified or (showNotificationCheckBox.isSelected != settings.showNotificationAfterHandoff)
        modified = modified or (showAcceptanceCriteriaCheckBox.isSelected != settings.showAcceptanceCriteria)
        val timeout = commandTimeoutField.text.toIntOrNull() ?: 600
        modified = modified or (timeout != settings.commandTimeoutSeconds)
        modified = modified or (featureTemplateArea.text != settings.featureTemplate)
        return modified
    }

    override fun apply() {
        val settings = project.service<OrchestratorSettings>()
        settings.completionBehavior = completionBehaviorComboBox.selectedItem as CompletionBehavior
        settings.promptHandoffBehavior = promptHandoffComboBox.selectedItem as PromptHandoffBehavior
        settings.showNotificationAfterHandoff = showNotificationCheckBox.isSelected
        settings.showAcceptanceCriteria = showAcceptanceCriteriaCheckBox.isSelected
        settings.commandTimeoutSeconds = commandTimeoutField.text.toIntOrNull() ?: 600
        settings.featureTemplate = featureTemplateArea.text
    }

    override fun reset() {
        val settings = project.service<OrchestratorSettings>()
        completionBehaviorComboBox.selectedItem = settings.completionBehavior
        promptHandoffComboBox.selectedItem = settings.promptHandoffBehavior
        showNotificationCheckBox.isSelected = settings.showNotificationAfterHandoff
        showAcceptanceCriteriaCheckBox.isSelected = settings.showAcceptanceCriteria
        commandTimeoutField.text = settings.commandTimeoutSeconds.toString()
        featureTemplateArea.text = settings.featureTemplate
    }

    override fun disposeUIResources() {
        settingsPanel = null
    }
}
