package cn.echomirix.echolauncher.core

import cn.echomirix.echolauncher.util.extractCrashDescription
import cn.echomirix.echolauncher.util.findLatestCrashLog
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

enum class LaunchState {
    IDLE,
    CHECKING,
    STARTING,
    SUCCESS,
    ERROR,
    CRASHED
}

data class LaunchStatus(
    val state: LaunchState = LaunchState.IDLE,
    val text: String = "等待启动"
)

class LaunchTask(val ctx: LaunchContext) {

    private val logger = KotlinLogging.logger {}
    private val _status = MutableStateFlow(LaunchStatus())
    val status: StateFlow<LaunchStatus> = _status.asStateFlow()

    @Volatile
    var activeProcess: Process? = null
        private set

    private var resetJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _logFlow = MutableSharedFlow<String>(extraBufferCapacity = 100)
    val logFlow: SharedFlow<String> = _logFlow.asSharedFlow()
    private var isLaunchConfirmed = false

    @Volatile
    private var isKilledByUser = false

    suspend fun start() = coroutineScope {
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
            val command = mutableListOf("\"${ctx.javaPath}\"").apply { addAll(args) }

            val process = ProcessBuilder(command)
                .directory(File(ctx.gameDirectory))
                .redirectErrorStream(true)
                .start()

            activeProcess = process

            updateStatus(LaunchState.STARTING, "正在等待游戏窗口...")

//                logger.info { "启动命令: ${command.joinToString(" ")}" }

            launch(Dispatchers.IO) { handleLogs() }

            launch(Dispatchers.IO) {

                val exitCode = process.waitFor()
                activeProcess = null
                if (exitCode != 0 && !isKilledByUser) {
                    logger.error { "游戏非正常退出 (Exit Code: $exitCode)" }
                    updateStatus(LaunchState.CRASHED, "游戏崩溃退出 ($exitCode)")

                    // 去寻找最新的崩溃日志
                    val logFile = findLatestCrashLog(File(ctx.gameDirectory))

                    // 将崩溃信息转移到全局管理器中，这样即使本 Task 马上被销毁，UI也能弹窗
                    val description = extractCrashDescription(logFile)
                    LaunchManager.reportCrash(ctx.version, exitCode, description, logFile)
                } else {
                    logger.info { "游戏已正常退出 (Exit Code: $exitCode)" }
                    updateStatus(LaunchState.IDLE, "游戏已退出 (Exit Code: $exitCode)")
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            activeProcess = null
            updateStatus(LaunchState.ERROR, "启动异常: ${e.message ?: "未知错误"}")
            resetStatusAfterDelay(5000)
        }
    }

    private fun handleLogs() {

        val timeoutJob = scope.launch {
            delay(25000)
            if (!isLaunchConfirmed && activeProcess?.isAlive == true) {
                isLaunchConfirmed = true
                updateStatus(LaunchState.SUCCESS, "游戏似乎已成功运行 (超时自动判定)")
                resetStatusAfterDelay(4000, forceReset = true)
            }
        }

        val p = activeProcess ?: return
        var handler: (String) -> Unit
        val successMarkers = listOf(
            "Reloading ResourceManager",
            "OpenAL initialized",
            "Setting user:", // 某些老版本
            "Backend library:" // 某些渲染器
        )

        handler = { line ->
            logger.info { "[MC Log] $line" }
            if (isLaunchConfirmed || successMarkers.any { marker -> line.contains(marker) }) {
                updateStatus(LaunchState.SUCCESS, "游戏启动成功！")
                resetStatusAfterDelay(4000, forceReset = true)
                isLaunchConfirmed = true
                timeoutJob.cancel()
                // 命中一次后换成纯打印，彻底零匹配开销
                handler = { pure -> logger.debug { "[MC Log] $pure" } }
            }
        }

        p.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line -> handler(line) }
        }
    }

    fun stop() {
        isKilledByUser = true // 标记为主动杀死，忽略崩溃警告
        activeProcess?.destroy()
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