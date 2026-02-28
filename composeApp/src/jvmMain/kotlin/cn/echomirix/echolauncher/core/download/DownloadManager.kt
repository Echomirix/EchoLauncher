package cn.echomirix.echolauncher.core.download

import cn.echomirix.echolauncher.core.config.AppConstant
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DownloadManager {

    suspend fun getVersionList(): List<Version> = withContext(Dispatchers.IO) {
        val manifest = fetchVersionManifest()
        manifest.versions
    }

    private suspend fun fetchVersionManifest(): VersionManifest = withContext(Dispatchers.IO) {
        val versionManifest = AppConstant.HttpClient.get { url(AppConstant.VERSION_MANIFEST_URL) }.body<VersionManifest>()

        versionManifest
    }
}