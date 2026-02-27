package cn.echomirix.echolauncher.util

import cn.echomirix.echolauncher.core.Library
import java.io.File

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
            // 读取最后 100 行来寻找 Exception
            // 这是一个简单的兜底逻辑，不一定百分百准确
            val lines = logFile.readLines().takeLast(100)
            val exceptionLine = lines.find { it.contains("Exception") || it.contains("Error") }
            if (exceptionLine != null) {
                return exceptionLine.substringAfter("]: ").trim() // 去掉前面的时间戳 [18:52:34] [main/ERROR]:
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return null
}

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