package cn.echomirix.echolauncher.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import cafe.adriel.voyager.navigator.Navigator
import cn.echomirix.echolauncher.core.LaunchState
import cn.echomirix.echolauncher.core.LaunchStatus
import cn.echomirix.echolauncher.core.account.AccountType
import cn.echomirix.echolauncher.core.config.AppConstant
import cn.echomirix.echolauncher.core.config.LauncherConfig
import cn.echomirix.echolauncher.core.download.Version
import cn.echomirix.echolauncher.core.version.LocalVersion
import cn.echomirix.echolauncher.ui.Screen.TabScreen
import cn.echomirix.echolauncher.ui.Screen.VersionInstallOptionsScreen


@Composable
fun LauncherNavBar(
    currentScreen: Screens,
    onScreenSelected: (Screens) -> Unit
) {
    NavigationBar(
        modifier = Modifier.fillMaxWidth().height(AppConstant.NAVBAR_HEIGHT.dp)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Screens.entries.filter { it != Screens.UNKNOWN }.forEach { s ->
            NavigationBarItem(
                selected = currentScreen == s,
                onClick = { onScreenSelected(s) },
                label = { Text(text = s.title) },
                icon = { Icon(imageVector = s.icon, contentDescription = s.title) }
            )
        }
    }
}

@Composable
fun WindowScope.CustomTopBar(
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    color: Color,
) {
    WindowDraggableArea(
        modifier = Modifier.fillMaxWidth().background(color),
        content = {
            Row(
                modifier = Modifier.fillMaxWidth().height(AppConstant.TOPBAR_HEIGHT.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onMinimize) {
                    Icon(Icons.Rounded.Minimize, contentDescription = "最小化", tint = Color.White)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = Color.White)
                }
            }
        }
    )
}

@Composable
fun DirectionalTabTransition(navigator: Navigator) {
    AnimatedContent(
        targetState = navigator.lastItem,
        transitionSpec = {
            val initialIsTab = initialState is TabScreen
            val targetIsTab = targetState is TabScreen

            // Tab <-> Tab：保持你原来的左右滑动
            if (initialIsTab && targetIsTab) {
                val initialIndex = (initialState as TabScreen).index
                val targetIndex = (targetState as TabScreen).index
                val direction = if (targetIndex > initialIndex) 1 else -1

                (slideInHorizontally { width -> direction * width } + fadeIn()) togetherWith
                        (slideOutHorizontally { width -> -direction * width } + fadeOut())
            } else {
                // Tab -> Screen：从上到下覆盖切入（像“下拉面板”盖住 Tab）
                // Screen -> Tab：从下到上抽出（像“面板”被拉回去）
                val isTabToScreen = initialIsTab
                val isScreenToTab = !initialIsTab && targetIsTab

                when {
                    isTabToScreen -> {
                        // 新页面从上往下盖住
                        (slideInVertically { fullHeight -> -fullHeight } + fadeIn()) togetherWith
                                (fadeOut())
                    }

                    isScreenToTab -> {
                        // Tab 从下往上“抽出”出现（旧 Screen 向上退场）
                        fadeIn() togetherWith
                                (slideOutVertically { fullHeight -> -fullHeight } + fadeOut())
                    }

                    else -> {
                        // Screen -> Screen：当作同层 push/pop（默认：从下往上推入，往下退出）
                        (slideInVertically { fullHeight -> fullHeight } + fadeIn()) togetherWith
                                (slideOutVertically { fullHeight -> -fullHeight } + fadeOut())
                    }
                }
            }
        },
        label = "DirectionalTabTransition"
    ) { screen ->
        navigator.saveableState("transition", screen) {
            screen.Content()
        }
    }
}

@Composable
fun UserProfileCard(appConfig: LauncherConfig, onAccountClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(20))
                .background(Color(appConfig.primaryColor)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Person,
                contentDescription = "玩家头像",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appConfig.playerName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            val typeText = when (appConfig.accountType) {
                AccountType.OFFLINE -> "离线账户"
                AccountType.LITTLESKIN -> "LittleSkin"
                else -> "Microsoft"
            }
            Text(
                text = typeText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onAccountClick) {
            Icon(Icons.Rounded.SwitchAccount, contentDescription = "切换账号")
        }
    }
}

