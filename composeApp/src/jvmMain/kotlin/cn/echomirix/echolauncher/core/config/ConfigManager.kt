package cn.echomirix.echolauncher.core.config

import androidx.compose.runtime.compositionLocalOf
import cn.echomirix.echolauncher.core.account.AccountType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption


val LocalAppConfig = compositionLocalOf<LauncherConfig> {
    throw IllegalStateException("LocalAppConfig not provided. Make sure to wrap your app with CompositionLocalProvider(LocalAppConfig provides appConfig).")
}

@Serializable
data class LauncherConfig(
    val playerName: String = "DefaultPlayer",
    val selectedVersionId: String? = null,
    val isIsolated: Boolean = true, // 是否开启版本隔离
    val customMinecraftDir: String? = null, // 可选的自定义 Minecraft 根
    val microsoftToken: String? = null, // 可选的微软登录 Token
    val littleSkinToken: String? = null,
    val accountType: AccountType = AccountType.OFFLINE,
    val primaryColor: Long = 0xFF6750A4,
    val subColor: Long = 0xFFFEF7F0
)

object ConfigManager {
    private val configFile = File(System.getProperty("user.dir"), "launcher_config.json")
    private val configScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val writeMutex = Mutex()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val _configFlow = MutableStateFlow(LauncherConfig())
    val configFlow: StateFlow<LauncherConfig> = _configFlow.asStateFlow()

    var config: LauncherConfig
        get() = _configFlow.value
        private set(value) {
            _configFlow.value = value
        }

    init {
        load()
    }

    fun load() {
        if (configFile.exists()) {
            try {
                config = json.decodeFromString(configFile.readText())
                println("[Config] 成功读取本地配置！$config")
            } catch (e: Exception) {
                println("[Config] 配置文件解析失败！复默认配置！异常: ${e.message}")
                configScope.launch { save() }
            }
        } else {
            println("[Config] 找不到配置文件，自动生成初始配置...")
            configScope.launch { save() }
        }
    }

    private suspend fun save() {
        writeMutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    val jsonStr = json.encodeToString(config)

                    // 1. 先写到一个临时文件里 (.tmp)
                    val tmpFile = File(configFile.parentFile, "${configFile.name}.tmp")
                    tmpFile.writeText(jsonStr)

                    // 2. 用 NIO 的原子级移动覆盖原文件！
                    // 这样就算在写 tmp 的时候断电，原本的 config.json 也毫发无损！
                    Files.move(
                        tmpFile.toPath(),
                        configFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                    )
                } catch (e: Exception) {
                    println("配置保存惨遭滑铁卢: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    fun updateConfig(block: LauncherConfig.() -> LauncherConfig) {
        _configFlow.value = _configFlow.value.block()
        println("[Config] 配置已更新")
        // 不要阻塞 UI 线程，让后台去排队存文件
        configScope.launch {
            save()
        }
    }
}