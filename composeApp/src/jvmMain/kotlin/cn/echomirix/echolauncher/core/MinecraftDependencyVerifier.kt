package cn.echomirix.echolauncher.core

import cn.echomirix.echolauncher.util.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.zip.ZipFile


private data class DownloadInfo(
    val path: String,
    val url: String,
    val sha1: String? = null,
    val sha256: String? = null,
    val sha512: String? = null,
    val md5: String? = null,
    val size: Long? = null
)


class MinecraftDependencyVerifier(
    private val versionMeta: MinecraftVersionMeta,
    private val librariesDirectory: String,
    private val assetsDirectory: String,
    private val nativesDirectory: String,
    private val currentOsName: String = getCurrentOsName(),
    private val currentOsArch: String = getCurrentOsArch()
) {


    private val logger = KotlinLogging.logger {}
    private val downloadDispatcher = Dispatchers.IO.limitedParallelism(16)

    suspend fun verifyAndDownloadAll() = coroutineScope {
        logger.info { "开始文件审查..." }

        // 1. 校验游戏本体 (Client Jar)
        val clientUrl = versionMeta.model.downloads?.client?.url
        val clientSha1 = versionMeta.model.downloads?.client?.sha1
        if (clientUrl != null && clientSha1 != null) {
            verifyOrDownloadFile(File(versionMeta.clientJarPath), clientUrl, clientSha1, "游戏本体")
        }

        // 确保 Natives 文件夹存在
        val nativesDirFile = File(nativesDirectory)
        if (!nativesDirFile.exists()) nativesDirFile.mkdirs()

        // 2. 校验依赖库 (Libraries) - 开启并发模式
        val libJobs = mutableListOf<Deferred<Unit>>()
        for (libElement in versionMeta.model.libraries) {

            if (!checkLibraryRules(libElement.rules)) continue
            val nativesObj = libElement.natives

            if (nativesObj == null) {
                val info = extractDownloadInfo(libElement) ?: continue
                val targetFile = File(librariesDirectory, info.path)
                targetFile.parentFile?.mkdirs()

                if (!targetFile.exists()) {
                    logger.info { "下载 ${info.url}" }
                    libJobs.add(async(downloadDispatcher) {
                        downloadFile(info.url, targetFile)
                        info.verify(targetFile)
                    })
                }
            }


            // 【线路B】：挖掘并下载 Native 库

            // 【线路B】：挖掘并下载 Native 库
            if (nativesObj != null) {
                val rawClassifier = nativesObj[currentOsName]
                if (rawClassifier != null) {
                    val classifierKey = rawClassifier.replace($$"${arch}", if (currentOsArch == "x86") "32" else "64")
                    val classifierObj =
                        libElement.downloads?.classifiers?.get(classifierKey)?.jsonObject

                    if (classifierObj != null) {
                        val pathStr = classifierObj["path"]?.jsonPrimitive?.content
                        val urlStr = classifierObj["url"]?.jsonPrimitive?.content
                        val sha1Str = classifierObj["sha1"]?.jsonPrimitive?.content

                        if (pathStr != null && urlStr != null && sha1Str != null) {
                            val targetFile = File(librariesDirectory, pathStr)

                            libJobs.add(async(downloadDispatcher) {
                                // 记录下载前的状态
                                val existedBefore =
                                    targetFile.exists() && calculateSha1(targetFile).equals(sha1Str, ignoreCase = true)

                                verifyOrDownloadFile(targetFile, urlStr, sha1Str, "Native库: $pathStr")

                                // 核心修复：如果文件以前不存在(或损坏被重新下载了)，才去执行暴力解压！
                                // 或者 Natives 目录为空，也要强制解压一次（防止玩家手欠删了 natives 文件夹）
                                val needsExtract = !existedBefore || (nativesDirFile.listFiles()?.isEmpty() != false)

                                if (needsExtract) {
                                    logger.info { "正在释放 Native 库: ${targetFile.name}" }
                                    extractNatives(targetFile, nativesDirFile)
                                }
                            })
                        }
                    }
                }
            }
        }
        // 等待所有库下载并解压完成
        libJobs.awaitAll()

        // 3. 校验资产索引 (Asset Index)
        val assetIndexId = versionMeta.getAssetIndexId()
        val assetIndexUrl = versionMeta.model.assetIndex?.url
        val assetIndexSha1 = versionMeta.model.assetIndex?.sha1

        if (assetIndexUrl != null && assetIndexSha1 != null) {
            val indexFile = File(assetsDirectory, "indexes/$assetIndexId.json")
            verifyOrDownloadFile(indexFile, assetIndexUrl, assetIndexSha1, "资产索引: $assetIndexId")

            // 4. 根据索引文件，去下载成百上千个音频和贴图！
            verifyAndDownloadAssetsObjects(indexFile)
        }

        logger.info { "校验: 所有依赖准备就绪！允许放行！" }
    }

    private fun extractDownloadInfo(libObj: Library): DownloadInfo? {
        val artifactObj = libObj.downloads?.artifact
        val pathFromArtifact = artifactObj?.path
        val urlFromArtifact = artifactObj?.url

        // 先试标准 artifact
        if (pathFromArtifact != null && urlFromArtifact != null) {
            return DownloadInfo(
                path = pathFromArtifact,
                url = urlFromArtifact,
                sha1 = artifactObj.sha1,
                size = artifactObj.size.toLong()
            )
        }

        // 兜底：Fabric/Forge 自带 Maven 坐标 + url
        val path = parseLibraryPath(libObj) ?: return null
        val base = libObj.url ?: "https://libraries.minecraft.net/"
        val url = if (base.endsWith("/")) "$base$path" else "$base/$path"
        return DownloadInfo(
            path = path,
            url = url,
        )
    }

    private fun DownloadInfo.verify(file: File) {
        fun check(expected: String?, actual: () -> String): Boolean {
            if (expected.isNullOrBlank()) return true
            return expected.equals(actual(), ignoreCase = true)
        }
        if (!check(sha1) { calculateSha1(file) }) throw IllegalStateException("哈希校验失败: $path (sha1)")
        if (!check(sha256) { calculateSha256(file) }) throw IllegalStateException("哈希校验失败: $path (sha256)")
        if (!check(sha512) { calculateSha512(file) }) throw IllegalStateException("哈希校验失败: $path (sha512)")
        if (!check(md5) { calculateMd5(file) }) throw IllegalStateException("哈希校验失败: $path (md5)")
        size?.let { if (file.length() != it) throw IllegalStateException("文件尺寸不符: $path") }
    }


    /**
     * 暴力解压 Native 文件
     */
    private fun extractNatives(jarFile: File, destDir: File) {
        if (!jarFile.exists()) return
        try {
            ZipFile(jarFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    if (!entry.isDirectory && !entry.name.startsWith("META-INF/")) {
                        val outFile = File(destDir, entry.name)
                        outFile.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            outFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error { "Native解压失败! ${jarFile.name} -> ${e.message}" }
        }
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
            val subDir = hash.substring(0, 2)
            val targetFile = File(objectsDir, "$subDir/$hash")

            val url = "https://resources.download.minecraft.net/$subDir/$hash"

            assetJobs.add(async(Dispatchers.IO) {
                verifyOrDownloadFile(targetFile, url, hash, "资产文件: $hash")
            })
        }
        logger.info { "校验: 正在审查 ${assetJobs.size} 个资产文件..." }
        assetJobs.awaitAll()
    }

    private fun verifyOrDownloadFile(file: File, url: String, expectedSha1: String, logName: String) {
        if (file.exists()) {
            val actualSha1 = calculateSha1(file)
            if (actualSha1.equals(expectedSha1, ignoreCase = true)) {
                return
            } else {
                logger.warn { "校验失败! $logName 文件损坏或被篡改，准备重新下载..." }
            }
        } else {
            logger.info { "发现缺失! $logName 不存在，准备下载..." }
        }

        downloadFile(url, file)

        val newSha1 = calculateSha1(file)
        if (!newSha1.equals(expectedSha1, ignoreCase = true)) {
            throw IllegalStateException("下载完的 $logName 哈希值依然对不上！")
        }
    }


    private fun checkLibraryRules(rulesArray: List<Rule>?): Boolean {
        if (rulesArray == null || rulesArray.isEmpty()) return true
        var isAllowed = false
        for (ruleElement in rulesArray) {
            val action = ruleElement.action

            var isRuleMatched = true
            if (ruleElement.os != null) {
                val osRule = ruleElement.os
                val osNameRule = osRule.name
                val osArchRule = osRule.arch
                if (osNameRule != null && osNameRule != currentOsName) isRuleMatched = false
                if (osArchRule != null && osArchRule != currentOsArch) isRuleMatched = false
            }

            if (action == "allow" && isRuleMatched) isAllowed = true
            else if (action == "disallow" && isRuleMatched) isAllowed = false
        }
        return isAllowed
    }
}