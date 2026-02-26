package cn.echomirix.echolauncher.util

import cn.echomirix.echolauncher.core.Library

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