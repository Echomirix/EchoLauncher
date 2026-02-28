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

/**
 * LaunchTask 类用于处理 Minecraft 游戏的启动流程，包括依赖检查、游戏启动以及日志处理等。
 *
 * 该类通过给定的 [LaunchContext] 初始化，提供了启动和停止游戏的方法，并且能够通过状态流和日志流来监控游戏的启动状态和输出日志。
 *
 * @param ctx 包含启动 Minecraft 所需的所有参数的上下文对象
 * @property status 公开的状态流，外部可以订阅以获取当前的启动状态和提示文本
 * @property logFlow 公开的日志流，外部可以订阅以获取游戏的输出日志
 *
 * 注意：此任务类设计为在协程作用域内使用。确保在适当的 CoroutineScope 中调用 start() 方法。
 */
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

    /**
     * 启动 Minecraft 游戏的入口方法。此方法执行一系列步骤来确保游戏能够顺利启动，包括检查和下载必要的依赖文件、构建启动参数、启动游戏进程，并监控游戏进程的状态。
     */
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

    /**
     * 处理游戏启动日志。此方法通过监听游戏进程的标准输出流来监控日志信息，并根据特定的日志标记或超时条件判断游戏是否成功启动。
     */
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

    /**
     * 停止当前正在运行的游戏进程。
     */
    fun stop() {
        isKilledByUser = true // 标记为主动杀死，忽略崩溃警告
        activeProcess?.destroy()
    }

    /**
     * 更新启动状态和状态文本。
     *
     * @param newState 新的状态枚举值，表示当前的启动状态。
     * @param newText 与新状态关联的描述文本。
     */
    private fun updateStatus(newState: LaunchState, newText: String) {
        _status.value = LaunchStatus(newState, newText)
    }

    /**
     * 重置启动状态，在指定延迟时间后执行。
     * 如果`forceReset`为`true`，或者当前激活的进程不再存活，则更新状态为`IDLE`并设置状态文本为"等待启动"。
     *
     * @param timeMillis 延迟的毫秒数，在此之后检查是否需要重置状态。
     * @param forceReset 可选参数，默认为`false`。如果为`true`，则忽略进程存活状态，强制重置状态。
     */
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