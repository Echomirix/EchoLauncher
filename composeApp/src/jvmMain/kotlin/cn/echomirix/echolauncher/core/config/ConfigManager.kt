package cn.echomirix.echolauncher.core.config

import cn.echomirix.echolauncher.core.account.AccountType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class LauncherConfig(
    val playerName: String = "DefaultPlayer",
    val selectedVersionId: String? = null,
    val isIsolated: Boolean = true, // 是否开启版本隔离
    val customMinecraftDir: String? = null, // 可选的自定义 Minecraft 根
    val microsoftToken: String? = null, // 可选的微软登录 Token
    val littleSkinToken: String? = null,
    val accountType: AccountType = AccountType.OFFLINE,
)

object ConfigManager {
    // 配置文件就直接保存在运行目录下
    private val configFile = File(System.getProperty("user.dir"), "launcher_config.json")

    // 配置一下 Json 序列化器，忽略未知字段防报错，并且开启美化输出
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    // 让你心心念念的 Compose 完美响应式状态流！
    private val _configFlow = MutableStateFlow(LauncherConfig())
    val configFlow: StateFlow<LauncherConfig> = _configFlow.asStateFlow()

    // 快捷访问属性
    var config: LauncherConfig
        get() = _configFlow.value
        private set(value) {
            _configFlow.value = value
        }

    init {
        load()
    }

    /**
     * 把配置从硬盘吃进内存，如果没有就建一个！
     */
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

    /**
     * 把内存里的配置拉一坨到硬盘上！
     */
    fun save() {
        try {
            configFile.writeText(json.encodeToString(config))
        } catch (e: Exception) {
            println("[Config] 配置文件写入失败，你是不是没给读写权限？异常: ${e.message}")
        }
    }

    /**
     * 闭包更新大法，更新完不仅自动保存硬盘，还会触发 Compose 的重组！
     * 用法：ConfigManager.updateConfig { copy(playerName = "新名字") }
     */
    fun updateConfig(block: LauncherConfig.() -> LauncherConfig) {
        config = config.block()
        save()
    }
}