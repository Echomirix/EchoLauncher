package cn.echomirix.echolauncher.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cn.echomirix.echolauncher.core.config.LocalAppConfig
import cn.echomirix.echolauncher.core.download.DownloadManager
import cn.echomirix.echolauncher.core.download.Version
import cn.echomirix.echolauncher.core.mod.LoaderType
import cn.echomirix.echolauncher.core.mod.ModSearchManager
import cn.echomirix.echolauncher.core.mod.modrinth.Hit
import cn.echomirix.echolauncher.ui.DownloadSideBar
import cn.echomirix.echolauncher.ui.SelectedInfoCard
import cn.echomirix.echolauncher.ui.VersionSearchBar
import cn.echomirix.echolauncher.util.capitalize
import cn.echomirix.echolauncher.util.formatNumber
import coil3.compose.rememberAsyncImagePainter
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch

enum class DownloadTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    VANILLA("游戏下载", Icons.Rounded.VideogameAsset),
    MODS("模组下载", Icons.Rounded.Extension),
    MANAGER("下载管理", Icons.Rounded.Download)
}

class DownloadScreen : TabScreen {
    override val index = 1
    private val logger = KotlinLogging.logger {}

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
                thickness = 0.6.dp,
                modifier = Modifier.fillMaxHeight()
            )

            Box(
                modifier = Modifier.fillMaxSize().background(Color(appConfig.subColor))
            ) {
                when (currentTab) {
                    DownloadTab.VANILLA -> {
                        VanillaDownloadContent(Modifier.align(Alignment.BottomEnd))
                    }

                    DownloadTab.MODS -> {
                        // TODO: 模组下载页面（可能是 Modrinth / CurseForge 的接入）
                        ModDownloadContent()
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ModDownloadContent() {
        val navigator = LocalNavigator.currentOrThrow
        val coroutineScope = rememberCoroutineScope()

        // 搜索栏状态
        var query by remember { mutableStateOf("") }
        var version by remember { mutableStateOf("") }
        var selectedLoader by remember { mutableStateOf(cn.echomirix.echolauncher.core.mod.LoaderType.UNKNOWN) }
        var loaderMenuExpanded by remember { mutableStateOf(false) }

        // 核心页面状态
        var loading by remember { mutableStateOf(false) }
        var isLoadMore by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }

        // 分页与数据状态
        var currentPage by remember { mutableStateOf(0) }
        var hasMore by remember { mutableStateOf(true) }
        var result by remember { mutableStateOf<List<Hit>>(emptyList()) }
        var selected by remember { mutableStateOf<Hit?>(null) }

        val listState = rememberLazyListState()

        // 触发搜索的函数
        // isNewSearch: true 代表重新搜索(清空原列表), false 代表加载下一页(追加列表)
        fun performSearch(isNewSearch: Boolean = true) {
            // 如果正在加载（不管是不是加载更多），就不要重复触发
            if (loading || isLoadMore) return

            if (isNewSearch) {
                loading = true
                currentPage = 0
                hasMore = true
                error = null
            } else {
                if (!hasMore) return
                isLoadMore = true
            }

            coroutineScope.launch {
                try {
                    val offset = currentPage * 20
                    val list = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        ModSearchManager.searchMods(
                            query = query,
                            gameVersion = version,
                            loader = selectedLoader,
                            offset = offset
                        )
                    }
                    if (isNewSearch) {
                        result = list
                        // 尝试平滑滚动回顶部，如果列表不为空的话
                        if (result.isNotEmpty()) {
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(50)
                                listState.animateScrollToItem(0) // 瞬间滚回去，或者用 animateScrollToItem
                            }
                        }
                    } else {
                        // 加载更多：拼接列表
                        result = result + list
                    }

                    // 如果返回数量小于 20，说明见底了，没有更多页了
                    hasMore = list.size == 20
                    if (hasMore) {
                        currentPage++
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    error = e.message ?: "未知网络错误"
                    if (isNewSearch) {
                        result = emptyList()
                    }
                } finally {
                    loading = false
                    logger.info { "搜索完成，结果数量=${result.size}，是否有下一页=$hasMore" }
                    isLoadMore = false
                }
            }
        }

        // 只需要这一个 LaunchedEffect，专门负责首次进入的静默加载
        LaunchedEffect(Unit) {
            if (result.isEmpty()) {
                performSearch(isNewSearch = true)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 顶部搜索操作区
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 搜索名称输入框
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("搜索模组名称") },
                    modifier = Modifier.weight(2f),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { performSearch(isNewSearch = true) }
                    )
                )

                // 版本筛选输入框
                OutlinedTextField(
                    value = version,
                    onValueChange = { version = it },
                    label = { Text("MC 版本") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("如 1.20.1") },
                    singleLine = true,
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { performSearch(isNewSearch = true) }
                    )
                )

                // 加载器下拉选择框
                Box(modifier = Modifier.weight(1.2f)) {
                    OutlinedTextField(
                        value = if (selectedLoader == LoaderType.UNKNOWN) "全部加载器" else selectedLoader.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("加载器") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { loaderMenuExpanded = true }) {
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = "展开")
                            }
                        }
                    )
                    // 透明层接管点击事件
                    Surface(
                        modifier = Modifier.matchParentSize(),
                        color = Color.Transparent,
                        onClick = { loaderMenuExpanded = true }
                    ) {}

                    DropdownMenu(
                        expanded = loaderMenuExpanded,
                        onDismissRequest = { loaderMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("全部加载器") },
                            onClick = {
                                selectedLoader = LoaderType.UNKNOWN
                                loaderMenuExpanded = false
                                performSearch(isNewSearch = true)
                            }
                        )
                        LoaderType.entries.filter { it != LoaderType.UNKNOWN }.forEach { loader ->
                            DropdownMenuItem(
                                text = { Text(loader.name) },
                                onClick = {
                                    selectedLoader = loader
                                    loaderMenuExpanded = false
                                    performSearch(isNewSearch = true)
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = { performSearch(isNewSearch = true) },
                    modifier = Modifier.height(56.dp) // 与 TextField 高度对齐
                ) {
                    Text("搜索")
                }
            }

            // 2. 错误提示
            if (error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "加载失败：$error",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // 3. 模组列表区
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            ) {
                // 如果是全新搜索且正在加载，显示全屏转圈
                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text("正在从 Modrinth 获取数据...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else if (result.isEmpty() && error == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("未找到相关模组，换个关键词或加载器试试？", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(result, key = { it.project_id }) { mod ->
                            val isSelected = selected?.project_id == mod.project_id

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selected = mod },
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                tonalElevation = if (isSelected) 4.dp else 1.dp
                            ) {
                                ListItem(
                                    leadingContent = {
                                        val imageUrl = mod.icon_url.ifBlank { "https://placehold.co/64?text=Mod" }
                                        Image(
                                            painter = rememberAsyncImagePainter(imageUrl),
                                            contentDescription = mod.title,
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    },
                                    headlineContent = {
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                text = mod.title,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = "by ${mod.author}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    supportingContent = {
                                        Column(modifier = Modifier.padding(top = 4.dp)) {
                                            Text(
                                                text = mod.description,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Spacer(Modifier.height(6.dp))

                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                                    Text("⬇ ${mod.downloads.formatNumber()}", color = MaterialTheme.colorScheme.onSecondaryContainer)
                                                }
                                                mod.display_categories.take(2).forEach { category ->
                                                    Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                                                        Text(category.capitalize())
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    trailingContent = {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = "已选中",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                            }
                        }

                        // 列表底部的“加载更多”控制项
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                if (hasMore) {
                                    if (isLoadMore) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    } else {
                                        OutlinedButton(onClick = { performSearch(isNewSearch = false) }) {
                                            Text("加载下一页")
                                        }
                                    }
                                } else {
                                    Text("已经到底啦~", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun VanillaDownloadContent(btnModifier: Modifier) {
        val navigator = LocalNavigator.currentOrThrow
        val coroutineScope = rememberCoroutineScope()

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

        // 触发搜索的函数
        fun refresh() {
            // 如果已经在加载了，就别重复点了
            if (loading) return

            loading = true
            error = null

            coroutineScope.launch {
                try {
                    // 切到 IO 线程
                    val list = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        DownloadManager.getVersionList()
                    }
                    versions = list
                } catch (e: kotlinx.coroutines.CancellationException) {
                    // 页面切走时的正常取消
                    throw e
                } catch (e: Exception) {
                    // 网络错误
                    error = e.message ?: "未知错误"
                    versions = emptyList() // 搜索失败时清空旧数据
                } finally {
                    loading = false
                }
            }
        }

        // 只需要这一个 LaunchedEffect，专门负责首次进入的静默加载
        LaunchedEffect(Unit) {
            if (versions.isEmpty()) {
                refresh()
            }
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