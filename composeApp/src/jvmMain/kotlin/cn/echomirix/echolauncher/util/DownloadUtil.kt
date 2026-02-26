package cn.echomirix.echolauncher.util

import java.io.File
import java.net.URL
import java.security.MessageDigest

fun downloadFile(urlStr: String, targetFile: File) {
    try {
        targetFile.parentFile?.mkdirs()
        URL(urlStr).openStream().use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    } catch (e: Exception) {
        targetFile.delete()
        throw e
    }
}

fun calculateSha1(file: File): String = digest(file, "SHA-1")
fun calculateSha256(file: File): String = digest(file, "SHA-256")
fun calculateSha512(file: File): String = digest(file, "SHA-512")
fun calculateMd5(file: File): String = digest(file, "MD5")

private fun digest(file: File, algo: String): String {
    val digest = MessageDigest.getInstance(algo)
    file.inputStream().use { fis ->
        val buffer = ByteArray(8192)
        var read: Int
        while (fis.read(buffer).also { read = it } != -1) {
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}