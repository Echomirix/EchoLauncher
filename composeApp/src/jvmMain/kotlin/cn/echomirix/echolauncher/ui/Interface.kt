package cn.echomirix.echolauncher.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cn.echomirix.echolauncher.core.GameManager
import cn.echomirix.echolauncher.core.LaunchContext
import cn.echomirix.echolauncher.core.LaunchState
import cn.echomirix.echolauncher.core.config.AppConfig


interface IndexedScreen : Screen {
    val index: Int
}

class HomeScreen : IndexedScreen {
    override val index = 0

    @Composable
    override fun Content() {
        // 观察全局启动状态
        val launchState = GameManager.currentState
        val statusText = GameManager.statusText
        val isGameRunning = GameManager.activeProcess?.isAlive == true

        // 构造你的上下文
        val ctx = remember {
            LaunchContext(
                authPlayerName = "Echomirix",
                authUuid = "123e4567-e89b-12d3-a456-426614174000",
                authAccessToken = "0",
                version = "1.20.2-Fabric 0.18.4",
                minecraftDir = "D:/Project/Java/EchoLauncher/.minecraft"
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            ) {
                // =============== 左侧：操作卡片区域 ===============
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(360.dp)
                        .padding(
                            horizontal = AppConfig.CARD_DEFAULT_PADDING_HORIZONTAL.dp,
                            vertical = AppConfig.CARD_DEFAULT_PADDING_VERTICAL.dp
                        )
                ) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        )
                    ) {
                        // 还原你心心念念的 SpaceBetween 上下结构！
                        Column(
                            modifier = Modifier.fillMaxSize().padding(AppConfig.UI_DEFAULT_PADDING.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // --- 顶部：玩家信息区域 (死死钉住，绝对不参与动画) ---
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(20)).background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Person, contentDescription = "玩家头像", tint = MaterialTheme.colorScheme.onPrimary)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Echomirix", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("离线账户", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { /* TODO: 切换账号 */ }) {
                                    Icon(Icons.Rounded.SwitchAccount, contentDescription = "切换账号")
                                }
                            }

                            // --- 底部：带动画的状态机区域 ---
                            AnimatedContent(
                                targetState = launchState,
                                transitionSpec = {
                                    (fadeIn() + slideInVertically { height -> height / 2 }) togetherWith
                                            (fadeOut() + slideOutVertically { height -> -height / 2 })
                                },
                                label = "LaunchStateAnimation",
                                modifier = Modifier.fillMaxWidth()
                            ) { state ->
                                when (state) {
                                    // 状态1：空闲，显示版本信息和启动按钮
                                    LaunchState.IDLE -> {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Surface(
                                                onClick = { /* TODO: 弹出版本选择菜单 */ },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surface
                                            ) {
                                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Rounded.VideogameAsset, contentDescription = "Version Icon", tint = MaterialTheme.colorScheme.primary)
                                                    Spacer(modifier = Modifier.width(16.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text("Fabric 1.20.1", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                        Text("1.20.1", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                    IconButton(onClick = { /* TODO: 版本设置 */ }) {
                                                        Icon(Icons.Rounded.Settings, contentDescription = "设置")
                                                    }
                                                }
                                            }

                                            Button(
                                                onClick = { GameManager.startGame(ctx) },
                                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play")
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("开始游戏", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // 状态2：校验与启动中，显示转圈圈
                                    LaunchState.CHECKING, LaunchState.STARTING -> {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.height(24.dp))
                                            Text(statusText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }

                                    // 状态3：启动成功，显示绿勾
                                    LaunchState.SUCCESS -> {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Rounded.CheckCircle, contentDescription = "Success", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
                                            Spacer(Modifier.height(16.dp))
                                            Text(statusText, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    // 状态4：异常报错，显示返回按钮
                                    LaunchState.ERROR -> {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(statusText, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                            Button(
                                                onClick = { GameManager.killGame() },
                                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                            ) {
                                                Icon(Icons.Rounded.Refresh, contentDescription = "Retry")
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("返回重试", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
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

            // =============== 右下角：全局悬浮的关机按钮 ===============
            if (isGameRunning) {
                FloatingActionButton(
                    onClick = { GameManager.killGame() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 32.dp, bottom = 32.dp),
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PowerSettingsNew,
                        contentDescription = "强制结束进程"
                    )
                }
            }
        }
    }
}
// (下面保留你原有的 DownloadScreen, SettingsScreen, AboutScreen)


class DownloadScreen : IndexedScreen {
    override val index = 1
    @Composable
    override fun Content() {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
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
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
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
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
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