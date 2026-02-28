package cn.echomirix.echolauncher.core.config

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.awt.Dimension

object AppConstant {
    const val APP_NAME = "EchoLauncher"
    const val APP_ABBR = "EchoL"
    const val APP_VERSION = "1.0.0"

    const val WINDOW_WIDTH_DEFAULT = 800
    const val WINDOW_HEIGHT_DEFAULT = 550

    const val WINDOW_MIN_WIDTH = 600
    const val WINDOW_MIN_HEIGHT = 450
    val WINDOW_MIN_SIZE = Dimension(WINDOW_MIN_WIDTH, WINDOW_MIN_HEIGHT)

    const val WINDOW_ROUND_CORNER_RADIUS = 16
    const val TOPBAR_HEIGHT = 48
    const val NAVBAR_HEIGHT = 60

    const val UI_DEFAULT_PADDING = 20
    const val CARD_DEFAULT_PADDING_HORIZONTAL = 40
    const val CARD_DEFAULT_PADDING_VERTICAL = 30

    const val VERSION_MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json"

    val HttpClient = HttpClient(OkHttp) {
        expectSuccess = false // 【确保这个是 false，或者干脆不写（默认是 false），这样我们才能手动抓取 400 的响应体】

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true // 必须开启！微软返回的 JSON 有很多乱七八糟我们不需要的字段
                isLenient = true
                encodeDefaults = true
            })
        }
        // ...
    }
}