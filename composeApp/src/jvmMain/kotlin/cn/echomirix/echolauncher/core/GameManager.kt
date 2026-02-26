package cn.echomirix.echolauncher.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

enum class LaunchState {
    IDLE,
    CHECKING,
    STARTING,
    SUCCESS,
    ERROR
}

data class LaunchStatus(
    val state: LaunchState = LaunchState.IDLE,
    val text: String = "等待启动"
)

object GameManager {
    private val _status = MutableStateFlow(LaunchStatus())
    val status: StateFlow<LaunchStatus> = _status.asStateFlow()

    @Volatile
    var activeProcess: Process? = null
        private set

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var resetJob: Job? = null

    fun startGame(ctx: LaunchContext) {
        if (activeProcess?.isAlive == true || _status.value.state != LaunchState.IDLE) {
            updateStatus(LaunchState.ERROR, "游戏正在运行或处于非空闲状态！")

            resetStatusAfterDelay(3000)
            return
        }

        resetJob?.cancel()

        scope.launch {
            try {
                updateStatus(LaunchState.CHECKING, "正在扫描和补全依赖文件...")

                val versionMeta = MinecraftVersionMeta(
                    targetVersion = ctx.version,
                    versionsDir = File(ctx.versionsDirectory)
                )

                val verifier = MinecraftDependencyVerifier(
                    versionMeta = versionMeta,
                    librariesDirectory = ctx.librariesDirectory,
                    assetsDirectory = ctx.assetsRoot,
                    nativesDirectory = ctx.nativesDirectory
                )
                verifier.verifyAndDownloadAll()

                updateStatus(LaunchState.STARTING, "依赖就绪，正在唤醒 JVM...")

                val args = MinecraftArgBuilder(versionMeta, ctx).build()
                val command = mutableListOf("java").apply { addAll(args) }

                val process = ProcessBuilder(command)
                    .directory(File(ctx.gameDirectory))
                    .redirectErrorStream(true)
                    .start()

                activeProcess = process

                updateStatus(LaunchState.STARTING, "正在等待游戏窗口...")

                println("启动命令: ${command.joinToString(" ")}")

                launch(Dispatchers.IO) {
                    val successMarkers = listOf("Reloading ResourceManager")

                    var handler: (String) -> Unit

                    handler = { line ->
                        println("[MC Log] $line")
                        if (successMarkers.any { marker -> line.contains(marker) }) {
                            updateStatus(LaunchState.SUCCESS, "游戏启动成功！")
                            resetStatusAfterDelay(4000, forceReset = true)
                            // 命中一次后换成纯打印，彻底零匹配开销
                            handler = { pure -> println("[MC Log] $pure") }
                        }
                    }

                    process.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { line -> handler(line) }
                    }

                    // 进程自然死亡或被强制杀死
                    val exitCode = process.waitFor()
                    activeProcess = null
                    updateStatus(LaunchState.IDLE, "游戏已退出 (Exit Code: $exitCode)")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                activeProcess = null
                updateStatus(LaunchState.ERROR, "启动异常: ${e.message ?: "未知错误"}")
                resetStatusAfterDelay(5000)
            }
        }
    }

    fun killGame() {
        activeProcess?.let {
            if (it.isAlive) {
                it.destroy()
                updateStatus(LaunchState.IDLE, "已强行拔管，终止进程")
            }
        }
        activeProcess = null
    }

    private fun updateStatus(newState: LaunchState, newText: String) {
        _status.value = LaunchStatus(newState, newText)
    }

    private fun resetStatusAfterDelay(timeMillis: Long, forceReset: Boolean = false) {
        resetJob?.cancel()
        resetJob = scope.launch {
            delay(timeMillis)
            if (forceReset || activeProcess?.isAlive != true) {
                updateStatus(LaunchState.IDLE, "等待启动")
            }
        }
    }
}