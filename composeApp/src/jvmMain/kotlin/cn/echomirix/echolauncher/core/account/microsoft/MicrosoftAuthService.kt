package cn.echomirix.echolauncher.core.account.microsoft

import cn.echomirix.echolauncher.core.account.AccountType
import cn.echomirix.echolauncher.core.config.ConfigManager
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val logger = KotlinLogging.logger {}

object MicrosoftAuthService {
    // 经典的 Minecraft Java 版客户端 ID
    private const val REDIRECT_URI = "https://login.live.com/oauth20_desktop.srf"

    // 生成供玩家在浏览器中打开的登录链接
    fun getLoginUrl(): String {
        return "https://login.live.com/oauth20_authorize.srf" +
                "?client_id=${MicrosoftClientId.MinecraftJava.id}" +
                "&response_type=code" +
                "&redirect_uri=$REDIRECT_URI" +
                "&scope=XboxLive.signin%20offline_access"
    }

    /**
     * 执行完整的验证流程
     * @param authCode 玩家从浏览器复制的回调链接或 Code
     * @param client 注入的 Ktor HttpClient
     * @return 返回获取到的玩家档案
     */
    suspend fun authenticate(authCode: String, client: HttpClient): McProfileResponse = withContext(Dispatchers.IO) {
        try {
            val rawCode = extractCode(authCode) ?: throw IllegalArgumentException("无法从输入中提取授权码(code)。")

            val code = if (rawCode.contains("&")) rawCode.substringBefore("&") else rawCode
            logger.info { "开始微软登录流程，提取纯净授权码: ${code.take(5)}***" }

            logger.info { "步骤 1/5: 获取微软 OAuth Token..." }

            val oauthResponseRequest = client.submitForm(
                url = "https://login.live.com/oauth20_token.srf",
                formParameters = parameters {
                    append("client_id", MicrosoftClientId.MinecraftJava.id)
                    append("code", code)
                    append("grant_type", "authorization_code")
                    append("redirect_uri", REDIRECT_URI)
                }
            )

            if (!oauthResponseRequest.status.isSuccess()) {
                val errorBody = oauthResponseRequest.bodyAsText()
                logger.error { "微软 OAuth 接口拒绝了请求 (400)！原因: $errorBody" }
                throw IllegalStateException("兑换 Access Token 失败，可能是 Code 已过期，请重新登录！")
            }

            val oauthResponse = oauthResponseRequest.body<OAuthTokenResponse>()
            return@withContext getMinecraftToken(oauthResponse, client).second
        } catch (e: Exception) {
        logger.error(e) { "微软登录流程中断: ${e.message}" }
        throw e
        }
    }

    suspend fun getMinecraftToken(oauthResponse: OAuthTokenResponse, client: HttpClient): Pair<McAuthResponse, McProfileResponse> {
        logger.info { "步骤 2/5: 验证 Xbox Live..." }
//            logger.info { "body: ${Json.encodeToString(XblAuthRequest(XblProperties(RpsTicket = "d=${oauthResponse.accessToken}")))}" }
        val xblResponseReq = client.post("https://user.auth.xboxlive.com/user/authenticate") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(XblAuthRequest(XblProperties(RpsTicket = "d=${oauthResponse.accessToken}")))
        }
        if (!xblResponseReq.status.isSuccess()) {
            throw IllegalStateException("Xbox Live 验证失败: ${xblResponseReq.bodyAsText()}")
        }
        val xblResponse = xblResponseReq.body<XboxAuthResponse>()
        val xblToken = xblResponse.Token

