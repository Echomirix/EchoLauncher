package cn.echomirix.echolauncher.ui.Screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cn.echomirix.echolauncher.core.config.LocalAppConfig
import cn.echomirix.echolauncher.core.download.Version
import cn.echomirix.echolauncher.data.InstallOptions
import cn.echomirix.echolauncher.data.enabledState
import cn.echomirix.echolauncher.data.loaderSummary


interface TabScreen : Screen {
    val index: Int
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