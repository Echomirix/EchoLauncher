package cn.echomirix.echolauncher.core.mod.modrinth

import cn.echomirix.echolauncher.core.config.AppConstant
import cn.echomirix.echolauncher.core.mod.LoaderType
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object ModrinthApi {

    private val logger = KotlinLogging.logger {}
    private const val BASE_URL = "https://api.modrinth.com/v2"

    suspend fun searchMods(
        query: String,
        gameVersion: String,
        loader: LoaderType,
        offset: Int = 0,
        limit: Int = 20
    ): List<Hit> = withContext(Dispatchers.IO) {
        // 构建 facets (过滤器二维数组)
        val facets = mutableListOf<List<String>>()

        // 1. 过滤游戏版本
        if (gameVersion.isNotBlank()) {
            facets.add(listOf("versions:$gameVersion"))
        }

        // 2. 过滤加载器 (忽略 UNKNOWN)
        if (loader != LoaderType.UNKNOWN) {
            val loaderName = loader.name.lowercase()
            facets.add(listOf("categories:$loaderName"))
        }

        // 3. 强制只搜索 Mod (排除资源包和整合包)
        facets.add(listOf("project_type:mod"))

        logger.info { "准备搜索mod" }

        val modrinthSearchModel = AppConstant.HttpClient.get("$BASE_URL/search") {
            if (query.isNotBlank()) parameter("query", query)
            if (facets.isNotEmpty()) {
                parameter("facets", Json.encodeToString(facets))
            }
            parameter("limit", limit)
            parameter("offset", offset)
        }.body<ModrinthSearchModel>()

        logger.info { "搜索完毕，共 ${modrinthSearchModel.hits.size} 条结果" }
        return@withContext modrinthSearchModel.hits
    }
}