package com.github.crunchwrap89.featureorchestrator.featureorchestrator.skills

import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

class SkillServiceTest : BasePlatformTestCase() {

    private lateinit var skillService: SkillService
    private lateinit var tempDir: File

    override fun setUp() {
        super.setUp()
        skillService = project.service<SkillService>()
        tempDir = createTempDir("skills-test")
        skillService.skillsDirOverride = tempDir.absolutePath
    }

    override fun tearDown() {
        skillService.skillsDirOverride = null
        tempDir.deleteRecursively()
        super.tearDown()
    }

    fun testLoadSkillsIncludesManualOnes() {
        val skillsDir = skillService.getSkillsDir()
        assertNotNull(skillsDir)
        
        val manualSkillDir = File(skillsDir!!.path, "manual-skill")
        manualSkillDir.mkdirs()
        val skillMd = File(manualSkillDir, "SKILL.md")
        skillMd.writeText("""
            ---
            name: Manual Skill
            description: A manually added skill
            ---
            Content here.
        """.trimIndent())
        
        // Refresh VFS
        VfsUtil.markDirtyAndRefresh(false, true, true, skillsDir)
        
        val skills = skillService.loadSkills()
        val manualSkill = skills.find { it.name == "Manual Skill" }
        assertNotNull("Manual skill should be loaded", manualSkill)
        assertEquals("A manually added skill", manualSkill?.description)
    }

    fun testLoadSkillsFallbackToDirName() {
        val skillsDir = skillService.getSkillsDir()
        assertNotNull(skillsDir)

        val namelessSkillDir = File(skillsDir!!.path, "nameless-skill-dir")
        namelessSkillDir.mkdirs()
        val skillMd = File(namelessSkillDir, "SKILL.md")
        skillMd.writeText("""
            ---
            description: A skill without a name field
            ---
        """.trimIndent())

        VfsUtil.markDirtyAndRefresh(false, true, true, skillsDir)

        val skills = skillService.loadSkills()
        val namelessSkill = skills.find { it.path.contains("nameless-skill-dir") }
        assertNotNull("Nameless skill should be loaded", namelessSkill)
        assertEquals("nameless-skill-dir", namelessSkill?.name)
        assertEquals("A skill without a name field", namelessSkill?.description)
    }

    fun testLoadSkillsNoFrontmatter() {
        val skillsDir = skillService.getSkillsDir()
        assertNotNull(skillsDir)

        val noFrontmatterSkillDir = File(skillsDir!!.path, "no-frontmatter-skill")
        noFrontmatterSkillDir.mkdirs()
        val skillMd = File(noFrontmatterSkillDir, "SKILL.md")
        skillMd.writeText("Just some content without frontmatter.")

        VfsUtil.markDirtyAndRefresh(false, true, true, skillsDir)

        val skills = skillService.loadSkills()
        val noFrontmatterSkill = skills.find { it.path.contains("no-frontmatter-skill") }
        assertNotNull("Skill without frontmatter should be loaded", noFrontmatterSkill)
        assertEquals("no-frontmatter-skill", noFrontmatterSkill?.name)
        assertEquals("", noFrontmatterSkill?.description)
    }

    fun testAddManualSkill() {
        val skillName = "Newly Created Skill"
        val skillFile = skillService.addManualSkill(skillName)
        assertNotNull("Skill file should be created", skillFile)
        assertTrue("Skill file should exist", skillFile!!.exists())
        assertTrue("SKILL.md should contain manual: true", skillFile.readText().contains("manual: true"))
        assertTrue("SKILL.md should contain the name", skillFile.readText().contains("name: $skillName"))

        val skills = skillService.loadSkills()
        val addedSkill = skills.find { it.name == skillName }
        assertNotNull("Added skill should be loadable", addedSkill)
    }
}
