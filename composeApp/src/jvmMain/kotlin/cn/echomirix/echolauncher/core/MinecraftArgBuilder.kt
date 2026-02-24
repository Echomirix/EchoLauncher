package cn.echomirix.echolauncher.core

import cn.echomirix.echolauncher.core.config.AppConfig
import cn.echomirix.echolauncher.util.parseLibraryPath
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.io.File

/**
 * 环境上下文：极简模式！
 */
@Serializable
data class LaunchContext(
    val authPlayerName: String,
    val authUuid: String,
    val authAccessToken: String,
    val version: String,          // 比如 "1.20.1"
    val minecraftDir: String,     // 唯一的基准根目录，比如 "D:/Project/Java/EchoLauncher/.minecraft"
    val launcherName: String = AppConfig.APP_NAME,
    val launcherVersion: String = AppConfig.APP_VERSION,
    val osName: String = getCurrentOsName(),
    val osArch: String = getCurrentOsArch(),
    val features: Map<String, Boolean> = mapOf(
        "has_custom_resolution" to true,
        "is_demo_user" to false
    ),
    val resolutionWidth: String = "854",
    val resolutionHeight: String = "480",
    val isIsolated: Boolean = true, // 核心：默认开启版本隔离！
) {
    // ---------------------------------------------------------
    // 核心重构：利用 Kotlin 的动态属性 (get()) 自动推导绝对路径！
    // 外面调用的时候，再也不用传这些恶心人的长字符串了！
    // ---------------------------------------------------------
    val gameDirectory: String get() = if (isIsolated)
        File(minecraftDir, "versions/$version").absolutePath
    else
        File(minecraftDir).absolutePath
    val assetsRoot: String get() = File(minecraftDir, "assets").absolutePath
    val librariesDirectory: String get() = File(minecraftDir, "libraries").absolutePath
    val nativesDirectory: String get() = File(minecraftDir, "natives").absolutePath
    val gameJarPath: String get() = File(minecraftDir, "$version.jar").absolutePath
    val versionsDirectory: String get() = File(minecraftDir, "versions").absolutePath

    override fun toString(): String {
        return "LaunchContext(玩家=$authPlayerName, 版本=$version, 根目录=$minecraftDir)"
    }
}

/**
 * 终极核心：单例解析、一站式生成启动命令
 */
