package com.github.crunchwrap89.awebstormplugin.featureorchestrator.core

import com.github.crunchwrap89.awebstormplugin.featureorchestrator.model.BacklogFeature
import com.github.crunchwrap89.awebstormplugin.featureorchestrator.model.OrchestratorState
import com.github.crunchwrap89.awebstormplugin.featureorchestrator.settings.OrchestratorSettings
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.util.concurrent.atomic.AtomicReference

class OrchestratorControllerTest : BasePlatformTestCase() {

    private lateinit var controller: OrchestratorController
    private val lastState = AtomicReference<OrchestratorState>(OrchestratorState.IDLE)

    override fun setUp() {
        super.setUp()
        val settings = project.service<OrchestratorSettings>()
        settings.showNotificationAfterHandoff = false

        val listener = object : OrchestratorController.Listener {
            override fun onStateChanged(state: OrchestratorState) {
                lastState.set(state)
            }
            override fun onLog(message: String) {
            }
            override fun onFeaturePreview(feature: BacklogFeature?) {}
            override fun onChangeCountChanged(count: Int) {}
            override fun onPromptGenerated(prompt: String) {}
            override fun onCompletion(success: Boolean) {}
        }
        controller = OrchestratorController(project, listener)
    }

    override fun tearDown() {
        controller.dispose()
        super.tearDown()
    }

    fun testVerifyRetry() {
        // Create backlog.md
        myFixture.addFileToProject("backlog.md", """
            # Backlog
            ## Feature
            - [ ] Test Feature
            ### Description
            Test description
            ### Acceptance Criteria
            - File exists: test.txt
        """.trimIndent())

        // Run next feature
        controller.runNextFeature()
        assertEquals(OrchestratorState.RUNNING, lastState.get())

        // Verify - should fail because test.txt does not exist
        controller.verifyNow()
        assertEquals(OrchestratorState.FAILED, lastState.get())

        // Create test.txt
        myFixture.addFileToProject("test.txt", "content")

        // Verify again - should succeed
        controller.verifyNow()
        assertEquals(OrchestratorState.COMPLETED, lastState.get())
    }
}
