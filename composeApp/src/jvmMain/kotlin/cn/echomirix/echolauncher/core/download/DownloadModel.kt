package cn.echomirix.echolauncher.core.download

import kotlinx.serialization.Serializable

@Serializable
data class VersionManifest(
    val latest: Latest,
    val versions: List<Version>
)

@Serializable
data class Latest(
    val release: String,
    val snapshot: String
)


@Serializable
data class Version(
    val id: String,
    val releaseTime: String,
    val time: String,
    val type: String,
    val url: String
)