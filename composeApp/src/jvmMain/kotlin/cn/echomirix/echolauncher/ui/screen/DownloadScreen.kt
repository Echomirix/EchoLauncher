package cn.echomirix.echolauncher.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VideogameAsset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cn.echomirix.echolauncher.core.config.LocalAppConfig
import cn.echomirix.echolauncher.core.download.DownloadManager
import cn.echomirix.echolauncher.core.download.Version
import cn.echomirix.echolauncher.ui.DownloadSideBar
import cn.echomirix.echolauncher.ui.SelectedInfoCard
import cn.echomirix.echolauncher.ui.VersionSearchBar

enum class DownloadTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    VANILLA("游戏下载", Icons.Rounded.VideogameAsset),
    MODS("模组下载", Icons.Rounded.Extension),
    MANAGER("下载管理", Icons.Rounded.Download)
}

class DownloadScreen : TabScreen {
    override val index = 1

    @Composable
    override fun Content() {
        val appConfig = LocalAppConfig.current


        var currentTab by remember { mutableStateOf(DownloadTab.VANILLA) }



        Row(modifier = Modifier.fillMaxSize()) {
            DownloadSideBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )

            VerticalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp,
                modifier = Modifier.fillMaxHeight()
            )

            Box(
                modifier = Modifier.fillMaxSize().background(Color(appConfig.subColor))
            ) {
                when (currentTab) {
                    DownloadTab.VANILLA -> {
                        // 【你原来在 DownloadScreen 里写的那一大坨加载游戏版本的代码和卡片，统统塞到这个分支或者抽成一个新组件 VanillaDownloadContent()】
                        VanillaDownloadContent(Modifier.align(Alignment.BottomEnd))
                    }

                    DownloadTab.MODS -> {
                        // TODO: 模组下载页面（可能是 Modrinth / CurseForge 的接入）
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Mod 下载功能开发中...", style = MaterialTheme.typography.titleLarge)
                        }
                    }

                    DownloadTab.MANAGER -> {
                        // TODO: 任务管理页面（显示下载进度条等）
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("下载管理器开发中...", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }


        }
    }

    @Composable
    fun VanillaDownloadContent(btnModifier: Modifier) {
        val navigator = LocalNavigator.currentOrThrow

        var versions by remember { mutableStateOf<List<Version>>(emptyList()) }
        var selected by remember { mutableStateOf<Version?>(null) }

        var loading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        var query by remember { mutableStateOf("") }
        var onlyRelease by remember { mutableStateOf(true) }

        val filtered = remember(versions, query, onlyRelease) {
            versions.asSequence()
                .filter { v ->
                    val okType = if (onlyRelease) v.type == "release" else true
                    val okQuery = if (query.isBlank()) true else v.id.contains(query.trim(), ignoreCase = true)
                    okType && okQuery
                }
                .toList()
        }

        fun refresh() {
            loading = true
            error = null
        }

        LaunchedEffect(loading) {
            if (!loading) return@LaunchedEffect
            runCatching {
                DownloadManager.getVersionList()
            }.onSuccess { list ->
                versions = list
//                if (selected == null) selected = list.firstOrNull()
            }.onFailure { e ->
                error = e.message ?: "未知错误"
            }
            loading = false
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp, 0.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // 顶部操作条：刷新 / 过滤
            VersionSearchBar(
                query = query,
                onQueryChange = { query = it },
                onlyRelease = onlyRelease,
                onOnlyReleaseChange = { onlyRelease = !onlyRelease },
                loading = loading,
                filtered = filtered,
                selected = selected,
                navigator = navigator
            )

            // 错误提示
            if (error != null) {
                AssistChip(
                    onClick = { /* no-op */ },
                    label = { Text("加载失败：$error") },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.onErrorContainer,
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                )
            }

            // 列表
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "正在获取版本清单...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (versions.isEmpty()) "还没有加载版本列表，点一下刷新吧。" else "没有匹配的版本。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filtered, key = { it.id + it.time }) { v ->
                                val isSelected = selected?.id == v.id && selected?.time == v.time

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    } else {
                                        Color.Transparent
                                    },
                                    onClick = { selected = v }
                                ) {
                                    ListItem(
                                        headlineContent = {
                                            Text(
                                                text = v.id,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        supportingContent = {
                                            Text(
                                                text = "${v.type} · releaseTime=${v.releaseTime}",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        trailingContent = {
                                            if (isSelected) {
                                                Text(
                                                    "已选中",
                                                    color = MaterialTheme.colorScheme.primary,
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                }

                                HorizontalDivider()
                            }
                        }
                    }
                }
            }

            // 底部：当前选择信息
            SelectedInfoCard(selected)
        }

        // 右下角浮动刷新按钮（方便一点）
        FloatingActionButton(
            onClick = { refresh() },
            modifier = btnModifier.padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = "刷新")
        }

        // 初次进入自动刷新一次
        LaunchedEffect(Unit) {
            if (versions.isEmpty() && !loading) {
                refresh()
            }
        }
    }
}