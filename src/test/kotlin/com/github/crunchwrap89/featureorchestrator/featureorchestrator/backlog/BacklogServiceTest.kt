package com.github.crunchwrap89.featureorchestrator.featureorchestrator.backlog

import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.BacklogFeature
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.settings.CompletionBehavior
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ui.UIUtil

class BacklogServiceTest : BasePlatformTestCase() {

    private lateinit var service: BacklogService

    override fun setUp() {
        super.setUp()
        service = project.service<BacklogService>()
    }

    fun `test create template backlog`() {
        service.createTemplateBacklog()

        // Thread.sleep(500) // Wait for write action
        UIUtil.dispatchAllInvocationEvents()

        val basePath = myFixture.project.basePath
        val baseDir = com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByPath(basePath!!)
        baseDir?.refresh(false, true)

        val file = baseDir?.findChild("BACKLOG.md")
        assertNotNull("BACKLOG.md should be created", file)

        val content = String(file!!.contentsToByteArray())
        assertTrue(content.contains("# Backlog"))
        assertTrue(content.contains("## Feature name"))
    }

    fun `test append template to backlog`() {
        myFixture.addFileToProject("BACKLOG.md", "# Backlog\n")

        service.appendTemplateToBacklog()

        val file = myFixture.findFileInTempDir("BACKLOG.md")
        val content = String(file.contentsToByteArray())

        assertTrue(content.contains("# Backlog"))
        assertTrue(content.contains("## Feature name"))
        assertTrue(content.contains("New Landing Page"))
    }

    fun `test append empty feature to backlog`() {
        myFixture.addFileToProject("BACKLOG.md", "# Backlog\n")

        service.appendEmptyFeatureToBacklog()

        val file = myFixture.findFileInTempDir("BACKLOG.md")
        val content = String(file.contentsToByteArray())

        assertTrue(content.contains("# Backlog"))
        assertTrue(content.contains("## Feature name"))
        assertTrue(content.contains("### Description"))
        assertFalse(content.contains("New Landing Page"))
    }

    fun `test mark completed check off`() {
        val backlogContent = """
            # Backlog
            
            ---
            ## Feature name
            [ ] Feature 1
            ### Description
            Desc
            ---
        """.trimIndent()
        myFixture.addFileToProject("BACKLOG.md", backlogContent)

        val feature = BacklogFeature(
            name = "Feature 1",
            checked = false,
            description = "Desc",
            optionalSections = emptyMap(),
            acceptanceCriteria = emptyList(),
            rawBlock = """
            ## Feature name
            [ ] Feature 1
            ### Description
            Desc
        """.trimIndent(),
            blockStartOffset = 0,
            blockEndOffset = 0
        )

        service.markCompletedOrRemove(feature, CompletionBehavior.CHECK_OFF)

        val file = myFixture.findFileInTempDir("BACKLOG.md")
        val content = String(file.contentsToByteArray())

        assertTrue("Should be checked off", content.contains("[x] Feature 1"))
    }

    fun `test mark completed remove`() {
        val backlogContent = """
            # Backlog
            
            ---
            ## Feature name
            [ ] Feature 1
            ### Description
            Desc
            ---
        """.trimIndent()
        myFixture.addFileToProject("BACKLOG.md", backlogContent)

        val feature = BacklogFeature(
            name = "Feature 1",
            checked = false,
            description = "Desc",
            optionalSections = emptyMap(),
            acceptanceCriteria = emptyList(),
            rawBlock = """
            ## Feature name
            [ ] Feature 1
            ### Description
            Desc
        """.trimIndent(),
            blockStartOffset = 0,
            blockEndOffset = 0
        )

        service.markCompletedOrRemove(feature, CompletionBehavior.REMOVE_FEATURE)

        val file = myFixture.findFileInTempDir("BACKLOG.md")
        val content = String(file.contentsToByteArray())

        assertFalse("Feature should be removed", content.contains("Feature 1"))
    }

    fun `test mark completed move to completed`() {
        val backlogContent = """
            # Backlog
            
            ---
            ## Feature name
            [ ] Feature 1
            ### Description
            Desc
            ---
        """.trimIndent()
        myFixture.addFileToProject("BACKLOG.md", backlogContent)

        val feature = BacklogFeature(
            name = "Feature 1",
            checked = false,
            description = "Desc",
            optionalSections = emptyMap(),
            acceptanceCriteria = emptyList(),
            rawBlock = """
            ## Feature name
            [ ] Feature 1
            ### Description
            Desc
        """.trimIndent(),
            blockStartOffset = 0,
            blockEndOffset = 0
        )

        service.markCompletedOrRemove(feature, CompletionBehavior.MOVE_TO_COMPLETED)

        val backlogFile = myFixture.findFileInTempDir("BACKLOG.md")
        val backlogContentAfter = String(backlogFile.contentsToByteArray())
        assertFalse("Feature should be removed from backlog", backlogContentAfter.contains("Feature 1"))

        val completedFile = myFixture.findFileInTempDir("COMPLETED.md")
        assertNotNull("COMPLETED.md should be created", completedFile)
        val completedContent = String(completedFile.contentsToByteArray())
        assertTrue("Feature should be in completed", completedContent.contains("Feature 1"))
        assertTrue("Feature should be checked in completed", completedContent.contains("[x] Feature 1"))
    }

    fun `test first unchecked feature`() {
        val backlogContent = """
            # Backlog
            
            ---
            ## Feature name
            [x] Feature 1
            ### Description
            Desc
            ---
            ## Feature name
            [ ] Feature 2
            ### Description
            Desc
            ---
        """.trimIndent()
        myFixture.addFileToProject("BACKLOG.md", backlogContent)

        val backlog = service.parseBacklog()
        assertNotNull(backlog)

        val feature = service.firstUncheckedFeature(backlog!!)
        assertNotNull(feature)
        assertEquals("Feature 2", feature?.name)
    }
}
