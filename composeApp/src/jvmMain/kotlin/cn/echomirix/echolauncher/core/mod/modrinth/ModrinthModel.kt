package cn.echomirix.echolauncher.core.mod.modrinth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModrinthSearchModel(
    val hits: List<Hit>,
    val limit: Int,
    val offset: Int,
    val total_hits: Int
)

@Serializable
data class Hit(
    val author: String,
    val categories: List<String>,
    val client_side: SideSupport,
    val color: Int? = null,
    val date_created: String,
    val date_modified: String,
    val description: String,
    val display_categories: List<String>,
    val downloads: Int,
    val featured_gallery: String? = null,
    val follows: Int,
    val gallery: List<String>,
    val icon_url: String,
    val latest_version: String,
    val license: String? = null,
    val project_id: String,
    val project_type: String,
    val server_side: SideSupport,
    val slug: String,
    val title: String,
    val versions: List<String>
)

@Serializable
data class ModrinthProjectModel(
    val additional_categories: List<String>,
    val approved: String? = null,
    val body: String,
    val body_url: String? = null,
    val categories: List<String>,
    val client_side: SideSupport,
    val color: Int? = null,
    val description: String,
    val discord_url: String? = null,
    val donation_urls: List<DonationUrl>,
    val downloads: Int,
    val followers: Int,
    val gallery: List<Gallery>,
    val game_versions: List<String>,
    val icon_url: String? = null,
    val id: String,
    val issues_url: String? = null,
    val license: License,
    val loaders: List<String>,
    val moderator_message: ModeratorMessage? = null,
    val monetization_status: MonetizationStatus,
    val organization: String? = null,
    val project_type: ProjectType,
    val published: String,
    val queued: String? = null,
    val requested_status: RequestedStatus? = null,
    val server_side: SideSupport,
    val slug: String,
    val source_url: String? = null,
    val status: RequestedStatus,
    val team: String,
    val thread_id: String,
    val title: String,
    val updated: String,
    val versions: List<String>,
    val wiki_url: String? = null
)

@Serializable
data class ModeratorMessage(
    val message: String,
    val body: String? = null
)

@Serializable
data class Gallery(
    val url: String,
    val featured: Boolean,
    val title: String? = null,
    val description: String? = null,
    val created: String,
    val ordering: Int
)

@Serializable
data class DonationUrl(
    val id: String? = null,
    val platform: String? = null,
    val url: String? = null
)

@Serializable
data class License(
    val id: String,
    val name: String,
    val url: String? = null
)

@Serializable
enum class ProjectType {
    @SerialName("mod")
    MOD,

    @SerialName("resourcepack")
    RESOURCE_PACK,

    @SerialName("modpack")
    MOD_PACK,

    @SerialName("shader")
    SHADER
}

@Serializable
enum class SideSupport {
    @SerialName("required")
    REQUIRED,

    @SerialName("optional")
    OPTIONAL,

    @SerialName("unsupported")
    UNSUPPORTED,

    @SerialName("unknown")
    UNKNOWN
}

@Serializable
enum class RequestedStatus {
    @SerialName("approved")
    APPROVED,

    @SerialName("archived")
    ARCHIVED,

    @SerialName("unlisted")
    UNLISTED,

    @SerialName("private")
    PRIVATE,

    @SerialName("rejected")
    REJECTED,

    @SerialName("processing")
    PROCESSING,

    @SerialName("withheld")
    WITHHELD,

    @SerialName("scheduled")
    SCHEDULED,

    @SerialName("draft")
    DRAFT
}

@Serializable
enum class MonetizationStatus {
    @SerialName("monetized")
    MONETIZED,

    @SerialName("demonetized")
    DEMONETIZED,

    @SerialName("force-monetized")
    FORCE_MONETIZED
}