package cn.echomirix.echolauncher.core.account.microsoft

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OAuthTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Int
)

@Serializable
data class XblAuthRequest(
    val Properties: XblProperties,
    val RelyingParty: String = "http://auth.xboxlive.com",
    val TokenType: String = "JWT"
)

@Serializable
data class XblProperties(
    val AuthMethod: String = "RPS",
    val SiteName: String = "user.auth.xboxlive.com",
    val RpsTicket: String
)

@Serializable
data class XstsAuthRequest(
    val Properties: XstsProperties,
    val RelyingParty: String = "rp://api.minecraftservices.com/",
    val TokenType: String = "JWT"
)

@Serializable
data class XstsProperties(
    val SandboxId: String = "RETAIL",
    val UserTokens: List<String>
)

@Serializable
data class XboxAuthResponse(
    val Token: String,
    val DisplayClaims: DisplayClaims
)

@Serializable
data class DisplayClaims(
    val xui: List<Map<String, String>>
)

@Serializable
data class McAuthRequest(
    val identityToken: String
)

@Serializable
data class McAuthResponse(
    val access_token: String,
    val expires_in: Int
)

@Serializable
data class McProfileResponse(
    val id: String,
    val name: String
)