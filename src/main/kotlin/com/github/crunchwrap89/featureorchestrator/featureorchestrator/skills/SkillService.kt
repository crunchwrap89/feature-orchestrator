package com.github.crunchwrap89.featureorchestrator.featureorchestrator.skills

import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.Skill
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.HttpRequests
import java.io.File
import java.nio.file.Paths

@Service(Service.Level.PROJECT)
class SkillService(private val project: Project) {
    internal var skillsDirOverride: String? = null

    private val globalSkillsDir: String by lazy {
        skillsDirOverride ?: run {
            val systemPath = PathManager.getSystemPath()
            val newDir = Paths.get(systemPath, "feature-orchestrator", "skills").toString()
            
            // Migration from ~/.feature-orchestrator/skills if it exists
            val userHome = System.getProperty("user.home")
            val oldGlobalDir = Paths.get(userHome, ".feature-orchestrator", "skills").toFile()
            val newDirFile = File(newDir)
            
            if (oldGlobalDir.exists()) {
                if (!newDirFile.exists()) {
                    try {
                        newDirFile.parentFile.mkdirs()
                        oldGlobalDir.renameTo(newDirFile)
                        if (!newDirFile.exists()) {
                            oldGlobalDir.copyRecursively(newDirFile)
                            oldGlobalDir.deleteRecursively()
                        }
                    } catch (e: Exception) {
                        thisLogger().warn("Failed to migrate skills from $oldGlobalDir to $newDir", e)
                    }
                } else {
                    // Both exist? Move manual skills from ~/.feature-orchestrator/skills to system path
                    oldGlobalDir.listFiles()?.filter { it.isDirectory }?.forEach { skillDir ->
                        val targetDir = File(newDirFile, skillDir.name)
                        if (!targetDir.exists()) {
                            skillDir.renameTo(targetDir)
                        }
                    }
                }
            }
            
            newDir
        }
    }
    private val logger = thisLogger()

    interface SkillsUpdateListener {
        companion object {
            val TOPIC = com.intellij.util.messages.Topic.create("SkillsUpdateListener", SkillsUpdateListener::class.java)
        }
        fun onSkillsUpdated()
    }

    fun getSkillsDir(): VirtualFile? {
        val dir = File(globalSkillsDir)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(dir)
    }


    fun loadSkills(): List<Skill> {
        val skillsDir = getSkillsDir() ?: return emptyList()
        val skills = mutableListOf<Skill>()

        skillsDir.children.filter { it.isDirectory }.forEach { skillFolder ->
            val skillMd = skillFolder.findChild("SKILL.md")
            if (skillMd != null) {
                val skill = parseSkill(skillMd)
                if (skill != null) {
                    skills.add(skill)
                }
            }
        }
        return skills
    }

