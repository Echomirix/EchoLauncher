package cn.echomirix.echolauncher.core

import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.io.File
import java.net.URL
import java.security.MessageDigest

class MinecraftDependencyVerifier(
    private val versionJsonString: String,
    private val librariesDirectory: String,
    private val assetsDirectory: String,
    private val gameJarPath: String,
    private val currentOsName: String = getCurrentOsName(),
    private val currentOsArch: String = getCurrentOsArch()
) {
    private val rootElement = Json.parseToJsonElement(versionJsonString).jsonObject

    /**
     * 一键校验并下载所有缺失依赖！
     */
    suspend fun verifyAndDownloadAll() = coroutineScope {
        println("[校验] 开始地狱级的文件审查...")

        // 1. 校验游戏本体 (Client Jar)
        val clientDownload = rootElement["downloads"]?.jsonObject?.get("client")?.jsonObject
        val clientUrl = clientDownload?.get("url")?.jsonPrimitive?.content
        val clientSha1 = clientDownload?.get("sha1")?.jsonPrimitive?.content
        if (clientUrl != null && clientSha1 != null) {
            verifyOrDownloadFile(File(gameJarPath), clientUrl, clientSha1, "游戏本体")
        }

        // 2. 校验依赖库 (Libraries) - 开启疯狂并发模式！
        val librariesArray = rootElement["libraries"]?.jsonArray
        if (librariesArray != null) {
            val libJobs = mutableListOf<Deferred<Unit>>()
            for (libElement in librariesArray) {
                val libObj = libElement.jsonObject

                // 必须过规则！不需要的库我们坚决不下载！
                if (!checkLibraryRules(libObj["rules"]?.jsonArray)) continue

                val artifactObj = libObj["downloads"]?.jsonObject?.get("artifact")?.jsonObject ?: continue
                val pathStr = artifactObj["path"]?.jsonPrimitive?.content ?: continue
                val urlStr = artifactObj["url"]?.jsonPrimitive?.content ?: continue
                val sha1Str = artifactObj["sha1"]?.jsonPrimitive?.content ?: continue

                val targetFile = File(librariesDirectory, pathStr)

                // 协程大军出击！并发下载！
                libJobs.add(async(Dispatchers.IO) {
                    verifyOrDownloadFile(targetFile, urlStr, sha1Str, "依赖库: $pathStr")
                })
            }
            // 等待所有库下载完成
            libJobs.awaitAll()
        }

        // 3. 校验资产索引 (Asset Index)
        val assetIndexObj = rootElement["assetIndex"]?.jsonObject
        val assetIndexId = assetIndexObj?.get("id")?.jsonPrimitive?.content
        val assetIndexUrl = assetIndexObj?.get("url")?.jsonPrimitive?.content
        val assetIndexSha1 = assetIndexObj?.get("sha1")?.jsonPrimitive?.content

        if (assetIndexId != null && assetIndexUrl != null && assetIndexSha1 != null) {
            val indexFile = File(assetsDirectory, "indexes/$assetIndexId.json")
            verifyOrDownloadFile(indexFile, assetIndexUrl, assetIndexSha1, "资产索引: $assetIndexId")

            // 4. 根据索引文件，去下载成百上千个音频和贴图！
            verifyAndDownloadAssetsObjects(indexFile)
        }

        println("[校验] 所有依赖准备就绪！允许放行！")
    }

    /**
     * 解析 Asset Index JSON 并疯狂并发下载几千个小文件
     */
    private suspend fun verifyAndDownloadAssetsObjects(indexFile: File) = coroutineScope {
        if (!indexFile.exists()) return@coroutineScope
        val indexJson = Json.parseToJsonElement(indexFile.readText()).jsonObject
        val objectsObj = indexJson["objects"]?.jsonObject ?: return@coroutineScope

        val assetJobs = mutableListOf<Deferred<Unit>>()
        val objectsDir = File(assetsDirectory, "objects")

        for ((_, value) in objectsObj) {
            val hash = value.jsonObject["hash"]?.jsonPrimitive?.content ?: continue
            val subDir = hash.substring(0, 2) // Mojang 的套路：取哈希值前两位做文件夹
            val targetFile = File(objectsDir, "$subDir/$hash")

            // 拼接官方资源下载地址
            val url = "https://resources.download.minecraft.net/$subDir/$hash"

            assetJobs.add(async(Dispatchers.IO) {
                verifyOrDownloadFile(targetFile, url, hash, "资产文件: $hash")
            })
        }
        println("[校验] 正在审查 ${assetJobs.size} 个资产文件...")
        assetJobs.awaitAll()
    }

    /**
     * 核心校验与下载逻辑
     */
    private fun verifyOrDownloadFile(file: File, url: String, expectedSha1: String, logName: String) {
        if (file.exists()) {
            val actualSha1 = calculateSha1(file)
            if (actualSha1.equals(expectedSha1, ignoreCase = true)) {
                // 文件完好无损
                return
            } else {
                println("[校验失败] $logName 文件损坏或被篡改，准备重新下载...")
            }
        } else {
            println("[发现缺失] $logName 不存在，准备下载...")
        }

        // 文件不存在或哈希不匹配，开始下载
        downloadFile(url, file)

        // 下载完再查一次岗！
        val newSha1 = calculateSha1(file)
        if (!newSha1.equals(expectedSha1, ignoreCase = true)) {
            throw IllegalStateException("你家网络有毒吧？下载完的 $logName 哈希值依然对不上！")
        }
    }

    private fun downloadFile(urlStr: String, targetFile: File) {
        try {
            targetFile.parentFile?.mkdirs()
            URL(urlStr).openStream().use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            println("[下载成功] -> ${targetFile.name}")
        } catch (e: Exception) {
            println("[下载失败] $urlStr -> ${e.message}")
            targetFile.delete() // 下载失败必须删掉残缺文件！
            throw e
        }
    }

    private fun calculateSha1(file: File): String {
        val digest = MessageDigest.getInstance("SHA-1")
        file.inputStream().use { fis ->
            val buffer = ByteArray(8192)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun checkLibraryRules(rulesArray: JsonArray?): Boolean {
        if (rulesArray == null || rulesArray.isEmpty()) return true
        var isAllowed = false
        for (ruleElement in rulesArray) {
            val rule = ruleElement.jsonObject
            val action = rule["action"]?.jsonPrimitive?.content ?: continue

            var isRuleMatched = true
            if (rule.containsKey("os")) {
                val osRule = rule["os"]!!.jsonObject
                val osNameRule = osRule["name"]?.jsonPrimitive?.content
                val osArchRule = osRule["arch"]?.jsonPrimitive?.content
                if (osNameRule != null && osNameRule != currentOsName) isRuleMatched = false
                if (osArchRule != null && osArchRule != currentOsArch) isRuleMatched = false
            }

            if (action == "allow" && isRuleMatched) isAllowed = true
            else if (action == "disallow" && isRuleMatched) isAllowed = false
        }
        return isAllowed
    }
}