package cn.echomirix.echolauncher.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import java.io.File

// ==========================================
// 1. 自定义序列化器 (处理混合数组)
// ==========================================

object ArgumentSerializer : JsonContentPolymorphicSerializer<Argument>(Argument::class) {
    override fun selectDeserializer(element: JsonElement): KSerializer<out Argument> {
        return when (element) {
            is JsonPrimitive -> if (element.isString) StringArgumentSerializer else throw Exception("未知的 primitive Argument")
            is JsonObject -> RuleArgument.serializer()
            else -> throw Exception("无法解析的 Argument 格式")
        }
    }
}

object StringArgumentSerializer : KSerializer<StringArgument> {
    override val descriptor: SerialDescriptor = String.serializer().descriptor

    override fun deserialize(decoder: Decoder): StringArgument {
        return StringArgument(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: StringArgument) {
        encoder.encodeString(value.value)
    }
}

// ==========================================
// 2. 数据模型定义
// ==========================================

@Serializable(with = ArgumentSerializer::class)
sealed interface Argument

data class StringArgument(val value: String) : Argument

@Serializable
data class RuleArgument(
    val rules: List<Rule> = emptyList(),
    // 官方 JSON 里 value 可能是单个字符串，也可能是字符串数组
    val value: JsonElement
) : Argument

@Serializable
data class Arguments(
    val game: List<Argument> = emptyList(),
    val jvm: List<Argument> = emptyList()
)

@Serializable
data class AssetIndex(
    val id: String,
    val sha1: String,
    val size: Int,
    val totalSize: Int,
    val url: String
)

@Serializable
data class Client(
    val sha1: String,
    val size: Int,
    val url: String
)

@Serializable
data class Downloads(
    val client: Client? = null
    // 省略了 client_mappings, server 等不需要的字段
)

@Serializable
data class Artifact(
    val path: String,
    val sha1: String,
    val size: Int,
    val url: String
)

@Serializable
data class DownloadsX(
    val artifact: Artifact? = null,
    val classifiers: JsonObject? = null // 用于 Native 库
)

@Serializable
data class Os(
    val name: String? = null,
    val arch: String? = null,
    val version: String? = null
)

@Serializable
data class Rule(
    val action: String,
    val features: Map<String, Boolean>? = null,
    val os: Os? = null
)

@Serializable
data class Library(
    val name: String,
    val downloads: DownloadsX? = null,
    val url: String? = null, // Forge/Fabric 的库可能直接写在外层
    val rules: List<Rule>? = null,
    val natives: Map<String, String>? = null // 用于提取 native 分类器
)

// 最外层数据模型
@Serializable
data class VersionJsonModel(
    val id: String,
    val inheritsFrom: String? = null,
    val type: String,
    val mainClass: String,
    val minecraftArguments: String? = null, // 1.12.2 及以前的旧参数格式
    val arguments: Arguments? = null,       // 1.13 及以后的新参数格式
    val assetIndex: AssetIndex? = null,
    val downloads: Downloads? = null,
    val libraries: List<Library> = emptyList()
)

// ==========================================
// 3. 核心解析与合并逻辑类
// ==========================================

class MinecraftVersionMeta(
    targetVersion: String,
    versionsDir: File
) {
    // 供外部获取合并后的数据
    val model: VersionJsonModel
    val clientJarPath: String
    val isLegacyArguments: Boolean

    // 配置 JSON 解析器，忽略未知的键以防止反序列化崩溃
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    init {
        val targetFile = File(versionsDir, "$targetVersion/$targetVersion.json")
        if (!targetFile.exists()) throw IllegalStateException("版本 JSON 找不到：${targetFile.absolutePath}")

        val targetModel = json.decodeFromString<VersionJsonModel>(targetFile.readText())

        if (targetModel.inheritsFrom != null) {
            // 需要继承原版 JSON (如 Fabric / Forge)
            val baseFile = File(versionsDir, "${targetModel.inheritsFrom}/${targetModel.inheritsFrom}.json")
            if (!baseFile.exists()) throw IllegalStateException("找不到被继承的原版 JSON：${baseFile.absolutePath}")

            val baseModel = json.decodeFromString<VersionJsonModel>(baseFile.readText())
            model = mergeModels(baseModel, targetModel)
            clientJarPath =
                File(versionsDir, "${targetModel.inheritsFrom}/${targetModel.inheritsFrom}.jar").absolutePath
        } else {
            // 纯原版
            model = targetModel
            clientJarPath = File(versionsDir, "$targetVersion/$targetVersion.jar").absolutePath
        }

        // 判断是否是老版本的参数格式
        isLegacyArguments = model.minecraftArguments != null && model.arguments == null
    }

    /**
     * 将 Mod 端的 JSON 合并到原版 JSON 上
     */
    private fun mergeModels(base: VersionJsonModel, target: VersionJsonModel): VersionJsonModel {
        // 合并 Libraries
        val mergedLibraries = mutableListOf<Library>()
        mergedLibraries.addAll(base.libraries)
        mergedLibraries.addAll(target.libraries)

        // 合并 Arguments
        val mergedGameArgs = mutableListOf<Argument>()
        val mergedJvmArgs = mutableListOf<Argument>()

        base.arguments?.let {
            mergedGameArgs.addAll(it.game)
            mergedJvmArgs.addAll(it.jvm)
        }
        target.arguments?.let {
            mergedGameArgs.addAll(it.game)
            mergedJvmArgs.addAll(it.jvm)
        }

        val mergedArguments = Arguments(
            game = mergedGameArgs,
            jvm = mergedJvmArgs
        )

        return VersionJsonModel(
            id = target.id,
            inheritsFrom = target.inheritsFrom,
            type = target.type,
            // 如果 Target(如 Fabric) 覆盖了 mainClass，使用 Target 的，否则用 Base 的
            mainClass = target.mainClass.ifBlank { base.mainClass },
            minecraftArguments = target.minecraftArguments ?: base.minecraftArguments,
            arguments = mergedArguments,
            assetIndex = target.assetIndex ?: base.assetIndex,
            downloads = target.downloads ?: base.downloads,
            libraries = mergedLibraries
        )
    }

    // --- 为了兼容你其他类的旧调用方式，提供一些便捷方法 ---

    fun getAssetIndexId(): String {
        return model.assetIndex?.id ?: throw IllegalStateException("缺失 assetIndex.id")
    }

    fun getMainClass(): String {
        return model.mainClass.ifBlank { throw IllegalStateException("缺失 mainClass") }
    }
}