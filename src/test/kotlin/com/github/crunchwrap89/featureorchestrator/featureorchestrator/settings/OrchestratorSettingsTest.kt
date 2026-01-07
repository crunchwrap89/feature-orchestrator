package com.github.crunchwrap89.featureorchestrator.featureorchestrator.settings

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class OrchestratorSettingsTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        val settings = project.service<OrchestratorSettings>()
        settings.loadState(OrchestratorSettingsState()) // Reset to defaults
    }

    fun `test default settings`() {
        val settings = project.service<OrchestratorSettings>()

        println("DEBUG: settings.completionBehavior = ${settings.completionBehavior}")
        assertEquals("Completion behavior should be MOVE_TO_COMPLETED by default", CompletionBehavior.MOVE_TO_COMPLETED, settings.completionBehavior)
        assertEquals(PromptHandoffBehavior.COPY_TO_CLIPBOARD, settings.promptHandoffBehavior)
        assertTrue(settings.showNotificationAfterHandoff)
        assertEquals(600, settings.commandTimeoutSeconds)
        assertFalse(settings.showAcceptanceCriteria)
    }

    fun `test persistence`() {
        val settings = project.service<OrchestratorSettings>()

        settings.completionBehavior = CompletionBehavior.CHECK_OFF
        settings.promptHandoffBehavior = PromptHandoffBehavior.AUTO_COPILOT
        settings.showNotificationAfterHandoff = false
        settings.commandTimeoutSeconds = 300
        settings.showAcceptanceCriteria = true

        val state = settings.state
        assertEquals(CompletionBehavior.CHECK_OFF, state.completionBehavior)
        assertEquals(PromptHandoffBehavior.AUTO_COPILOT, state.promptHandoffBehavior)
        assertFalse(state.showNotificationAfterHandoff)
        assertEquals(300, state.commandTimeoutSeconds)
        assertTrue(state.showAcceptanceCriteria)

        // Simulate reload
        val newSettings = OrchestratorSettings(project)
        newSettings.loadState(state)

        assertEquals(CompletionBehavior.CHECK_OFF, newSettings.completionBehavior)
        assertEquals(PromptHandoffBehavior.AUTO_COPILOT, newSettings.promptHandoffBehavior)
        assertFalse(newSettings.showNotificationAfterHandoff)
        assertEquals(300, newSettings.commandTimeoutSeconds)
        assertTrue(newSettings.showAcceptanceCriteria)
    }
}
