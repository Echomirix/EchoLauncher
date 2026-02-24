package cn.echomirix.echolauncher.core

import cn.echomirix.echolauncher.core.config.AppConfig
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

fun StartGame() {


    val ctx = LaunchContext(
        authPlayerName = "Echomirix",
        authUuid = "123e4567-e89b-12d3-a456-426614174000",
        authAccessToken = "0",
        version = "1.20.1",
        minecraftDir = "D:/Project/Java/EchoLauncher/.minecraft",
    )

    val jsonFile = File("D:/Project/Java/EchoLauncher/.minecraft/versions/1.20.1/1.20.1.json")
    val versionJsonString = jsonFile.readText()

    // 重点 1：在组装参数之前，给我把天堑守住了！不补齐文件不准放行！
    println("正在开启全网疯狂扫描与下载补齐模式...")
    runBlocking {
        val verifier = MinecraftDependencyVerifier(
            versionJsonString = versionJsonString,
            librariesDirectory = ctx.librariesDirectory,
            assetsDirectory = ctx.assetsRoot,
            gameJarPath = ctx.gameJarPath
        )
        verifier.verifyAndDownloadAll() // 这个方法会卡住主线程直到几千个文件下完！
    }
    println("扫描完成！全部文件健康无损！")

    // 重点 2：文件确认无误后，再去拼接那段恶心的参数！
    val args = MinecraftArgBuilder(
        versionJsonString = versionJsonString,
        context = ctx
    ).build()

    val command = mutableListOf("java")
    command.addAll(args)

    println("你梦寐以求的启动参数：\n${command.joinToString(" ")}")

    // 执行命令
    val process = ProcessBuilder(command)
        .directory(File(ctx.gameDirectory))
        .redirectErrorStream(true)
        .start()

    // 重点 3：别特么启动完就跑！把流读出来！不然进程会死锁！
    Thread {
        process.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { println("[MC] $it") }
        }
    }.start()

}