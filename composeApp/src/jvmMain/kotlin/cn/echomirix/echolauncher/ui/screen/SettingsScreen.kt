package cn.echomirix.echolauncher.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import cn.echomirix.echolauncher.core.config.ConfigManager
import cn.echomirix.echolauncher.core.config.LocalAppConfig
import cn.echomirix.echolauncher.ui.ColorSettingRow
import cn.echomirix.echolauncher.ui.JavaSettingRow
class SettingsScreen : TabScreen {
    override val index = 2

    @Composable
    override fun Content() {
        val appConfig = LocalAppConfig.current
        
        var draftConfig by remember(appConfig) { mutableStateOf(appConfig.copy()) }

        val isModified = draftConfig != appConfig

        val currentDraft by rememberUpdatedState(draftConfig)
        val currentIsModified by rememberUpdatedState(isModified)

        DisposableEffect(Unit) {
            onDispose {
                // 当此界面从导航栈中被移除或被其他 Tab 盖住（如果不再进行组合）时触发
                if (currentIsModified) {
                    println("[SettingsScreen] 检测到未保存的更改，离开页面时自动保存...")
                    ConfigManager.updateConfig { currentDraft.copy() }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(draftConfig.subColor)) // 预览背景色修改
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                // 底部多留点空间，避免被浮动按钮遮挡
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. 游戏运行设置
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "游戏运行目录",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            OutlinedTextField(
                                value = draftConfig.minecraftDir,
                                onValueChange = { draftConfig = draftConfig.copy(minecraftDir = it) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Minecraft 根目录 (.minecraft)") },
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = draftConfig.customMinecraftDir ?: "",
                                onValueChange = {
                                    draftConfig = draftConfig.copy(customMinecraftDir = it.ifBlank { null })
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("自定义游戏运行目录 (可选)") },
                                placeholder = { Text("如果不填，将默认使用 Minecraft 根目录") },
                                singleLine = true
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = draftConfig.isIsolated,
                                    onCheckedChange = { draftConfig = draftConfig.copy(isIsolated = it) }
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("版本隔离", style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        "开启后每个版本将使用独立的文件夹存储 mods、saves 等",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            JavaSettingRow(
                                draftConfig.javaPath,
                                draftConfig.javaList,
                                onJavaPathChange = { draftConfig = draftConfig.copy(javaPath = it) }
                            )
                        }
                    }
                }

                // 3. 外观设置
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "外观与个性化",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // 使用我们刚才封装的高级组件！
                            ColorSettingRow(
                                label = "主色调 (ARGB Hex)",
                                colorValue = draftConfig.primaryColor,
                                onColorChange = { draftConfig = draftConfig.copy(primaryColor = it) }
                            )

                            ColorSettingRow(
                                label = "副色调 / 背景色 (ARGB Hex)",
                                colorValue = draftConfig.subColor,
                                onColorChange = { draftConfig = draftConfig.copy(subColor = it) }
                            )
                        }
                    }
                }
            }

            // 3. 只有当配置发生变化时，才显示悬浮保存按钮
            if (isModified) {
                var isHovered by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                        // 检测鼠标悬停事件
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    when (event.type) {
                                        androidx.compose.ui.input.pointer.PointerEventType.Enter -> isHovered = true
                                        androidx.compose.ui.input.pointer.PointerEventType.Exit -> isHovered = false
                                    }
                                }
                            }
                        },
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // “撤销更改”按钮（仅在悬停时显示）
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isHovered,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { it / 2 }),
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { it / 2 })
                    ) {
                        SmallFloatingActionButton(
                            onClick = {
                                // 撤销更改：将草稿恢复为与当前 appConfig 相同的状态
                                draftConfig = appConfig.copy()
                            },
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Restore,
                                contentDescription = "撤销更改"
                            )
                        }
                    }

                    // “保存修改”主按钮
                    ExtendedFloatingActionButton(
                        onClick = {
                            // 将草稿一次性覆盖到全局配置
                            ConfigManager.updateConfig { draftConfig.copy() }
                        },
                        icon = {
                            Icon(
                                Icons.Rounded.SystemUpdateAlt,
                                contentDescription = "保存"
                            )
                        },
                        text = { Text("保存修改") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}