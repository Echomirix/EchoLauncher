package cn.echomirix.echolauncher.core

import kotlinx.serialization.json.*
import java.io.File

class MinecraftVersionMeta(
    targetVersion: String,
    versionsDir: File
) {
    private val targetObj: JsonObject
    private val baseObj: JsonObject?

    init {
        val targetFile = File(versionsDir, "$targetVersion/$targetVersion.json")
        if (!targetFile.exists()) throw IllegalStateException("连版本 JSON 都找不到：${targetFile.absolutePath}")
        targetObj = Json.parseToJsonElement(targetFile.readText()).jsonObject

        val inherits = targetObj["inheritsFrom"]?.jsonPrimitive?.content
        if (inherits != null) {
            val baseFile = File(versionsDir, "$inherits/$inherits.json")
            if (!baseFile.exists()) throw IllegalStateException("找不到被继承的原版 JSON：${baseFile.absolutePath}")
            baseObj = Json.parseToJsonElement(baseFile.readText()).jsonObject
        } else {
            baseObj = null
        }
    }

    // 核心1：推导真实的游戏本体 Jar 路径！(Fabric 没有 jar，必须用原版的 jar)
    val baseVersion: String = targetObj["inheritsFrom"]?.jsonPrimitive?.content ?: targetVersion
    val clientJarPath: String = File(versionsDir, "$baseVersion/$baseVersion.jar").absolutePath

    // 核心2：无缝合并 Libraries (原版的库 + Mod端的特有库)
    val libraries: JsonArray = buildJsonArray {
        baseObj?.get("libraries")?.jsonArray?.forEach { add(it) }
        targetObj["libraries"]?.jsonArray?.forEach { add(it) }
    }

    // 核心3：合并 JVM 参数
    val jvmArgs: JsonArray = buildJsonArray {
        baseObj?.get("arguments")?.jsonObject?.get("jvm")?.jsonArray?.forEach { add(it) }
        targetObj["arguments"]?.jsonObject?.get("jvm")?.jsonArray?.forEach { add(it) }
    }

    // 核心4：合并 Game 参数
    val gameArgs: JsonArray = buildJsonArray {
        baseObj?.get("arguments")?.jsonObject?.get("game")?.jsonArray?.forEach { add(it) }
        targetObj["arguments"]?.jsonObject?.get("game")?.jsonArray?.forEach { add(it) }
    }

    val minecraftArguments: String? = targetObj["minecraftArguments"]?.jsonPrimitive?.content
        ?: baseObj?.get("minecraftArguments")?.jsonPrimitive?.content

    // 核心5：提取 MainClass (如果 Mod 端覆盖了，必须用 Mod 端的！比如 KnotClient)
    val mainClass: String = targetObj["mainClass"]?.jsonPrimitive?.content
        ?: baseObj?.get("mainClass")?.jsonPrimitive?.content
        ?: throw IllegalStateException("连 MainClass 都没有，启动个寂寞！")

    // 核心6：提取 Asset Index
    val assetIndex: JsonObject = targetObj["assetIndex"]?.jsonObject
        ?: baseObj?.get("assetIndex")?.jsonObject
        ?: throw IllegalStateException("找不到 assetIndex，贴图全废！")

    // 核心7：提取 Client 本体下载链接
    val clientDownload: JsonObject? = targetObj["downloads"]?.jsonObject?.get("client")?.jsonObject
        ?: baseObj?.get("downloads")?.jsonObject?.get("client")?.jsonObject
}