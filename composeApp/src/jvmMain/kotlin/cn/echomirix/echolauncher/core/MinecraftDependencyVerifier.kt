package cn.echomirix.echolauncher.core

import cn.echomirix.echolauncher.util.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.zip.ZipFile


/**
 * 表示一个下载项的信息，包括文件路径、下载链接以及可选的校验和信息。
 *
 * @property path 文件将被保存的本地路径。
 * @property url 文件的下载链接。
 * @property sha1 可选的 SHA-1 校验和，用于验证文件完整性。
 * @property sha256 可选的 SHA-256 校验和，提供比 SHA-1 更强的安全性来验证文件。
 * @property sha512 可选的 SHA-512 校验和，提供最高级别的安全性来验证文件。
 * @property md5 可选的 MD5 校验和，尽管不如 SHA 系列安全，但依然可用于基本的文件完整性检查。
 * @property size 可选的文件大小（以字节为单位），可以用来预先估计下载时间和存储空间需求。
 */
private data class DownloadInfo(
    val path: String,
    val url: String,
    val sha1: String? = null,
    val sha256: String? = null,
    val sha512: String? = null,
    val md5: String? = null,
    val size: Long? = null
)


/**
 * 用于验证和下载 Minecraft 游戏运行所需的所有文件，包括游戏本体、依赖库、资产索引以及对应的音频和贴图等资源。
 *
 * 该类通过异步方式处理文件的校验与下载，确保所有必要的文件都已正确下载并存在于指定目录中。
 *
 * @param versionMeta 版本元数据，包含有关当前 Minecraft 版本的信息。
 * @param librariesDirectory 依赖库文件存储目录。
 * @param assetsDirectory 资源文件存储目录。
 * @param nativesDirectory Native 库文件存储目录。
 * @param currentOsName 当前操作系统名称，默认使用 [getCurrentOsName] 方法获取。
 * @param currentOsArch 当前操作系统架构，默认使用 [getCurrentOsArch] 方法获取。
 */
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

    /**
     * 检查并下载Minecraft运行所需的所有文件，包括游戏本体、依赖库、资产索引及相关的资源文件。
     */
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

    /**
     * 从给定的[Library]对象中提取下载信息。
     *
     * @param libObj 代表一个库的对象，从中提取下载路径和URL等信息。
     * @return 返回一个包含下载路径、URL以及可选的SHA-1校验和和文件大小的[DownloadInfo]对象。如果无法解析有效的下载信息，则返回null。
     */
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

    /**
     * 验证给定文件的完整性，包括检查文件的哈希值和大小。
     *
     * @param file 要验证的文件对象。
     * @throws IllegalStateException 如果文件的SHA-1、SHA-256、SHA-512或MD5校验和与预期不符，或者文件的实际大小与预期大小不同，则抛出此异常。
     */
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
     * 从指定的JAR文件中提取所有非目录且不以'META-INF/'开头的条目，并将它们保存到目标目录。
     *
     * @param jarFile 要从中提取本地库的JAR文件。
     * @param destDir 提取出来的文件将被放置的目标目录。
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
     * 校验并下载资产索引文件中列出的所有对象。
     *
     * 该方法首先检查提供的索引文件是否存在。如果存在，它将解析JSON格式的索引文件以获取所有对象信息。对于每个对象，通过其哈希值构建目标文件路径，并尝试从指定URL下载或验证已存在的文件。使用协程异步执行每个下载/验证任务，并在所有任务完成后统计失败数量。如果有任何文件下载失败，则抛出异常。
     *
     * @param indexFile 资产索引文件，包含需要下载或验证的对象信息。
     */
    private suspend fun verifyAndDownloadAssetsObjects(indexFile: File) = coroutineScope {
        if (!indexFile.exists()) return@coroutineScope
        val indexJson = Json.parseToJsonElement(indexFile.readText()).jsonObject
        val objectsObj = indexJson["objects"]?.jsonObject ?: return@coroutineScope
        val assetJobs = mutableListOf<Deferred<Result<Unit>>>()
        val objectsDir = File(assetsDirectory, "objects")

        for ((_, value) in objectsObj) {
            val hash = value.jsonObject["hash"]?.jsonPrimitive?.content ?: continue
            val subDir = hash.take(2)
            val targetFile = File(objectsDir, "$subDir/$hash")

            val url = "https://resources.download.minecraft.net/$subDir/$hash"
            assetJobs.add(async(downloadDispatcher) {
                runCatching {
                    retryIO(3) {
                        verifyOrDownloadFile(targetFile, url, hash, "资产文件: $hash")
                    }
                }
            })
        }
        logger.info { "校验: 正在审查 ${assetJobs.size} 个资产文件..." }
        val results = assetJobs.awaitAll()
        val failedCount = results.count { it.isFailure }
        if (failedCount > 0) {
            throw IllegalStateException("有 $failedCount 个资产文件下载失败，请检查网络！")
        }
    }

    /**
     * 检查指定文件是否存在且其SHA-1哈希值与预期相符，如果文件不存在或校验失败则从给定URL下载。
     *
     * @param file 要验证或下载的目标文件。
     * @param url 如果需要下载文件时使用的URL地址。
     * @param expectedSha1 预期的文件SHA-1哈希值，用于校验文件完整性。
     * @param logName 用于日志记录中的文件名标识。
     */
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



    /**
     * 检查库规则是否允许当前操作系统。
     *
     * @param rulesArray 规则列表，包含操作系统的限制条件。
     * @return 如果规则允许当前环境，则返回true；否则返回false。
     */
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