class MinecraftArgBuilder(
    private val versionMeta: MinecraftVersionMeta,
    private val context: LaunchContext
) {

    // 自动从 JSON 里掏出 assetsIndex 的名字，坚决不让外面传！
    private val assetsIndexName: String by lazy {
        versionMeta.assetIndex["id"]?.jsonPrimitive?.content ?: throw IllegalStateException("缺失 assetIndex.id")
    }

    // 内部自己生成的 Classpath 字符串
    private val generatedClasspath: String by lazy {
        generateClasspath()
    }

    // 占位符映射表，用到 context 里自动推导出的路径
    private val macroMap: Map<String, String> by lazy {
        mapOf(
            "\${auth_player_name}" to context.authPlayerName,
            "\${version_name}" to context.version,
            "\${game_directory}" to context.gameDirectory,
            "\${assets_root}" to context.assetsRoot,
            "\${assets_index_name}" to assetsIndexName,
            "\${auth_uuid}" to context.authUuid,
            "\${auth_access_token}" to context.authAccessToken,
            "\${clientid}" to "null",
            "\${auth_xuid}" to "null",
            "\${user_type}" to "msa",
            "\${version_type}" to AppConfig.APP_ABBR,
            "\${resolution_width}" to context.resolutionWidth,
            "\${resolution_height}" to context.resolutionHeight,
            "\${natives_directory}" to context.nativesDirectory,
            "\${launcher_name}" to context.launcherName,
            "\${launcher_version}" to context.launcherVersion,
            "\${classpath}" to generatedClasspath
        )
    }

    /**
     * 构建最终的启动命令参数列表
     */
    fun build(): List<String> {
        // 直接从 Meta 里拿合并好的参数！不用再操心了！
        val jvmArgs = parseArgumentList(versionMeta.jvmArgs)
        val gameArgs = parseArgumentList(versionMeta.gameArgs)

        return jvmArgs + listOf(versionMeta.mainClass) + gameArgs
    }

    /**
     * 基于已经解析的 rootElement 生成 Classpath
     */
    private fun generateClasspath(): String {

        val classpathFiles = mutableListOf<String>()

        for (libElement in versionMeta.libraries) {
            val libObj = libElement.jsonObject

            if (!checkRules(libObj["rules"]?.jsonArray, isLibraryRule = true)) continue

            val pathStr = parseLibraryPath(libObj) ?: continue

            // 直接用 context.librariesDirectory 拼接
            val absoluteLibFile = File(context.librariesDirectory, pathStr)

            if (!absoluteLibFile.exists()) {
                println("[警告] 依赖库丢失，游戏绝对会崩溃！缺失文件: ${absoluteLibFile.absolutePath}")
            }

            classpathFiles.add(absoluteLibFile.absolutePath)
        }

        // 把游戏本体加进去
        val absoluteGameJar = File(versionMeta.clientJarPath)
        if (!absoluteGameJar.exists()) {
            throw IllegalStateException("你连游戏本体 ${context.gameJarPath} 都没有，玩空气呢？！")
        }
        classpathFiles.add(absoluteGameJar.absolutePath)

        return classpathFiles.joinToString(File.pathSeparator)
    }

    /**
     * 解析 Arguments 数组
     */
    private fun parseArgumentList(array: JsonArray?): List<String> {
        if (array == null) return emptyList()
        val result = mutableListOf<String>()

        for (element in array) {
            when (element) {
                is JsonPrimitive -> {
                    if (element.isString) {
                        result.add(replaceMacros(element.content))
                    }
                }
                is JsonObject -> {
                    if (checkRules(element["rules"]?.jsonArray)) {
                        val valueElement = element["value"]
                        if (valueElement is JsonArray) {
                            valueElement.forEach { result.add(replaceMacros(it.jsonPrimitive.content)) }
                        } else if (valueElement is JsonPrimitive) {
                            result.add(replaceMacros(valueElement.content))
                        }
                    }
                }
                else -> {}
            }
        }
        return result
    }

    /**
     * 大一统规则审判者
     */
    private fun checkRules(rulesArray: JsonArray?, isLibraryRule: Boolean = false): Boolean {
        if (rulesArray == null || rulesArray.isEmpty()) return true

        var isAllowed = false
        for (ruleElement in rulesArray) {
            val rule = ruleElement.jsonObject
            val action = rule["action"]?.jsonPrimitive?.content ?: continue
            val isRuleMatched = matchRule(rule, isLibraryRule)

            if (action == "allow" && isRuleMatched) {
                isAllowed = true
            } else if (action == "disallow" && isRuleMatched) {
                isAllowed = false
            }
        }
        return isAllowed
    }

    /**
     * 匹配具体的单条规则
     */
    private fun matchRule(rule: JsonObject, isLibraryRule: Boolean): Boolean {
        if (rule.containsKey("os")) {
            val osRule = rule["os"]!!.jsonObject
            val osNameRule = osRule["name"]?.jsonPrimitive?.content
            val osArchRule = osRule["arch"]?.jsonPrimitive?.content

            if (osNameRule != null && osNameRule != context.osName) return false
            if (osArchRule != null && osArchRule != context.osArch) return false
            return true
        }

        if (!isLibraryRule && rule.containsKey("features")) {
            val featuresRule = rule["features"]!!.jsonObject
            for ((key, value) in featuresRule) {
                val requiredValue = value.jsonPrimitive.booleanOrNull ?: false
                val actualValue = context.features[key] ?: false
                if (requiredValue != actualValue) return false
            }
            return true
        }

        return true
    }

    /**
     * 宏替换大法
     */
    private fun replaceMacros(input: String): String {
        var output = input
        for ((macro, value) in macroMap) {
            output = output.replace(macro, value)
        }
        return output
    }
}

// --- 辅助工具函数 ---
fun getCurrentOsName(): String {
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("win") -> "windows"
        os.contains("mac") -> "osx"
        os.contains("linux") || os.contains("unix") -> "linux"
        else -> "unknown"
    }
}

fun getCurrentOsArch(): String {
    val arch = System.getProperty("os.arch").lowercase()
    return when {
        arch.contains("64") && !arch.contains("aarch64") && !arch.contains("arm64") -> "x86"
        else -> arch
    }
}