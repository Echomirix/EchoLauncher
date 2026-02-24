package cn.echomirix.echolauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cn.echomirix.echolauncher.core.StartGame
import cn.echomirix.echolauncher.core.config.AppConfig


interface IndexedScreen : Screen {
    val index: Int
}


class HomeScreen : IndexedScreen {
    override val index = 0
    @Composable
    override fun Content() {
        // 核心修改 1：最外层使用 Row，让左侧卡片和右侧内容水平排列
        Row(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        ) {
            // =============== 左侧：操作卡片区域 ===============
            // 给左侧一个固定的宽度空间，并设置外边距(padding)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(360.dp) // 稍微加宽一点，容纳卡片和外边距
                    .padding(
                        horizontal = AppConfig.CARD_DEFAULT_PADDING_HORIZONTAL.dp,
                        vertical = AppConfig.CARD_DEFAULT_PADDING_VERTICAL.dp
                    )
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(), // 填满 Box 剩余的空间
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    )
                ) {
                    // 卡片内部的所有内容
                    Column(
                        modifier = Modifier.fillMaxSize().padding(AppConfig.UI_DEFAULT_PADDING.dp),
                        verticalArrangement = Arrangement.SpaceBetween // 顶部放头像，底部放启动，中间空开
                    ) {
                        // --- 顶部：玩家信息区域 ---
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            // 玩家头像（占位）
                            Box(
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(20))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = "玩家头像",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))

                            // 玩家名称
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Echomirix",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "离线账户",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // 切换账号按钮
                            IconButton(onClick = { /* TODO: 切换账号 */ }) {
                                Icon(
                                    imageVector = Icons.Rounded.SwitchAccount,
                                    contentDescription = "切换账号"
                                )
                            }
                        }

                        // --- 底部：版本与启动区域 ---
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 版本信息卡片
                            Surface(
                                onClick = { /* TODO: 弹出版本选择菜单 */ },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 2.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VideogameAsset,
                                        contentDescription = "Version Icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Fabric 1.20.1",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "1.20.1",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(onClick = { /* TODO: 版本设置 */ }) {
                                        Icon(Icons.Rounded.Settings, contentDescription = "设置")
                                    }
                                }
                            }

                            // 启动游戏按钮
                            Button(
                                onClick = { StartGame() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "开始游戏",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // =============== 右侧：背景或公告区域 ===============
            // 核心修改 2：使用 weight(1f) 占据右侧所有剩余空间
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "EchoLauncher 的右侧空间\n将来可以放动态背景或者社区公告",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

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