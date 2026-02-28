package cn.echomirix.echolauncher.util

import cn.echomirix.echolauncher.core.Library
import java.io.File

/**
 * 解析库文件路径
 *
 * @param libObj 库对象
 * @return 库文件路径，未找到时返回 null
 */
fun parseLibraryPath(libObj: Library): String? {
    // 1. 优先认 Mojang 官方的标准路径
    val downloadPath = libObj.downloads?.artifact?.path
    if (downloadPath != null) return downloadPath

    val nameStr = libObj.name

    val parts = nameStr.split(":")
    if (parts.size >= 3) {
        val groupId = parts[0].replace('.', '/')
        val artifactId = parts[1]
        val version = parts[2]
        val classifier = if (parts.size >= 4) "-${parts[3]}" else ""

        return "$groupId/$artifactId/$version/$artifactId-$version$classifier.jar"
    }
    return null
}

/**
 * 从日志文件中提取崩溃描述。
 * @param logFile 日志文件
 * @return 崩溃描述或null
 */
fun extractCrashDescription(logFile: File?): String? {
    if (logFile == null || !logFile.exists()) return null

    try {
        // 如果是原版的 crash-report，前 20 行内一定有 Description:
        if (logFile.name.startsWith("crash-")) {
            logFile.useLines { lines ->
                for (line in lines) {
                    if (line.startsWith("Description: ")) {
                        return line.substringAfter("Description: ").trim()
                    }
                }
            }
        }
        // 如果没找到，或者是 latest.log，可以尝试提取第一个 Exception
        else if (logFile.name == "latest.log") {
            val circularBuffer = ArrayDeque<String>(100)
            logFile.useLines { lines ->
                for (line in lines) {
                    if (circularBuffer.size == 100) {
                        circularBuffer.removeFirst() // 挤出最老的一行
                    }
                    circularBuffer.addLast(line)
                }
            }

            // 倒序查找（越靠后的异常越可能是崩溃原因）
            val exceptionLine = circularBuffer.findLast { it.contains("Exception") || it.contains("Error") }
            if (exceptionLine != null) {
                return exceptionLine.substringAfter("]: ").trim()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return null
}

/**
 * 查找最新的崩溃日志文件。
 * @param gameDir 游戏目录
 * @return 最新的崩溃日志文件，如果没有找到则返回null
 */
fun findLatestCrashLog(gameDir: File): File? {
    val crashDir = File(gameDir, "crash-reports")
    if (crashDir.exists() && crashDir.isDirectory) {
        val latestCrash = crashDir.listFiles { file ->
            file.name.startsWith("crash-") && file.name.endsWith(".txt")
        }?.maxByOrNull { it.lastModified() }

        // 5分钟内生成的视为本次崩溃日志
        if (latestCrash != null && (System.currentTimeMillis() - latestCrash.lastModified() < 5 * 60 * 1000)) {
            return latestCrash
        }
    }

    val latestLog = File(gameDir, "logs/latest.log")
    if (latestLog.exists()) {
        return latestLog
    }
    return null
}

/**
 * 重试执行给定的挂起函数
 * @param times 重试次数，默认为3
 * @param block 需要执行的挂起函数
 * @return 返回block执行的结果
 */
suspend fun <T> retryIO(times: Int = 3, block: suspend () -> T): T {
    var currentException: Exception? = null
    for (i in 0 until times) {
        try {
            return block()
        } catch (e: Exception) {
            currentException = e
            kotlinx.coroutines.delay(1000L * (i + 1)) // 指数退避：1秒, 2秒, 3秒
        }
    }
    throw currentException ?: RuntimeException("重试失败")
}
