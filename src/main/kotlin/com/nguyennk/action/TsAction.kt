package com.nguyennk.action

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.icons.AllIcons
import com.intellij.lang.javascript.TypeScriptFileType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.nodejs.run.NodeJsRunConfiguration
import com.jetbrains.nodejs.run.NodeJsRunConfigurationType
import javax.swing.Icon

open class TsAction(icon: Icon = AllIcons.Actions.Execute) : AnAction(icon) {
    private val logger = Logger.getInstance(javaClass)
    private val testRegex = """.*\.(spec|test)\.[jt]sx?${'$'}""".toRegex()

    protected open val debug: Boolean = false

    override fun actionPerformed(event: AnActionEvent) {
        logger.info("""[actionPerformed]""")
        val project = event.getData(CommonDataKeys.PROJECT) as Project
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE) as VirtualFile

        val runManager = RunManager.getInstance(event.project!!)
        val type = NodeJsRunConfigurationType.getInstance()
        val settings = runManager.createConfiguration(virtualFile.name, type)
        val nodeJsRunConf = settings.configuration as NodeJsRunConfiguration
        logger.info("working directory: ${nodeJsRunConf.workingDirectory}")

        if (nodeJsRunConf.workingDirectory.isNullOrEmpty())
            nodeJsRunConf.workingDirectory = project.basePath

        nodeJsRunConf.inputPath = virtualFile.path.replace(nodeJsRunConf.workingDirectory!!, "").substring(1)
        if (nodeJsRunConf.programParameters?.contains("--require ts-node/register") != true) {
            nodeJsRunConf.programParameters = "--require ts-node/register " + nodeJsRunConf.programParameters.orEmpty()
        }

//        runManager.addConfiguration(settings)
        runManager.setTemporaryConfiguration(settings)

        val executor =
            if (debug) DefaultDebugExecutor.getDebugExecutorInstance() else DefaultRunExecutor.getRunExecutorInstance()

        ProgramRunnerUtil.executeConfiguration(settings, executor)
    }

    override fun update(event: AnActionEvent) {
        logger.info("""[update] - $javaClass""")

        val project = event.getData(CommonDataKeys.PROJECT) as Project
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE) as VirtualFile

        if (shouldShow(virtualFile, project)) {
            event.presentation.isEnabledAndVisible = true
            event.presentation.text = getText(virtualFile)
        } else {
            event.presentation.isEnabledAndVisible = false
        }
    }

    private fun shouldShow(virtualFile: VirtualFile, project: Project): Boolean {
        val existed = getExistedConfiguration(virtualFile, project)
        return virtualFile.fileType is TypeScriptFileType
                && existed == null
                && !testRegex.matches(virtualFile.name)
    }

    private fun getText(virtualFile: VirtualFile): String {
        return if (debug) {
            "Debug '${virtualFile.name}'"
        } else {
            "Run '${virtualFile.name}'"
        }
    }

    private fun getExistedConfiguration(file: VirtualFile, project: Project): RunnerAndConfigurationSettings? {
        val runManager = RunManager.getInstance(project)
        val allSettings = runManager.getConfigurationSettingsList(NodeJsRunConfigurationType.getInstance())
        return allSettings.find { x ->
            val config = x.configuration as NodeJsRunConfiguration
            var workingDirectory = config.workingDirectory.orEmpty()
            if (!workingDirectory.endsWith("/")) {
                workingDirectory += "/"
            }
            val fullPath = workingDirectory + config.inputPath.orEmpty()
            fullPath == file.canonicalPath
        }
    }
}
