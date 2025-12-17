package com.github.crunchwrap89.awebstormplugin.featureorchestrator.core

import com.github.crunchwrap89.awebstormplugin.featureorchestrator.model.BacklogFeature
import com.github.crunchwrap89.awebstormplugin.featureorchestrator.model.OrchestratorState
import com.github.crunchwrap89.awebstormplugin.featureorchestrator.settings.CompletionBehavior
import com.github.crunchwrap89.awebstormplugin.featureorchestrator.settings.OrchestratorSettings
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ui.UIUtil
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
            override fun onClearLog() {}
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
        waitForState(OrchestratorState.FAILED)
        assertEquals(OrchestratorState.FAILED, lastState.get())

        // Create test.txt
        myFixture.addFileToProject("test.txt", "content")

        // Verify again - should succeed
        controller.verifyNow()
        waitForState(OrchestratorState.COMPLETED)
        assertEquals(OrchestratorState.COMPLETED, lastState.get())
    }

    fun testMoveToCompleted() {
        val settings = project.service<OrchestratorSettings>()
        settings.completionBehavior = CompletionBehavior.MOVE_TO_COMPLETED

        // Create backlog.md
        myFixture.addFileToProject("backlog.md", """
            # Backlog
            ## Feature
            - [ ] Feature 1
            ### Description
            Desc 1
            ### Acceptance Criteria
            - File exists: f1.txt

            ## Feature
            - [ ] Feature 2
            ### Description
            Desc 2
        """.trimIndent())

        myFixture.addFileToProject("f1.txt", "")

        // Run next feature (Feature 1)
        controller.runNextFeature()
        assertEquals(OrchestratorState.RUNNING, lastState.get())

        // Verify
        controller.verifyNow()
        waitForState(OrchestratorState.COMPLETED)
        assertEquals(OrchestratorState.COMPLETED, lastState.get())

        // Check backlog.md
        val backlog = myFixture.findFileInTempDir("backlog.md")
        val backlogText = String(backlog.contentsToByteArray())
        assertFalse("Backlog should not contain Feature 1", backlogText.contains("Feature 1"))
        assertTrue("Backlog should contain Feature 2", backlogText.contains("Feature 2"))

        // Check completed.md
        val completed = myFixture.findFileInTempDir("completed.md")
        assertNotNull("completed.md should exist", completed)
        val completedText = String(completed.contentsToByteArray())
        assertTrue("Completed should contain Feature 1", completedText.contains("Feature 1"))
        assertTrue("Feature 1 should be checked", completedText.contains("- [x] Feature 1"))
    }

    private fun waitForState(expected: OrchestratorState) {
        val start = System.currentTimeMillis()
        while (lastState.get() != expected && System.currentTimeMillis() - start < 5000) {
            UIUtil.dispatchAllInvocationEvents()
            Thread.sleep(10)
        }
    }
}