@Composable
fun LaunchActionArea(
    versions: List<LocalVersion>,
    selectedVersion: LocalVersion?,
    launchStatus: LaunchStatus,
    onVersionSelected: (LocalVersion) -> Unit,
    onLaunch: () -> Unit
) {
    var isVersionMenuExpanded by remember { mutableStateOf(false) }
    AnimatedContent(
        targetState = launchStatus.state,
        transitionSpec = {
            (fadeIn() + slideInVertically { height -> height / 2 }) togetherWith
                    (fadeOut() + slideOutVertically { height -> -height / 2 })
        },
        label = "LaunchStateAnimation",
        modifier = Modifier.fillMaxWidth()
    ) { state ->
        when (state) {
            // 状态A：闲置或报错 (展示下拉框和启动按钮)
            LaunchState.IDLE, LaunchState.ERROR -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 报错提示
                    if (state == LaunchState.ERROR) {
                        Text(
                            text = launchStatus.text,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // 版本信息卡片 + 下拉菜单组合
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            onClick = {
                                if (versions.isNotEmpty()) {
                                    isVersionMenuExpanded = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.VideogameAsset,
                                    contentDescription = "Version Icon",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedVersion?.id
                                            ?: if (versions.isEmpty()) "未找到版本" else "加载中...",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = selectedVersion?.type
                                            ?: "请确保 versions 目录下有文件",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(onClick = { /* TODO: 版本设置 */ }) {
                                    Icon(Icons.Rounded.Settings, contentDescription = "设置")
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = isVersionMenuExpanded,
                            onDismissRequest = { isVersionMenuExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            versions.forEach { ver ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = ver.id,
                                            fontWeight = if (ver.id == selectedVersion?.id) FontWeight.Bold else FontWeight.Normal,
                                            color = if (ver.id == selectedVersion?.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = { onVersionSelected(ver) }
                                )
                            }
                        }
                    }

                    // 启动按钮
                    Button(
                        onClick = onLaunch,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Play")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "开始游戏",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 状态B：校验中或启动中
            LaunchState.CHECKING, LaunchState.STARTING -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = launchStatus.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 状态C：启动成功
            LaunchState.SUCCESS -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Success",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = launchStatus.text,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun VersionSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onlyRelease: Boolean,
    onOnlyReleaseChange: () -> Unit,
    filtered: List<Version>,
    selected: Version?,
    loading: Boolean,
    navigator: Navigator,

    ) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            OutlinedTextField(
                value = query,
                onValueChange = { onQueryChange(query) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "搜索") },
                label = { Text("搜索版本号 (例如 1.20.1)") },
                singleLine = true
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 左侧组
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = onlyRelease,
                        onClick = onOnlyReleaseChange ,
                        label = { Text("只看 release") }
                    )
                    Text(
                        text = "共 ${filtered.size} 个版本",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 右侧组
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
//                                Button(
//                                    enabled = !loading,
//                                    onClick = { refresh() }
//                                ) {
//                                    Icon(Icons.Rounded.Refresh, contentDescription = "刷新")
//                                    Spacer(Modifier.width(8.dp))
//                                    Text("刷新版本列表")
//                                }

                    FilledTonalButton(
                        enabled = selected != null && !loading,
                        onClick = {
                            val v = selected ?: return@FilledTonalButton
                            navigator.push(VersionInstallOptionsScreen(v))
                        }
                    ) {
                        Icon(Icons.Rounded.SystemUpdateAlt, contentDescription = "下载")
                        Spacer(Modifier.width(8.dp))
                        Text("下载所选版本")
                    }
                }
            }


        }
    }
}

@Composable
fun SelectedInfoCard(
    selected: Version?
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(6.dp)) {
            Text(
                text = "当前选择",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = selected?.id ?: "未选择",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = selected?.let { "${it.type} · ${it.releaseTime}" } ?: "请先选择一个版本",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ColorSettingRow(
    label: String,
    colorValue: Long,
    onColorChange: (Long) -> Unit
) {
    // 绑定文本和展开状态
    var localHex by remember(colorValue) {
        mutableStateOf(colorValue.toString(16).padStart(8, '0').uppercase())
    }
    var expanded by remember { mutableStateOf(false) }

    val currentColor = Color(colorValue)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = localHex,
                onValueChange = { localHex = it },
                modifier = Modifier.weight(1f).onFocusChanged { focus ->
                    if (!focus.isFocused) {
                        val parsed = localHex.toLongOrNull(16)
                        if (parsed != null) {
                            onColorChange(parsed)
                        } else {
                            localHex = colorValue.toString(16).padStart(8, '0').uppercase()
                        }
                    }
                },
                label = { Text(label) },
                leadingIcon = { Text("#") },
                singleLine = true
            )

            Spacer(Modifier.width(16.dp))

            // 颜色预览与取色器开关
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(currentColor)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .clickable { expanded = !expanded },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.Palette,
                    contentDescription = "调色板",
                    // 根据背景亮度决定图标是黑色还是白色，防止看不清
                    tint = if (currentColor.luminance() > 0.5f) Color.Black else Color.White
                )
            }
        }

        // 展开的色条区域
        AnimatedVisibility(visible = expanded) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {

                    // 内部辅助函数：单条 Slider
                    @Composable
                    fun ColorSlider(title: String, value: Float, trackColor: Color, onValueChange: (Float) -> Unit) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(title, modifier = Modifier.width(24.dp), style = MaterialTheme.typography.labelMedium)
                            Slider(
                                value = value,
                                onValueChange = onValueChange,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = trackColor,
                                    activeTrackColor = trackColor.copy(alpha = 0.7f)
                                )
                            )
                            Text(
                                text = (value * 255).toInt().toString(),
                                modifier = Modifier.width(32.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    // 统一更新方法
                    val updateColor = { r: Float, g: Float, b: Float, a: Float ->
                        val newColor = Color(red = r, green = g, blue = b, alpha = a)
                        // 将 Compose Color 转回 Long (ARGB)
                        val argbLong = newColor.toArgb().toLong() and 0xFFFFFFFFL
                        onColorChange(argbLong)
                    }

                    ColorSlider("R", currentColor.red, Color.Red) {
                        updateColor(
                            it,
                            currentColor.green,
                            currentColor.blue,
                            currentColor.alpha
                        )
                    }
                    ColorSlider("G", currentColor.green, Color.Green) {
                        updateColor(
                            currentColor.red,
                            it,
                            currentColor.blue,
                            currentColor.alpha
                        )
                    }
                    ColorSlider("B", currentColor.blue, Color.Blue) {
                        updateColor(
                            currentColor.red,
                            currentColor.green,
                            it,
                            currentColor.alpha
                        )
                    }
                    ColorSlider("A", currentColor.alpha, Color.Gray) {
                        updateColor(
                            currentColor.red,
                            currentColor.green,
                            currentColor.blue,
                            it
                        )
                    }
                }
            }
        }
    }
}