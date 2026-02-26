package cn.echomirix.echolauncher.core.download

import cn.echomirix.echolauncher.core.config.AppConstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.URL

object DownloadManager {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun getVersionList(): List<Version> = withContext(Dispatchers.IO) {
        val manifest = fetchVersionManifest()
        manifest.versions
    }

    private suspend fun fetchVersionManifest(): VersionManifest = withContext(Dispatchers.IO) {
        val text = URL(AppConstant.VERSION_MANIFEST_URL).readText()
        val versionManifest = json.decodeFromString(VersionManifest.serializer(), text)

        versionManifest
    }
}