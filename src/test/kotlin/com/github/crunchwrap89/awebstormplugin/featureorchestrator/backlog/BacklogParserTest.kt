package com.github.crunchwrap89.awebstormplugin.featureorchestrator.backlog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BacklogParserTest {

    @Test
    fun `test parse feature with blank line before description`() {
        val text = """
            # Backlog

            ## Feature
            - [ ] About Page

            ### Description
            Add a new "About" page.
        """.trimIndent()

        val backlog = BacklogParser.parse(text)

        assertTrue("Should have no warnings: ${backlog.warnings}", backlog.warnings.isEmpty())
        assertEquals(1, backlog.features.size)
        val feature = backlog.features[0]
        assertEquals("About Page", feature.name)
        assertEquals("Add a new \"About\" page.", feature.description)
    }

    @Test
    fun `test parse feature without blank line before description`() {
        val text = """
            # Backlog

            ## Feature
            - [ ] About Page
            ### Description
            Add a new "About" page.
        """.trimIndent()

        val backlog = BacklogParser.parse(text)

        assertTrue("Should have no warnings: ${backlog.warnings}", backlog.warnings.isEmpty())
        assertEquals(1, backlog.features.size)
        val feature = backlog.features[0]
        assertEquals("About Page", feature.name)
        assertEquals("Add a new \"About\" page.", feature.description)
    }

    @Test
    fun `test parse feature with mixed acceptance criteria`() {
        val text = """
            # Backlog

            ## Feature
            - [ ] About Page
            ### Description
            Desc
            ### Acceptance Criteria
            - File exists: src/pages/about.tsx
            - Route `/about` renders without errors
            - Visual style matches existing pages
            - Command succeeds: npm run build
            
            ---
        """.trimIndent()

        val backlog = BacklogParser.parse(text)

        assertTrue("Should have no warnings: ${backlog.warnings}", backlog.warnings.isEmpty())
        assertEquals(1, backlog.features.size)
        val feature = backlog.features[0]
        assertEquals(2, feature.acceptanceCriteria.size)
    }
}
