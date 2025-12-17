package com.github.crunchwrap89.awebstormplugin.featureorchestrator.core

import com.github.crunchwrap89.awebstormplugin.featureorchestrator.model.AcceptanceCriterion
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

data class VerificationResult(val success: Boolean, val details: List<String>)

object AcceptanceVerifier {
    fun verify(project: Project, criteria: List<AcceptanceCriterion>): VerificationResult {
        if (criteria.isEmpty()) return VerificationResult(false, listOf("No acceptance criteria present."))
        val details = mutableListOf<String>()
        var allOk = true
        criteria.forEach { c ->
            when (c) {
                is AcceptanceCriterion.FileExists -> {
                    var exists = false
                    runReadAction {
                        val roots = ProjectRootManager.getInstance(project).contentRoots
                        for (root in roots) {
                            if (root.findFileByRelativePath(c.relativePath) != null) {
                                exists = true
                                break
                            }
                        }

                        if (!exists) {
                            val path = project.basePath?.let { File(it, c.relativePath) } ?: File(c.relativePath)
                            exists = path.exists()
                            if (exists) LocalFileSystem.getInstance().refreshAndFindFileByIoFile(path)
                        }
                    }

                    details += (if (exists) "✔ File exists: ${c.relativePath}" else "✘ File missing: ${c.relativePath}")
                    allOk = allOk && exists
                }
                is AcceptanceCriterion.CommandSucceeds -> {
                    val cmd = c.command
                    val commandLine = if (isWindows()) {
                        GeneralCommandLine("cmd", "/c", cmd)
                    } else {
                        GeneralCommandLine("sh", "-lc", cmd)
                    }
                    project.basePath?.let { commandLine.withWorkDirectory(it) }
                    val handler = CapturingProcessHandler(commandLine)
                    val output = handler.runProcess(60_000)
                    val ok = output.exitCode == 0
                    details += (if (ok) "✔ Command succeeded: $cmd" else "✘ Command failed ($cmd): exit=${output.exitCode}\n${output.stdout}\n${output.stderr}")
                    allOk = allOk && ok
                }
            }
        }
        return VerificationResult(allOk, details)
    }

    private fun isWindows(): Boolean = System.getProperty("os.name").lowercase().contains("win")
}
