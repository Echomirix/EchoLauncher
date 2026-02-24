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
import cn.echomirix.echolauncher.core.config.AppConfig

object Interface {
    @Composable
    fun HomeScreen() {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            ) {
                Card(
                    modifier = Modifier.fillMaxHeight().width(320.dp).padding(AppConfig.CARD_DEFAULT_PADDING_HORIZONTAL.dp, AppConfig.CARD_DEFAULT_PADDING_VERTICAL.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    ), // 略带透明的表面色
                    content = {
                        Column (modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceEvenly) {
                            Spacer(modifier = Modifier.weight(0.5f))

                        // --- 顶部：玩家信息区域 ---
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(AppConfig.UI_DEFAULT_PADDING.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
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
                                        text = "Echomirix", // TODO: 动态玩家名
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

                                // 修改名称/切换账号按钮
                                IconButton(onClick = { /* TODO: 切换账号 */ }) {
                                    Icon(
                                        imageVector = Icons.Rounded.SwitchAccount,
                                        contentDescription = "切换账号"
                                    )
                                }
                            }
                        }
                        // --- 中间可以放其他信息 (暂时用 Spacer 撑开) ---
                        Spacer(modifier = Modifier.weight(1f))


                        // --- 底部：版本与启动区域 ---
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .padding(AppConfig.UI_DEFAULT_PADDING.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 版本信息卡片 (可以点击展开版本列表)
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
                                    // 版本图标 (Placeholder)
                                    Icon(
                                        imageVector = Icons.Rounded.VideogameAsset,
                                        contentDescription = "Version Icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Fabric 1.20.1", // TODO: 动态版本名称
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "1.20.1", // TODO: 动态底层版本号
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // 版本设置按钮
                                    IconButton(onClick = { /* TODO: 版本独立设置 */ }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Settings,
                                            contentDescription = "Version Settings"
                                        )
                                    }
                                }
                            }

                            // 核心：启动游戏按钮
                            Button(
                                onClick = { /* TODO: 启动游戏逻辑 */ },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(AppConfig.UI_DEFAULT_PADDING.dp)
                                    .height(56.dp), // 加高按钮显得有分量
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Play"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "开始游戏",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }})
            }
        }

        // =============== 右侧：背景或公告区域 (Placeholder) ===============
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "EchoLauncher 的右侧空间\n将来可以放动态背景或者社区公告",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "欢迎使用 EchoLauncher！",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }

    @Composable
    fun DownloadScreen() {
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

    @Composable
    fun SettingsScreen() {
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

    @Composable
    fun AboutScreen() {
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