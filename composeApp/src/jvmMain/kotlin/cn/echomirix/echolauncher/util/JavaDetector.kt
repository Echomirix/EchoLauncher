package cn.echomirix.echolauncher.util

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class JavaInfo(
    val path: String,       // java.exe 的绝对路径
    val version: String,    // 例如 "1.8.0_311", "17.0.2", "21"
    val is64Bit: Boolean    // 是否为 64 位
) {
    // 提取主版本号，方便游戏按需选择 (比如 MC 1.17+ 需要 Java 16/17)
    val majorVersion: Int
        get() {
            val v = version.split(".", "_", "-")
            return if (v.first() == "1") {
                v.getOrNull(1)?.toIntOrNull() ?: 8 // 1.8 -> 8
            } else {
                v.first().toIntOrNull() ?: 0       // 17 -> 17
            }
        }
}

object JavaDetector {


    private val logger = KotlinLogging.logger {}

    /**
     * 核心暴露方法：扫描本机所有有效的 Java 路径
     */
    suspend fun scanLocalJava(): List<JavaInfo> = withContext(Dispatchers.IO) {
        val possiblePaths = mutableSetOf<File>()

        // 1. 获取当前程序正在使用的 Java
        val currentJavaHome = System.getProperty("java.home")
        if (currentJavaHome != null) possiblePaths.add(File(currentJavaHome))

        // 2. 获取环境变量 JAVA_HOME
        val envJavaHome = System.getenv("JAVA_HOME")
        if (envJavaHome != null) possiblePaths.add(File(envJavaHome))

        // 3. 解析 PATH 环境变量中带有 java 的路径
        System.getenv("PATH")?.split(File.pathSeparator)?.forEach { pathStr ->
            val file = File(pathStr)
            if (file.exists() && (file.name.equals("bin", ignoreCase = true) || file.list()
                    ?.contains(getJavaExecutableName()) == true)
            ) {
                // 如果是 bin 目录，我们把它的父目录当做 Java Home
                if (file.name.equals("bin", ignoreCase = true)) {
                    possiblePaths.add(file.parentFile)
                } else {
                    possiblePaths.add(file)
                }
            }
        }

        // 4. 扫描各个操作系统的常见安装位置
        possiblePaths.addAll(scanCommonDirectories())

        // 5. 将所有可能是 JavaHome 的目录，转换成它下面的 bin/java.exe 路径，并过滤存在的
        val exeFiles = possiblePaths.map { File(it, "bin/${getJavaExecutableName()}") }
            .filter { it.exists() && it.isFile }
            .distinctBy { it.absolutePath }

        // 6. 开启并发：对每一个 java.exe 执行 `java -version` 获取精确信息
        val deferredResults = exeFiles.map { javaExe ->
            async {
                parseJavaVersion(javaExe.absolutePath)
            }
        }

        return@withContext deferredResults.awaitAll()
            .filterNotNull()
            .sortedByDescending { it.majorVersion } // 按版本从高到低排序
    }

    private fun getJavaExecutableName(): String {
        return if (System.getProperty("os.name").lowercase().contains("win")) "java.exe" else "java"
    }

    private fun scanCommonDirectories(): List<File> {
        val dirs = mutableListOf<File>()
        val os = System.getProperty("os.name").lowercase()

        if (os.contains("win")) {
            // Windows 下的常见 JDK 安装器路径
            val rootPaths = listOf(
                "C:\\Program Files\\Java",
                "C:\\Program Files (x86)\\Java",
                "C:\\Program Files\\Eclipse Adoptium",
                "C:\\Program Files\\AdoptOpenJDK",
                "C:\\Program Files\\Amazon Corretto",
                "C:\\Program Files\\Zulu"
            )
            for (root in rootPaths) {
                val rootDir = File(root)
                if (rootDir.exists() && rootDir.isDirectory) {
                    rootDir.listFiles()?.filter { it.isDirectory }?.let { dirs.addAll(it) }
                }
            }
        } else if (os.contains("mac")) {
            val rootDir = File("/Library/Java/JavaVirtualMachines")
            if (rootDir.exists()) {
                rootDir.listFiles()?.filter { it.isDirectory }?.forEach { jvmDir ->
                    // Mac 的 Home 通常在 Contents/Home 里面
                    dirs.add(File(jvmDir, "Contents/Home"))
                }
            }
        } else {
            // Linux
            val rootPaths = listOf("/usr/lib/jvm", "/usr/java")
            for (root in rootPaths) {
                val rootDir = File(root)
                if (rootDir.exists()) {
                    rootDir.listFiles()?.filter { it.isDirectory }?.let { dirs.addAll(it) }
                }
            }
        }
        return dirs
    }

    /**
     * 调用 java.exe -version，解析返回的字符串
     */
    private fun parseJavaVersion(javaPath: String): JavaInfo? {
        try {
            val process = ProcessBuilder(javaPath, "-version")
                .redirectErrorStream(true) // java -version 通常输出在 Error 里面
                .start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            // 解析输出文本
            // 典型输出 1: openjdk version "17.0.2" 2022-01-18
            // 典型输出 2: java version "1.8.0_311"
            val versionRegex = "\"(.*?)\"".toRegex()
            val matchResult = versionRegex.find(output)

            if (matchResult != null) {
                val versionStr = matchResult.groups[1]?.value ?: return null
                val is64Bit = output.contains("64-Bit", ignoreCase = true)
                return JavaInfo(
                    path = javaPath,
                    version = versionStr,
                    is64Bit = is64Bit
                )
            }
        } catch (e: Exception) {
            logger.error { "测试 Java 路径失败: $javaPath -> ${e.message}" }
        }
        return null
    }
}