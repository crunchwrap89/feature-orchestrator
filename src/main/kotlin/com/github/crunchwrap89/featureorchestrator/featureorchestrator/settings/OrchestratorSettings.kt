package com.github.crunchwrap89.featureorchestrator.featureorchestrator.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

enum class CompletionBehavior { CHECK_OFF, REMOVE_FEATURE, MOVE_TO_COMPLETED }

enum class PromptHandoffBehavior { COPY_TO_CLIPBOARD, AUTO_COPILOT, AUTO_AI_ASSISTANT }

data class OrchestratorSettingsState(
    var completionBehavior: CompletionBehavior = CompletionBehavior.MOVE_TO_COMPLETED,
    var promptHandoffBehavior: PromptHandoffBehavior = PromptHandoffBehavior.COPY_TO_CLIPBOARD,
    var showNotificationAfterHandoff: Boolean = true,
    var commandTimeoutSeconds: Int = 600,
    var showAcceptanceCriteria: Boolean = false,
)

@Service(Service.Level.PROJECT)
@State(name = "FeatureOrchestratorSettings", storages = [Storage("feature-orchestrator.xml")])
class OrchestratorSettings(private val project: Project) : PersistentStateComponent<OrchestratorSettingsState> {
    private var state = OrchestratorSettingsState()

    override fun getState(): OrchestratorSettingsState = state

    override fun loadState(state: OrchestratorSettingsState) {
        this.state = state
    }

    var completionBehavior: CompletionBehavior
        get() = state.completionBehavior
        set(value) { state.completionBehavior = value }

    var promptHandoffBehavior: PromptHandoffBehavior
        get() = state.promptHandoffBehavior
        set(value) { state.promptHandoffBehavior = value }

    var showNotificationAfterHandoff: Boolean
        get() = state.showNotificationAfterHandoff
        set(value) { state.showNotificationAfterHandoff = value }

    var commandTimeoutSeconds: Int
        get() = state.commandTimeoutSeconds
        set(value) { state.commandTimeoutSeconds = value }

    var showAcceptanceCriteria: Boolean
        get() = state.showAcceptanceCriteria
        set(value) { state.showAcceptanceCriteria = value }
}
