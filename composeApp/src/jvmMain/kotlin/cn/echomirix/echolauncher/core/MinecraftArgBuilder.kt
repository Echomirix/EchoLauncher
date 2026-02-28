package cn.echomirix.echolauncher.core

import cn.echomirix.echolauncher.core.config.AppConstant
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import java.io.File

/**
 * 启动上下文数据类，包含启动 Minecraft 所需的所有参数。
 *
 * @property authPlayerName 玩家名称
 * @property authUuid 玩家 UUID
 * @property authAccessToken 认证访问令牌
 * @property version Minecraft 版本号（如 "1.20.1"）
 * @property minecraftDir Minecraft 根目录
 * @property javaPath Java 可执行文件路径
 * @property launcherName 启动器名称
 * @property launcherVersion 启动器版本
 * @property osName 当前操作系统名称
 * @property osArch 当前操作系统架构
 * @property features 特性映射（如是否为演示用户等）
 * @property resolutionWidth 游戏窗口宽度
 * @property resolutionHeight 游戏窗口高度
 * @property isIsolated 是否启用版本隔离
 */
@Serializable
data class LaunchContext(
    val authPlayerName: String,
    val authUuid: String,
    val authAccessToken: String,
    val version: String,          // 比如 "1.20.1"
    val minecraftDir: String,
    val javaPath: String,
    val launcherName: String = AppConstant.APP_NAME,
    val launcherVersion: String = AppConstant.APP_VERSION,
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
    /** 游戏目录路径，若启用隔离则为 versions/版本号 子目录 */
    val gameDirectory: String
        get() = if (isIsolated)
            File(minecraftDir, "versions/$version").absolutePath
        else
            File(minecraftDir).absolutePath

    /** 资源文件根目录 */
    val assetsRoot: String get() = File(minecraftDir, "assets").absolutePath

    /** 库文件目录 */
    val librariesDirectory: String get() = File(minecraftDir, "libraries").absolutePath

    /** 原生库目录，若启用隔离则为版本专属目录 */
    val nativesDirectory: String
        get() = if (isIsolated) File(gameDirectory, "$version-natives").absolutePath else File(
            minecraftDir,
            "natives"
        ).absolutePath

    /** 版本目录 */
    val versionsDirectory: String get() = File(minecraftDir, "versions").absolutePath

    override fun toString(): String {
        return "LaunchContext(玩家=$authPlayerName, 版本=$version, 根目录=$minecraftDir)"
    }
}

/**
 * Minecraft 启动参数构建器，根据版本元数据和启动上下文生成启动参数列表。
 *
 * @property versionMeta Minecraft 版本元数据
 * @property context 启动上下文
 */
