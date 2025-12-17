package com.github.crunchwrap89.awebstormplugin.featureorchestrator.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

enum class CompletionBehavior { CHECK_OFF, REMOVE_FEATURE }

data class OrchestratorSettingsState(
    var completionBehavior: CompletionBehavior = CompletionBehavior.CHECK_OFF,
    var copyPromptToClipboard: Boolean = true,
    var showNotificationAfterHandoff: Boolean = true,
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

    var copyPromptToClipboard: Boolean
        get() = state.copyPromptToClipboard
        set(value) { state.copyPromptToClipboard = value }

    var showNotificationAfterHandoff: Boolean
        get() = state.showNotificationAfterHandoff
        set(value) { state.showNotificationAfterHandoff = value }
}
