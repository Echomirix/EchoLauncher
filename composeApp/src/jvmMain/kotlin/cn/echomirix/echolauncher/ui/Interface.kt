package cn.echomirix.echolauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cn.echomirix.echolauncher.core.GameManager
import cn.echomirix.echolauncher.core.LaunchContext
import cn.echomirix.echolauncher.core.account.AccountType
import cn.echomirix.echolauncher.core.config.AppConstant
import cn.echomirix.echolauncher.core.config.ConfigManager
import cn.echomirix.echolauncher.core.config.LocalAppConfig
import cn.echomirix.echolauncher.core.version.LocalVersion
import cn.echomirix.echolauncher.core.version.VersionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


interface IndexedScreen : Screen {
    val index: Int
}

class HomeScreen : IndexedScreen {
    override val index = 0

    @Composable
    override fun Content() {
        // 观察全局启动状态
        val launchStatus by GameManager.status.collectAsState()
        val isGameRunning = GameManager.activeProcess?.isAlive == true

        val appConfig = LocalAppConfig.current

        // 统一写死根目录，以后再抽到全局配置或者Settings里去
        val minecraftDir = "D:/Project/Java/EchoLauncher/.minecraft"

        var versions by remember { mutableStateOf<List<LocalVersion>>(emptyList()) }
        var selectedVersion by remember { mutableStateOf<LocalVersion?>(null) }
        var showAccountDialog by remember { mutableStateOf(false) }

        LaunchedEffect(minecraftDir) {
            withContext(Dispatchers.IO) {
                val scanned = VersionManager.scanLocalVersions(minecraftDir)
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
                authUuid = "123e4567-e89b-12d3-a456-426614174000",
                authAccessToken = "0",
                version = selectedVersion?.id ?: "null",
                minecraftDir = minecraftDir
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
                                launchStatus = launchStatus,
                                onLaunch = {
                                    if (selectedVersion != null) {
                                        GameManager.startGame(ctx)
                                    }
                                },
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
            if (isGameRunning) {
                FloatingActionButton(
                    onClick = { GameManager.killGame() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 48.dp, bottom = 48.dp),
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PowerSettingsNew,
                        contentDescription = "强制结束进程"
                    )
                }
            }
            if (showAccountDialog) {
                var tempName by remember { mutableStateOf(appConfig.playerName) }
                var tempType by remember { mutableStateOf(appConfig.accountType) }
                var accountMenuExpanded by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { showAccountDialog = false },
                    title = { Text("账号切换与管理", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // 下拉菜单选类型
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = when (tempType) {
                                        AccountType.OFFLINE -> "离线模式"; AccountType.LITTLESKIN -> "LittleSkin (TODO)"; else -> "Microsoft (TODO)"
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("登录方式") },
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        IconButton(onClick = { accountMenuExpanded = true }) {
                                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = "展开")
                                        }
                                    }
                                )
                                Surface(
                                    modifier = Modifier.matchParentSize(),
                                    color = Color.Transparent,
                                    onClick = { accountMenuExpanded = true }
                                ) {}

                                DropdownMenu(
                                    expanded = accountMenuExpanded,
                                    onDismissRequest = { accountMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("离线模式") },
                                        onClick = { tempType = AccountType.OFFLINE; accountMenuExpanded = false })
                                    DropdownMenuItem(
                                        text = { Text("LittleSkin (TODO)") },
                                        onClick = { tempType = AccountType.LITTLESKIN; accountMenuExpanded = false })
                                    DropdownMenuItem(
                                        text = { Text("Microsoft (TODO)") },
                                        onClick = { tempType = AccountType.MICROSOFT; accountMenuExpanded = false })
                                }
                            }

                            // 离线模式专属：输入玩家ID
                            if (tempType == AccountType.OFFLINE) {
                                OutlinedTextField(
                                    value = tempName,
                                    onValueChange = { tempName = it },
                                    label = { Text("离线玩家 ID") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text(
                                    text = "该登录方式核心逻辑暂未实现，请切回离线模式！",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (tempType == AccountType.OFFLINE && tempName.isNotBlank()) {
                                ConfigManager.updateConfig {
                                    copy(
                                        playerName = tempName,
                                        accountType = tempType
                                    )
                                }
                                showAccountDialog = false
                            }
                        }) {
                            Text("保存并使用")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAccountDialog = false }) {
                            Text("取消")
                        }
                    }
                )
            }
        }
    }
}


class DownloadScreen : IndexedScreen {
    override val index = 1

    @Composable
    override fun Content() {
        val appConfig = LocalAppConfig.current
        Box(
            modifier = Modifier.fillMaxSize().background(Color(appConfig.subColor)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "下载界面正在开发中...",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

class SettingsScreen : IndexedScreen {
    override val index = 2

    @Composable
    override fun Content() {
        val appConfig = LocalAppConfig.current
        Box(
            modifier = Modifier.fillMaxSize().background(Color(appConfig.subColor)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "设置界面正在开发中...",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

class AboutScreen : IndexedScreen {
    override val index = 3

    @Composable
    override fun Content() {
        val appConfig = LocalAppConfig.current
        Box(
            modifier = Modifier.fillMaxSize().background(Color(appConfig.subColor)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "关于界面正在开发中...",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}