class MinecraftArgBuilder(
    private val versionMeta: MinecraftVersionMeta,
    private val context: LaunchContext
) {

    /** 日志记录器 */
    private val logger = KotlinLogging.logger {}

    /** 资源索引名称，懒加载 */
    private val assetsIndexName: String by lazy {
        versionMeta.getAssetIndexId()
    }

    /** 生成的 classpath，懒加载 */
    private val generatedClasspath: String by lazy {
        generateClasspath()
    }

    /** 启动参数宏映射表，懒加载 */
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
            "\${version_type}" to AppConstant.APP_ABBR,
            "\${resolution_width}" to context.resolutionWidth,
            "\${resolution_height}" to context.resolutionHeight,
            "\${natives_directory}" to context.nativesDirectory,
            "\${launcher_name}" to context.launcherName,
            "\${launcher_version}" to context.launcherVersion,
            "\${classpath}" to generatedClasspath
        )
    }

    /**
     * 构建启动参数列表。
     * @return 启动参数字符串列表
     */
    fun build(): List<String> {
        val hasLegacyArgs = !versionMeta.model.minecraftArguments.isNullOrBlank()
        val hasModernArgs = (versionMeta.model.arguments?.jvm?.isNotEmpty() == true) ||
                (versionMeta.model.arguments?.game?.isNotEmpty() == true)

        val jvmArgs = mutableListOf<String>()
        val gameArgs = mutableListOf<String>()

        when {
            hasModernArgs -> {
                // 现代版本 (1.13+)
                val safeArgs = versionMeta.model.arguments
                jvmArgs.addAll(parseArgumentList(safeArgs.jvm))
                gameArgs.addAll(parseArgumentList(safeArgs.game))
            }

            hasLegacyArgs -> {
                // 远古版本 (≤1.12.2)
                jvmArgs += listOf(
                    "-Djava.library.path=${context.nativesDirectory}",
                    "-cp",
                    generatedClasspath,
                    "-Xmx2G"
                )

                val legacyArgs = versionMeta.model.minecraftArguments
                    .split(' ')
                    .filter { it.isNotBlank() }
                    .map { replaceMacros(it) }

                gameArgs.addAll(legacyArgs)
            }

            else -> throw IllegalStateException("既没有 arguments 也没有 minecraftArguments，无法启动！")
        }

        return jvmArgs + listOf(versionMeta.getMainClass()) + gameArgs
    }

    /**
     * 生成 classpath 路径字符串。
     * @return classpath 路径
     */
    private fun generateClasspath(): String {
        val classpathFiles = mutableListOf<String>()

        for (lib in versionMeta.model.libraries) {
            // 检查规则，不符合当前系统的库直接跳过
            if (!checkRules(lib.rules, isLibraryRule = true)) continue

            val pathStr = lib.downloads?.artifact?.path
                ?: cn.echomirix.echolauncher.util.parseLibraryPath(lib) // 假设你有一个根据 name 比如 org.ow2.asm:asm:9.9 推导路径的工具方法

            if (pathStr == null) continue
            var absoluteLibFile: File
            if (lib.natives == null) {
                absoluteLibFile = File(context.librariesDirectory, pathStr)
            } else {
                continue
            }

            if (!absoluteLibFile.exists()) {
                logger.warn { "依赖库丢失，游戏可能会崩溃！缺失文件: ${absoluteLibFile.absolutePath}" }
            }

            classpathFiles.add(absoluteLibFile.absolutePath)
        }

        val absoluteGameJar = File(versionMeta.clientJarPath)
        if (!absoluteGameJar.exists()) {
            throw IllegalStateException("找不到游戏本体 ${versionMeta.clientJarPath}！")
        }
        classpathFiles.add(absoluteGameJar.absolutePath)

        return classpathFiles.joinToString(File.pathSeparator)
    }

    /**
     * 解析参数列表，将宏替换为实际值。
     * @param array 参数对象列表
     * @return 解析后的参数字符串列表
     */
    private fun parseArgumentList(array: List<Argument>): List<String> {
        val result = mutableListOf<String>()

        for (element in array) {
            when (element) {
                // 如果是普通字符串，直接替换宏
                is StringArgument -> {
                    result.add(replaceMacros(element.value))
                }

                // 如果是带规则的复杂参数
                is RuleArgument -> {
                    if (checkRules(element.rules)) {
                        val valueElement = element.value
                        // 官方 JSON 的 value 可能是单一字符串，也可能是数组
                        if (valueElement is JsonArray) {
                            valueElement.forEach { item ->
                                if (item is JsonPrimitive && item.isString) {
                                    result.add(replaceMacros(item.content))
                                }
                            }
                        } else if (valueElement is JsonPrimitive && valueElement.isString) {
                            result.add(replaceMacros(valueElement.content))
                        }
                    }
                }
            }
        }
        return result
    }


    /**
     * 校验规则列表
     * @param rules 规则列表
     * @param isLibraryRule 是否为库规则
     * @return true 表示规则允许应用该参数/库，false 表示拒绝
     */
    private fun checkRules(rules: List<Rule>?, isLibraryRule: Boolean = false): Boolean {
        // 如果没有规则，默认允许
        if (rules.isNullOrEmpty()) return true

        var isAllowed = false // 对于有规则的情况，默认必须有一条匹配 allow 才能生效

        for (rule in rules) {
            val isRuleMatched = matchRule(rule, isLibraryRule)

            if (rule.action == "allow" && isRuleMatched) {
                isAllowed = true
            } else if (rule.action == "disallow" && isRuleMatched) {
                // 如果命中了一条 disallow 规则，直接拒绝并终止检查
                isAllowed = false
                break
            }
        }
        return isAllowed
    }

    /**
     * 判断单条规则是否匹配当前环境
     * @param rule 规则对象
     * @param isLibraryRule 是否为库规则
     * @return 是否匹配
     */
    private fun matchRule(rule: Rule, isLibraryRule: Boolean): Boolean {
        // 1. 检查 OS
        if (rule.os != null) {
            val osNameRule = rule.os.name
            val osArchRule = rule.os.arch

            if (osNameRule != null && osNameRule != context.osName) return false
            if (osArchRule != null && osArchRule != context.osArch) return false
            // version 暂时不校验
        }

        // 2. 检查 Features (库依赖通常不包含 feature，参数包含)
        if (!isLibraryRule && rule.features != null) {
            for ((key, requiredValue) in rule.features) {
                val actualValue = context.features[key] ?: false
                if (requiredValue != actualValue) return false
            }
        }

        return true
    }

    /**
     * 替换参数字符串中的宏为实际值。
     * @param input 输入字符串
     * @return 替换后的字符串
     */
    private fun replaceMacros(input: String): String {
        var output = input
        for ((macro, value) in macroMap) {
            // 容错：如果 value 是 ${...} 本身导致死循环，这里用 replace 即可
            output = output.replace(macro, value)
        }
        return output
    }
}

// --- 辅助工具函数 ---
/**
 * 获取当前操作系统名称（windows、osx、linux、unknown）
 * @return 操作系统名称字符串
 */
fun getCurrentOsName(): String {
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("win") -> "windows"
        os.contains("mac") -> "osx"
        os.contains("linux") || os.contains("unix") -> "linux"
        else -> "unknown"
    }
}

/**
 * 获取当前操作系统架构（如 x86、arm64 等）
 * @return 操作系统架构字符串
 */
fun getCurrentOsArch(): String {
    val arch = System.getProperty("os.arch").lowercase()
    // 粗略判断，可根据实际需要精细化 (如区分 arm32 和 aarch64)
    return when {
        arch.contains("64") && !arch.contains("aarch64") && !arch.contains("arm64") -> "x86"
        else -> arch
    }
}