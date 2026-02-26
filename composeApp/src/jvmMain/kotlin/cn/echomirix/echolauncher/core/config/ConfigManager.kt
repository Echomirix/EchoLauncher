package cn.echomirix.echolauncher.core.config

import androidx.compose.runtime.compositionLocalOf
import cn.echomirix.echolauncher.core.account.AccountType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File


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
                save()
            }
        } else {
            println("[Config] 找不到配置文件，自动生成初始配置...")
            save()
        }
    }

    fun save() {
        try {
            configFile.writeText(json.encodeToString(config))
        } catch (e: Exception) {
            println("[Config] 配置文件写入失败，你是不是没给读写权限？异常: ${e.message}")
        }
    }

    fun updateConfig(block: LauncherConfig.() -> LauncherConfig) {
        config = config.block()
        save()
    }
}