package cn.echomirix.echolauncher.core.version

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

data class LocalVersion(
    val id: String,
    val type: String,
    val releaseTime: String? = null
)

object VersionManager {
    fun scanLocalVersions(minecraftDir: String): List<LocalVersion> {
        val versionDir = File(minecraftDir, "versions")
        if (!versionDir.exists() || !versionDir.isDirectory) {
            println("版本目录不存在或不是一个文件夹：${versionDir.absolutePath}")
            return emptyList()
        }
        val versions = mutableListOf<LocalVersion>()

        val subDirs = versionDir.listFiles { it.isDirectory } ?: return emptyList()

        for (dir in subDirs) {
            val jsonFile = File(dir, "${dir.name}.json")
            if (jsonFile.exists()) {
                try {
                    val jsonText = jsonFile.readText()
                    val jsonObj = Json.parseToJsonElement(jsonText).jsonObject
                    val id = jsonObj["id"]?.jsonPrimitive?.content ?: continue
                    val type = jsonObj["type"]?.jsonPrimitive?.content ?: "unknown"
                    val releaseTime = jsonObj["releaseTime"]?.jsonPrimitive?.content
                    versions.add(LocalVersion(id, type, releaseTime))
                } catch (e: Exception) {
                    println("解析版本 JSON 失败：${jsonFile.absolutePath}，错误：${e.message}")
                }
            }
        }

        return versions.sortedByDescending { it.id }
    }
}