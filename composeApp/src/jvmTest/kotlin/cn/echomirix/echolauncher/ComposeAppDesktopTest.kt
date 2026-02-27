package cn.echomirix.echolauncher

import cn.echomirix.echolauncher.core.mod.modrinth.ModrinthProjectModel
import cn.echomirix.echolauncher.core.mod.modrinth.ModrinthSearchModel
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import kotlin.test.Test
import kotlin.test.assertNotNull

class ComposeAppDesktopTest {

    val khttp = OkHttpClient.Builder().build()
    @Test
    fun testModrinthProjectModel() {
        val url = "https://api.modrinth.com/v2/project/tweakeroo"
        val str = khttp.newCall(
            okhttp3.Request.Builder().url(url).build()
        ).execute().body?.string()
        assertNotNull(str)
        Json.decodeFromString(ModrinthProjectModel.serializer(), str)
    }

    @Test
    fun testModrinthSearchModel() {
        val url = "https://api.modrinth.com/v2/search?query=tweakeroo"
        val str = khttp.newCall(
            okhttp3.Request.Builder().url(url).build()
        ).execute().body?.string()
        assertNotNull(str)
        Json.decodeFromString(ModrinthSearchModel.serializer(), str)
    }
}