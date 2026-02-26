package cn.echomirix.echolauncher.core

import cn.echomirix.echolauncher.core.config.AppConstant
import cn.echomirix.echolauncher.util.parseLibraryPath
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.io.File


@Serializable
data class LaunchContext(
    val authPlayerName: String,
    val authUuid: String,
    val authAccessToken: String,
    val version: String,          // 比如 "1.20.1"
    val minecraftDir: String,
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
    val gameDirectory: String
        get() = if (isIsolated)
            File(minecraftDir, "versions/$version").absolutePath
        else
            File(minecraftDir).absolutePath
    val assetsRoot: String get() = File(minecraftDir, "assets").absolutePath
    val librariesDirectory: String get() = File(minecraftDir, "libraries").absolutePath
    val nativesDirectory: String
        get() = if (isIsolated) File(gameDirectory, "$version-natives").absolutePath else File(
            minecraftDir,
            "natives"
        ).absolutePath
    val versionsDirectory: String get() = File(minecraftDir, "versions").absolutePath

    override fun toString(): String {
        return "LaunchContext(玩家=$authPlayerName, 版本=$version, 根目录=$minecraftDir)"
    }
}

class MinecraftArgBuilder(
    private val versionMeta: MinecraftVersionMeta,
    private val context: LaunchContext
) {

    private val assetsIndexName: String by lazy {
        versionMeta.assetIndex["id"]?.jsonPrimitive?.content ?: throw IllegalStateException("缺失 assetIndex.id")
    }

    private val generatedClasspath: String by lazy {
        generateClasspath()
    }

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

    fun build(): List<String> {
        val hasModernArgs = versionMeta.jvmArgs.isNotEmpty() || versionMeta.gameArgs.isNotEmpty()
        val hasLegacyArgs = !versionMeta.minecraftArguments.isNullOrBlank()

        val jvmArgs = mutableListOf<String>()
        val gameArgs = mutableListOf<String>()

        when {
            hasModernArgs -> {
                // 现代版本 (1.13+) 正常 rules 解析
                jvmArgs.addAll(parseArgumentList(versionMeta.jvmArgs))
                gameArgs.addAll(parseArgumentList(versionMeta.gameArgs))
            }

            hasLegacyArgs -> {
                // 远古版本 (≤1.12.2) 没有 arguments，对付那条 minecraftArguments 字符串
                jvmArgs += listOf(
                    "-Djava.library.path=${context.nativesDirectory}",
                    "-cp",
                    generatedClasspath,
                    "-Xmx2G"
                )

                val legacyArgs = versionMeta.minecraftArguments
                    .split(' ')
                    .filter { it.isNotBlank() }
                    .map { replaceMacros(it) }
                gameArgs.addAll(legacyArgs)
            }

            else -> throw IllegalStateException("既没有 arguments 也没有 minecraftArguments，无法启动")
        }

        return jvmArgs + listOf(versionMeta.mainClass) + gameArgs
    }

    private fun generateClasspath(): String {
        val classpathFiles = mutableListOf<String>()

        for (libElement in versionMeta.libraries) {
            val libObj = libElement.jsonObject

            if (!checkRules(libObj["rules"]?.jsonArray, isLibraryRule = true)) continue

            val pathStr = parseLibraryPath(libObj) ?: continue

            val absoluteLibFile = File(context.librariesDirectory, pathStr)

            if (!absoluteLibFile.exists()) {
                println("[警告] 依赖库丢失，游戏可能会崩溃！缺失文件: ${absoluteLibFile.absolutePath}")
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