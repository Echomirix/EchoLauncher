package cn.echomirix.echolauncher.core.mod

import cn.echomirix.echolauncher.core.mod.modrinth.Hit
import cn.echomirix.echolauncher.core.mod.modrinth.ModrinthApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ModSearchManager {
    suspend fun searchMods(
        query: String,
        gameVersion: String,
        loader: LoaderType,
        offset: Int
    ): List<Hit> = withContext(Dispatchers.IO)  {
        // 我们默认每页加载 20 个
        return@withContext ModrinthApi.searchMods(query, gameVersion, loader, offset)
    }
}