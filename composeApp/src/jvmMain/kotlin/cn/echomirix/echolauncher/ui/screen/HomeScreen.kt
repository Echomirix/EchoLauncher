package cn.echomirix.echolauncher.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.echomirix.echolauncher.core.LaunchContext
import cn.echomirix.echolauncher.core.LaunchManager
import cn.echomirix.echolauncher.core.LaunchState
import cn.echomirix.echolauncher.core.LaunchStatus
import cn.echomirix.echolauncher.core.account.AccountType
import cn.echomirix.echolauncher.core.account.microsoft.MicrosoftAuthService
import cn.echomirix.echolauncher.core.config.AppConstant
import cn.echomirix.echolauncher.core.config.ConfigManager
import cn.echomirix.echolauncher.core.config.LocalAppConfig
import cn.echomirix.echolauncher.core.version.LocalVersion
import cn.echomirix.echolauncher.core.version.LocalVersionManager
import cn.echomirix.echolauncher.ui.ChangeAccountDialog
import cn.echomirix.echolauncher.ui.LaunchActionArea
import cn.echomirix.echolauncher.ui.UserProfileCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeScreen : TabScreen {
    override val index = 0

    @Composable
    override fun Content() {
        // 观察全局正在运行的任务列表
        val activeTasks by LaunchManager.activeTasks.collectAsState()

        // 判断全局是否有游戏在运行
        val isAnyGameRunning = activeTasks.isNotEmpty()

        val appConfig = LocalAppConfig.current
        val minecraftDir = appConfig.minecraftDir

        var versions by remember { mutableStateOf<List<LocalVersion>>(emptyList()) }
        var selectedVersion by remember { mutableStateOf<LocalVersion?>(null) }
        var showAccountDialog by remember { mutableStateOf(false) }

        val currentVersionTask = remember(activeTasks, selectedVersion) {
            activeTasks.find { it.ctx.version == selectedVersion?.id }
        }

        val currentVersionStatus by (currentVersionTask?.status
            ?: MutableStateFlow(LaunchStatus(LaunchState.IDLE, "等待启动"))).collectAsState()


        LaunchedEffect(minecraftDir) {
            withContext(Dispatchers.IO) {
                val scanned = LocalVersionManager.scanLocalVersions(minecraftDir)
                versions = scanned
                val savedId = appConfig.selectedVersionId
                if (scanned.isNotEmpty() && selectedVersion == null) {
                    selectedVersion = scanned.find { it.id == savedId } ?: scanned.firstOrNull()
                }
            }
        }

        val ctx = remember(selectedVersion) {
            LaunchContext(
                authPlayerName = appConfig.playerName,
                authUuid = appConfig.playerUuid,
                authAccessToken = appConfig.microsoftToken?:"",
                version = selectedVersion?.id ?: "null",
                minecraftDir = minecraftDir,
                javaPath = appConfig.javaPath
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize().background(Color(appConfig.subColor))) {
                // =============== 左侧：操作卡片区域 ===============
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(360.dp)
                        .padding(
                            horizontal = AppConstant.CARD_DEFAULT_PADDING_HORIZONTAL.dp,
                            vertical = AppConstant.CARD_DEFAULT_PADDING_VERTICAL.dp
                        )
                ) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(AppConstant.UI_DEFAULT_PADDING.dp)
                        ) {
                            // --- 1. 顶部：玩家信息区域 (死死钉住，不参与动画) ---
                            UserProfileCard(appConfig) {
                                showAccountDialog = true
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // --- 3. 底部：动画状态机 ---
                            LaunchActionArea(
                                versions = versions,
                                selectedVersion = selectedVersion,
                                onVersionSelected = {
                                    selectedVersion = it
                                    ConfigManager.updateConfig { copy(selectedVersionId = it.id) }
                                },
                                launchStatus = currentVersionStatus,
                                onLaunch = {

                                    if (appConfig.accountType == AccountType.MICROSOFT) {
                                        // 先检查并在后台刷新一下
                                        CoroutineScope(Dispatchers.IO).launch {
                                            try {
                                                // 如果有 refresh token，就静默刷新
                                                appConfig.msRefreshToken?.let {
                                                    if (it.isNotBlank()) {
                                                        MicrosoftAuthService.refreshAndGetMcToken(
                                                            appConfig.msRefreshToken,
                                                            AppConstant.HttpClient
                                                        )
                                                    }
                                                }

                                                // 刷新成功，去调起真正的 LaunchTask (此时 Config 里的 mcToken 已经是全新的了)
                                                LaunchManager.launch(ctx)

                                            } catch (e: Exception) {
                                                // 如果刷新失败（比如半年没玩过期了），弹窗让玩家重新扫码登录
//                                                showMicrosoftLoginDialog = true
                                            }
                                        }
                                    } else {
                                        // 离线模式直接启动
                                        LaunchManager.launch(ctx)
                                    }
                                }
                            )

                        }
                    }
                }

                // =============== 右侧：背景或公告区域 ===============
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "EchoLauncher 的右侧空间\n将来可以放动态背景或者社区公告",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // =============== 终极武器：全局悬浮的关机按钮 ===============
            if (isAnyGameRunning) {
                // 控制弹出菜单的状态
                var expanded by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 48.dp, bottom = 48.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            if (activeTasks.size == 1) {
                                // 只有一个，直接杀
                                activeTasks.first().stop()
                            } else {
                                // 有多个，展开菜单
                                expanded = true
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PowerSettingsNew,
                            contentDescription = "强制结束进程"
                        )
                    }

                    // 如果有多个任务，弹出一个下拉菜单让玩家选择要杀哪个进程
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        activeTasks.forEach { task ->
                            DropdownMenuItem(
                                text = { Text("强制结束：${task.ctx.version}") },
                                onClick = {
                                    task.stop()
                                    expanded = false
                                }
                            )
                        }
                        if (activeTasks.size > 1) {
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("结束所有游戏", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    activeTasks.forEach { it.stop() }
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            if (showAccountDialog) {
                ChangeAccountDialog(
                    appConfig = appConfig,
                    onDismiss = { showAccountDialog = false },
                    onConfirm = { tempName, tempType ->
                        run {
                            if (tempType == AccountType.OFFLINE && tempName.isNotBlank()) {
                                ConfigManager.updateConfig {
                                    copy(
                                        playerName = tempName,
                                        accountType = tempType
                                    )
                                }
                                showAccountDialog = false
                            }
                        }
                    })

            }
        }
    }
}