        logger.info { "步骤 3/5: 验证 XSTS..." }
        val xstsResponseReq = client.post("https://xsts.auth.xboxlive.com/xsts/authorize") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(XstsAuthRequest(XstsProperties(UserTokens = listOf(xblToken))))
        }

        if (!xstsResponseReq.status.isSuccess()) {
            val errorBody = xstsResponseReq.bodyAsText()
            logger.error { "XSTS 验证失败: $errorBody" }
            try {
                val json = Json.parseToJsonElement(errorBody).jsonObject
                val msg = when (val xErr = json["XErr"]?.jsonPrimitive?.content) {
                    "2148916233" -> "账号未绑定 Xbox，请先去 xbox.com 创建游戏档案！"
                    "2148916238" -> "账号为未成年人账号，无法通过 Xbox 验证，请将账号年龄修改为成年或加入家庭组。"
                    else -> "Xbox 账号状态异常 (错误码: $xErr)"
                }
                throw IllegalStateException(msg)
            } catch (e: Exception) {
                throw IllegalStateException("Xbox 账号状态异常，请确保已开通 Xbox 档案。")
            }
        }
        val xstsResponse = xstsResponseReq.body<XboxAuthResponse>()
        val xstsToken = xstsResponse.Token
        val userHash = xstsResponse.DisplayClaims.xui.firstOrNull()?.get("uhs")
            ?: throw IllegalStateException("未能获取到 Xbox UserHash")

        logger.info { "步骤 4/5: 获取 Minecraft Access Token..." }
        val mcResponseReq = client.post("https://api.minecraftservices.com/authentication/login_with_xbox") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(McAuthRequest(identityToken = "XBL3.0 x=$userHash;$xstsToken"))
        }
        if (!mcResponseReq.status.isSuccess()) {
            throw IllegalStateException("Minecraft 验证失败: ${mcResponseReq.bodyAsText()}")
        }
        val mcResponse = mcResponseReq.body<McAuthResponse>()

        logger.info { "步骤 5/5: 获取 Minecraft 玩家档案..." }
        val profileResponseReq = client.get("https://api.minecraftservices.com/minecraft/profile") {
            header(HttpHeaders.Authorization, "Bearer ${mcResponse.access_token}")
        }

        if (profileResponseReq.status == HttpStatusCode.NotFound) {
            throw IllegalStateException("该微软账号没有购买 Minecraft！")
        } else if (!profileResponseReq.status.isSuccess()) {
            throw IllegalStateException("获取玩家档案失败: ${profileResponseReq.bodyAsText()}")
        }

        val profileResponse = profileResponseReq.body<McProfileResponse>()

        ConfigManager.updateConfig {
            copy(
                playerName = profileResponse.name,
                accountType = AccountType.MICROSOFT,
                playerUuid = profileResponse.id,
                microsoftToken = mcResponse.access_token,
                msRefreshToken = oauthResponse.refreshToken
            )
        }

        logger.info { "登录成功！欢迎玩家: ${profileResponse.name}" }
        return Pair(mcResponse, profileResponse)
    }


    suspend fun refreshAndGetMcToken(refreshToken: String, client: HttpClient): McProfileResponse = withContext(Dispatchers.IO) {
        logger.info { "开始后台静默刷新微软 Token..." }
        try {
            val oauthResponseRequest = client.submitForm(
                url = "https://login.live.com/oauth20_token.srf",
                formParameters = parameters {
                    append("client_id", MicrosoftClientId.MinecraftJava.id)
                    append("refresh_token", refreshToken)
                    append("grant_type", "refresh_token")
                    append("redirect_uri", REDIRECT_URI)
                }
            )

            if (!oauthResponseRequest.status.isSuccess()) {
                throw IllegalStateException("刷新 Token 失败，登录已失效，请重新手动登录。")
            }
            val oauthResponse = oauthResponseRequest.body<OAuthTokenResponse>()

            val pair = getMinecraftToken(oauthResponse, client)
            val mcResponse = pair.first
            val profileResponse = pair.second

            ConfigManager.updateConfig {
                copy(
                    microsoftToken = mcResponse.access_token,
                    msRefreshToken = oauthResponse.refreshToken
                )
            }

            return@withContext profileResponse
        } catch (e: Exception) {
            logger.error(e) { "静默刷新失败" }
            throw e
        }
    }

    private fun extractCode(input: String): String? {
        if (input.contains("code=")) {
            val regex = "code=([^&]+)".toRegex()
            return regex.find(input)?.groupValues?.get(1)
        }
        if (input.isNotBlank() && !input.contains("http")) {
            return input.trim()
        }
        return null
    }
}