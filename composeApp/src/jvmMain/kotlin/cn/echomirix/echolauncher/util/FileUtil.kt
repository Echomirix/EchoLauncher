package cn.echomirix.echolauncher.util

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun parseLibraryPath(libObj: kotlinx.serialization.json.JsonObject): String? {
    // 1. 优先认 Mojang 官方的标准路径
    val downloadPath = libObj["downloads"]?.jsonObject?.get("artifact")?.jsonObject?.get("path")?.jsonPrimitive?.content
    if (downloadPath != null) return downloadPath

    // 2. 第三方加载器（Fabric/Forge）耍流氓，只给 name (Maven坐标)
    val nameStr = libObj["name"]?.jsonPrimitive?.content ?: return null

    // Maven 坐标格式: groupId:artifactId:version[:classifier]
    val parts = nameStr.split(":")
    if (parts.size >= 3) {
        val groupId = parts[0].replace('.', '/')
        val artifactId = parts[1]
        val version = parts[2]
        // 应对恶心的 classifier (比如 native-windows)
        val classifier = if (parts.size >= 4) "-${parts[3]}" else ""

        // 算出绝对标准路径，例如: net/fabricmc/fabric-loader/0.18.4/fabric-loader-0.18.4.jar
        return "$groupId/$artifactId/$version/$artifactId-$version$classifier.jar"
    }
    return null
}