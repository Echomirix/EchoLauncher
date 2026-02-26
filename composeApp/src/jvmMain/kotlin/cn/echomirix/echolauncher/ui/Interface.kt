package cn.echomirix.echolauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cn.echomirix.echolauncher.core.LaunchContext
import cn.echomirix.echolauncher.core.LaunchManager
import cn.echomirix.echolauncher.core.LaunchState
import cn.echomirix.echolauncher.core.LaunchStatus
import cn.echomirix.echolauncher.core.account.AccountType
import cn.echomirix.echolauncher.core.config.AppConstant
import cn.echomirix.echolauncher.core.config.ConfigManager
import cn.echomirix.echolauncher.core.config.LocalAppConfig
import cn.echomirix.echolauncher.core.download.DownloadManager
import cn.echomirix.echolauncher.core.download.Version
import cn.echomirix.echolauncher.core.version.LocalVersion
import cn.echomirix.echolauncher.core.version.LocalVersionManager
import cn.echomirix.echolauncher.data.InstallOptions
import cn.echomirix.echolauncher.data.enabledState
import cn.echomirix.echolauncher.data.loaderSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext


interface TabScreen : Screen {
    val index: Int
}

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
                                launchStatus = currentVersionStatus,
                                onLaunch = {
                                    if (selectedVersion != null) {
                                        // 调用新的 LaunchManager 启动游戏
                                        LaunchManager.launch(ctx)
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


class DownloadScreen : TabScreen {
    override val index = 1

    @Composable
    override fun Content() {
        val appConfig = LocalAppConfig.current
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

        Box(
            modifier = Modifier.fillMaxSize().background(Color(appConfig.subColor))
        ) {
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
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = "刷新")
            }
        }

        // 初次进入自动刷新一次
        LaunchedEffect(Unit) {
            if (versions.isEmpty() && !loading) {
                refresh()
            }
        }
    }
}

/**
 * 二级页面：针对某个 Minecraft 版本的“安装/下载选项”页面（Forge/Fabric/Quilt/NeoForge/OptiFine 等）
 * 注意：这是“栈页面”，不是 Tab 平级页面；所以不实现 IndexedScreen。
 */
class VersionInstallOptionsScreen(
    private val version: Version
) : Screen {


    // 把 OptionRow 移到 Content() 的外部，作为类的私有方法
    @Composable
    private fun OptionRow(
        title: String,
        checked: Boolean,
        enabledFlag: Boolean,
        reason: String?, // 将 reason 改为直接传参，消除后面的 magic string
        onChange: (Boolean) -> Unit
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = checked,
                    enabled = enabledFlag || checked,
                    onCheckedChange = onChange
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabledFlag || checked) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.38f
                    )
                )
            }
            if (reason != null) {
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.padding(start = 48.dp)
                )
            }
        }
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val appConfig = LocalAppConfig.current

        var options by remember { mutableStateOf(InstallOptions()) }
        val enabled = remember(options) { enabledState(options) }

        fun set(update: (InstallOptions) -> InstallOptions) {
            options = update(options)
        }

        // 动态生成的安装名称占位符，增强 UX 体验
        val defaultInstallName = remember(options, version.id) {
            buildString {
                append(version.id)
                if (options.forge) append("-Forge")
                if (options.neoForge) append("-NeoForge")
                if (options.fabric) append("-Fabric")
                if (options.quilt) append("-Quilt")
                if (options.optiFine) append("-OptiFine")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(appConfig.subColor))
        ) {
            // 滚动容器：页面内容可能超出窗口
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 顶部栏
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                            }
                            Spacer(Modifier.width(6.dp))
                            Column(Modifier.weight(1f)) {
                                Text("安装选项", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "Minecraft ${version.id}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            FilledTonalButton(
                                onClick = {
                                    // TODO: 在这里触发真正的安装/下载任务
                                },
                                enabled = options.installClient
                            ) {
                                Icon(Icons.Rounded.SystemUpdateAlt, contentDescription = "开始")
                                Spacer(Modifier.width(8.dp))
                                Text("开始安装")
                            }
                        }
                    }
                }

                // 概览
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("目标版本", style = MaterialTheme.typography.titleMedium)
                            Text(version.id, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                text = "类型：${version.type} · releaseTime=${version.releaseTime}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "当前方案：${loaderSummary(options)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Loader 选择（多选 + 动态禁用互斥项）
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Mod / 加载器选项", style = MaterialTheme.typography.titleMedium)

                            OptionRow(
                                title = "Forge",
                                checked = options.forge,
                                enabledFlag = enabled.forge,
                                reason = if (!enabled.forge && !options.forge) "已被其他主加载器排斥" else null
                            ) { options = options.copy(forge = it) }

                            OptionRow(
                                title = "NeoForge",
                                checked = options.neoForge,
                                enabledFlag = enabled.neoForge,
                                reason = if (!enabled.neoForge && !options.neoForge) "已被其他主加载器排斥" else null
                            ) { options = options.copy(neoForge = it) }

                            OptionRow(
                                title = "Fabric",
                                checked = options.fabric,
                                enabledFlag = enabled.fabric,
                                reason = if (!enabled.fabric && !options.fabric) {
                                    if (options.optiFine) "原生不支持与 OptiFine 共存" else "已被其他主加载器排斥"
                                } else null
                            ) { options = options.copy(fabric = it) }

                            OptionRow(
                                title = "Quilt",
                                checked = options.quilt,
                                enabledFlag = enabled.quilt,
                                reason = if (!enabled.quilt && !options.quilt) {
                                    if (options.optiFine) "原生不支持与 OptiFine 共存" else "已被其他主加载器排斥"
                                } else null
                            ) { options = options.copy(quilt = it) }

                            OptionRow(
                                title = "OptiFine",
                                checked = options.optiFine,
                                enabledFlag = enabled.optiFine,
                                reason = if (!enabled.optiFine && !options.optiFine) "不支持在 Fabric/Quilt 环境直接安装" else null
                            ) { options = options.copy(optiFine = it) }

                            if (options.forge || options.neoForge || options.fabric || options.quilt || options.optiFine) {
                                OutlinedTextField(
                                    value = options.loaderVersion,
                                    onValueChange = { v -> set { it.copy(loaderVersion = v) } },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("加载器版本(可选/占位)") },
                                    placeholder = { Text("以后这里可改成下拉列表自动获取") },
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                // 高级设置
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("高级设置", style = MaterialTheme.typography.titleMedium)

                            OutlinedTextField(
                                value = options.customName,
                                onValueChange = { v -> options = options.copy(customName = v) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("安装名称(可选)") },
                                placeholder = { Text(defaultInstallName) },
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = options.javaArgs,
                                onValueChange = { v -> options = options.copy(javaArgs = v) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Java 参数(可选)") },
                                placeholder = { Text("-Xmx2G -Dfile.encoding=UTF-8") },
                                singleLine = true
                            )
                        }
                    }
                }
            }
        }
    }
}

class SettingsScreen : TabScreen {
    override val index = 2

    @Composable
    override fun Content() {
        val appConfig = LocalAppConfig.current

        var draftConfig by remember(appConfig) { mutableStateOf(appConfig.copy()) }

        val isModified = draftConfig != appConfig

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
                // 页面标题
                item {
                    Text(
                        text = "全局设置",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

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
                ExtendedFloatingActionButton(
                    onClick = {
                        // 将草稿一次性覆盖到全局配置
                        ConfigManager.updateConfig { draftConfig.copy() }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp),
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

class AboutScreen : TabScreen {
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