package cn.echomirix.echolauncher.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*
import java.io.File

enum class LaunchState {
    IDLE,       // 空闲/等待启动
    CHECKING,   // 校验与下载依赖中
    STARTING,   // JVM已启动，等待游戏引擎初始化
    SUCCESS,    // 捕捉到 Setting user，启动成功
    ERROR       // 发生异常
}

object GameManager {
    // 暴露给 UI 层的响应式状态
    var currentState by mutableStateOf(LaunchState.IDLE)
        private set

    var statusText by mutableStateOf("等待启动")
        private set

    var activeProcess: Process? by mutableStateOf(null)
        private set

    // 专属的后台协程作用域，专门干脏活累活
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startGame(ctx: LaunchContext) {
        if (activeProcess?.isAlive == true) {
            statusText = "游戏已经在运行中，请勿重复启动"
            return
        }                // 1. 实例化终极合并元数据 (它会自动搞定原版/Fabric的继承关系)
        val versionMeta = MinecraftVersionMeta(
            targetVersion = ctx.version,
            versionsDir = File(ctx.versionsDirectory)
        )

        scope.launch {
            try {
                // 1. 切换到校验状态
                withContext(Dispatchers.Main) {
                    currentState = LaunchState.CHECKING
                    statusText = "正在扫描和补全依赖文件..."
                }

                val jsonFile = File(ctx.gameDirectory, "${ctx.version}.json")
                val versionJsonString = jsonFile.readText()

                val verifier = MinecraftDependencyVerifier(
                    versionMeta = versionMeta,
                    librariesDirectory = ctx.librariesDirectory,
                    assetsDirectory = ctx.assetsRoot
                )
                verifier.verifyAndDownloadAll()

                withContext(Dispatchers.Main) {
                    statusText = "依赖就绪，正在生成启动参数..."
                }
                val args = MinecraftArgBuilder(versionMeta, ctx).build()
                val command = mutableListOf("java").apply { addAll(args) }

                // 2. 切换到启动中状态
                withContext(Dispatchers.Main) {
                    statusText = "正在唤醒 JVM，等待游戏窗口渲染..."
                    currentState = LaunchState.STARTING
                }

//                println("[Debug] 启动命令: ${command.joinToString(" ")}")

                val process = ProcessBuilder(command)
                    .directory(File(ctx.gameDirectory))
                    .redirectErrorStream(true)
                    .start()

                withContext(Dispatchers.Main) {
                    activeProcess = process
                }

                // 3. 在独立协程里死盯日志流！
                launch(Dispatchers.IO) {
                    process.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { line ->
                            println("[MC Log] $line")

                            if (currentState == LaunchState.STARTING && line.contains("[Render thread/INFO]: Reloading ResourceManager")) {
                                launch(Dispatchers.Main) {
                                    currentState = LaunchState.SUCCESS
                                    statusText = "游戏启动成功！即将返回..."
                                    // 停留3秒让玩家看清楚成功提示，然后深藏功与名
                                    delay(3000)
                                    currentState = LaunchState.IDLE
                                    statusText = ""
                                }
                            }
                        }
                    }

                    // 进程死亡后的善后工作
                    val exitCode = process.waitFor()
                    withContext(Dispatchers.Main) {
                        if (activeProcess == process) {
                            activeProcess = null
                            currentState = LaunchState.IDLE
                            statusText = "游戏已退出 (代码: $exitCode)"
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    currentState = LaunchState.ERROR
                    statusText = "启动失败: ${e.message}"
                    delay(4000)
                    currentState = LaunchState.IDLE
                    statusText = "等待启动"
                }
            }
        }
    }

    fun killGame() {
        activeProcess?.destroy()
        activeProcess = null
        currentState = LaunchState.IDLE
        statusText = "已强制结束进程"
    }
}