    private fun parseSkill(skillMd: VirtualFile): Skill? {
        val content = try {
            VfsUtil.loadText(skillMd)
        } catch (e: Exception) {
            return null
        }
        
        var name = ""
        var description = ""

        // Simple frontmatter parser
        if (content.startsWith("---")) {
            val endOfFrontmatter = content.indexOf("---", 3)
            if (endOfFrontmatter != -1) {
                val frontmatter = content.substring(3, endOfFrontmatter)
                val lines = frontmatter.lines()

                lines.forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("name:")) {
                        name = trimmed.substringAfter("name:").trim().removeSurrounding("\"").removeSurrounding("'")
                    } else if (trimmed.startsWith("description:")) {
                        description = trimmed.substringAfter("description:").trim().removeSurrounding("\"").removeSurrounding("'")
                    }
                }
            }
        }

        if (name.isBlank()) {
            // Use directory name as fallback
            name = skillMd.parent.name
        }
        return Skill(name, description, skillMd.path)
    }

    fun initializeDefaultSkillsIfNeeded(indicator: ProgressIndicator? = null) {
        val skillsDirFile = File(globalSkillsDir)
        if (skillsDirFile.exists() && skillsDirFile.list()?.isNotEmpty() == true) {
            return
        }

        downloadSkills(indicator)
    }

    fun addManualSkill(name: String): File? {
        val skillDir = File(globalSkillsDir, name)
        if (skillDir.exists()) {
            return null
        }
        skillDir.mkdirs()
        val skillMd = File(skillDir, "SKILL.md")
        skillMd.writeText("""
            ---
            name: $name
            manual: true
            ---
            
            Description of the skill goes here.
        """.trimIndent())
        
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(skillDir)
        ApplicationManager.getApplication().invokeLater {
            project.messageBus.syncPublisher(SkillsUpdateListener.TOPIC).onSkillsUpdated()
        }
        return skillMd
    }

    fun downloadSkills(indicator: ProgressIndicator? = null) {
        logger.info("Initializing default skills from GitHub to $globalSkillsDir")
        indicator?.text = "Fetching skills list..."
        try {
            val skillsListUrl = "https://api.github.com/repos/anthropics/skills/contents/skills"
            val response = try {
                HttpRequests.request(skillsListUrl)
                    .userAgent("Mozilla/5.0 (IntelliJ Plugin)")
                    .readString()
            } catch (e: HttpRequests.HttpStatusException) {
                if (e.statusCode == 403) {
                    logger.warn("GitHub API rate limit exceeded or access forbidden. Please try again later. URL: $skillsListUrl")
                }
                throw e
            }

            val listType = object : TypeToken<List<GithubContent>>() {}.type
            val contents: List<GithubContent> = Gson().fromJson(response, listType)

            val skillDirs = contents.filter { it.type == "dir" }
            val total = skillDirs.size
            
            // Track which skills are from GitHub
            val githubSkillNames = skillDirs.map { it.name }.toSet()
            
            skillDirs.forEachIndexed { index, dir ->
                indicator?.checkCanceled()
                indicator?.fraction = index.toDouble() / total
                indicator?.text = "Downloading skill: ${dir.name} ($index/$total)"
                
                val skillDir = File(globalSkillsDir, dir.name)
                val skillMdFile = File(skillDir, "SKILL.md")
                
                if (skillMdFile.exists()) {
                    // Check if it's a manual skill
                    val content = skillMdFile.readText()
                    if (content.contains("manual: true")) {
                        logger.info("Skipping official download for manual skill: ${dir.name}")
                    } else {
                        downloadSkill(dir.name, dir.url)
                    }
                } else {
                    downloadSkill(dir.name, dir.url)
                }
                
                // Refresh VFS for the new skill folder
                val skillDirFile = File(globalSkillsDir, dir.name)
                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(skillDirFile)
                
                // Notify UI after each skill
                ApplicationManager.getApplication().invokeLater {
                    project.messageBus.syncPublisher(SkillsUpdateListener.TOPIC).onSkillsUpdated()
                }
            }

            LocalFileSystem.getInstance().refresh(true)
            indicator?.fraction = 1.0
            indicator?.text = "Skills download completed"
        } catch (e: Exception) {
            logger.error("Failed to initialize default skills", e)
        }
    }

    private fun downloadSkill(skillName: String, apiUrl: String) {
        try {
            val response = try {
                HttpRequests.request(apiUrl)
                    .userAgent("Mozilla/5.0 (IntelliJ Plugin)")
                    .readString()
            } catch (e: HttpRequests.HttpStatusException) {
                if (e.statusCode == 403) {
                    logger.warn("GitHub API rate limit exceeded or access forbidden. Please try again later. URL: $apiUrl")
                }
                throw e
            }

            val listType = object : TypeToken<List<GithubContent>>() {}.type
            val contents: List<GithubContent> = Gson().fromJson(response, listType)

            val skillDir = File(globalSkillsDir, skillName)
            skillDir.mkdirs()

            contents.forEach { content ->
                if (content.type == "file") {
                    val file = File(skillDir, content.name)
                    HttpRequests.request(content.download_url!!)
                        .userAgent("Mozilla/5.0 (IntelliJ Plugin)")
                        .saveToFile(file, null)
                } else if (content.type == "dir") {
                    downloadSubDir(skillDir, content.name, content.url)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to download skill $skillName", e)
        }
    }

    private fun downloadSubDir(parentDir: File, dirName: String, apiUrl: String) {
        val currentDir = File(parentDir, dirName)
        currentDir.mkdirs()
        
        try {
            val response = try {
                HttpRequests.request(apiUrl)
                    .userAgent("Mozilla/5.0 (IntelliJ Plugin)")
                    .readString()
            } catch (e: HttpRequests.HttpStatusException) {
                if (e.statusCode == 403) {
                    logger.warn("GitHub API rate limit exceeded or access forbidden. Please try again later. URL: $apiUrl")
                }
                throw e
            }

            val listType = object : TypeToken<List<GithubContent>>() {}.type
            val contents: List<GithubContent> = Gson().fromJson(response, listType)

            contents.forEach { content ->
                if (content.type == "file") {
                    val file = File(currentDir, content.name)
                    HttpRequests.request(content.download_url!!)
                        .userAgent("Mozilla/5.0 (IntelliJ Plugin)")
                        .saveToFile(file, null)
                } else if (content.type == "dir") {
                    downloadSubDir(currentDir, content.name, content.url)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to download subdirectory $dirName", e)
        }
    }

    private data class GithubContent(
        val name: String,
        val type: String,
        val url: String,
        val download_url: String?
    )
}
