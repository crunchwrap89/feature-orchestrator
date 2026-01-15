package com.github.crunchwrap89.featureorchestrator.featureorchestrator.core

import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.BacklogFeature
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.BacklogStatus
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.OrchestratorState
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.settings.CompletionBehavior
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.settings.OrchestratorSettings
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ui.UIUtil
import java.util.concurrent.atomic.AtomicReference

class OrchestratorControllerTest : BasePlatformTestCase() {

    private lateinit var controller: OrchestratorController
    private val lastState = AtomicReference<OrchestratorState>(OrchestratorState.IDLE)

    override fun setUp() {
        super.setUp()
        TestDialogManager.setTestDialog(TestDialog.OK)

        val listener = object : OrchestratorController.Listener {
            override fun onStateChanged(state: OrchestratorState) {
                lastState.set(state)
            }
            override fun onLog(message: String) {
            }
            override fun onClearLog() {}
            override fun onClearPrompt() {}
            override fun onFeatureSelected(feature: BacklogFeature?) {}
            override fun onPromptGenerated(prompt: String) {}
            override fun onCompletion(success: Boolean) {}
            override fun onBacklogStatusChanged(status: BacklogStatus) {}
            override fun onNavigationStateChanged(hasPrevious: Boolean, hasNext: Boolean) {}
            override fun onSkillsUpdated() {}
        }
        controller = OrchestratorController(project, listener)
    }

    override fun tearDown() {
        controller.dispose()
        super.tearDown()
    }

    fun testMoveToCompleted() {
        val settings = project.service<OrchestratorSettings>()
        settings.completionBehavior = CompletionBehavior.MOVE_TO_COMPLETED

        // Create backlog.md
        myFixture.addFileToProject("backlog.md", """
            # Backlog
            ## Feature name
            [ ] Feature 1
            ### Description
            Desc 1

            ## Feature name
            [ ] Feature 2
            ### Description
            Desc 2
        """.trimIndent())

        // Initial validation (async)
        controller.validateBacklog()
        waitForState(OrchestratorState.IDLE) // Wait for validation to finish (it starts at IDLE, so this is just to ensure it's not BUSY)
        // Actually we need to wait for availableFeatures to be populated.
        // Let's use a simpler wait for state after runNextFeature.

        // Run next feature (Feature 1)
        controller.runNextFeature()
        waitForState(OrchestratorState.AWAITING_AI)
        assertEquals(OrchestratorState.AWAITING_AI, lastState.get())

        // Complete manually (since verification is gone)
        controller.completeFeature()
        waitForState(OrchestratorState.COMPLETED)
        assertEquals(OrchestratorState.COMPLETED, lastState.get())

        // Check backlog.md
        val backlog = myFixture.findFileInTempDir("backlog.md")
        val backlogText = String(backlog.contentsToByteArray())
        assertFalse("Backlog should not contain Feature 1", backlogText.contains("Feature 1"))
        assertTrue("Backlog should contain Feature 2", backlogText.contains("Feature 2"))

        // Check completed.md
        val completed = myFixture.findFileInTempDir("COMPLETED.md")
        assertNotNull("COMPLETED.md should exist", completed)
        val completedText = String(completed.contentsToByteArray())
        assertTrue("Completed should contain Feature 1", completedText.contains("Feature 1"))
        assertTrue("Feature 1 should be checked", completedText.contains("[x] Feature 1"))
    }

    private fun waitForState(expected: OrchestratorState) {
        val start = System.currentTimeMillis()
        while (lastState.get() != expected && System.currentTimeMillis() - start < 5000) {
            UIUtil.dispatchAllInvocationEvents()
            Thread.sleep(10)
        }
    }
}
