package cn.echomirix.echolauncher.core.version

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

data class LocalVersion(
    val id: String,
    val type: String,
    val releaseTime: String? = null
)

object LocalVersionManager {
        suspend fun scanLocalVersions(minecraftDir: String): List<LocalVersion> = withContext(Dispatchers.IO) {
                val versionDir = File(minecraftDir, "versions")
                if (!versionDir.exists() || !versionDir.isDirectory) {
                        println("版本目录不存在或不是一个文件夹：${versionDir.absolutePath}")
                        return@withContext emptyList()
                }

                val subDirs = versionDir.listFiles { it.isDirectory } ?: return@withContext emptyList()

                val deferredVersions = subDirs.map { dir ->
                        async {
                                val jsonFile = File(dir, "${dir.name}.json")
                                if (jsonFile.exists()) {
                                        try {
                                                val jsonText = jsonFile.readText() // IO操作
                                                val jsonObj = Json.parseToJsonElement(jsonText).jsonObject
                                                val id = jsonObj["id"]?.jsonPrimitive?.content ?: return@async null
                                                val type = jsonObj["type"]?.jsonPrimitive?.content ?: "unknown"
                                                val releaseTime = jsonObj["releaseTime"]?.jsonPrimitive?.content

                                                LocalVersion(id, type, releaseTime)
                                        } catch (e: Exception) {
                                                println("解析版本 JSON 失败：${jsonFile.absolutePath}，错误：${e.message}")
                                                null
                                        }
                                } else null
                        }
                }

                return@withContext deferredVersions.awaitAll().filterNotNull().sortedByDescending { it.id }
        }
}