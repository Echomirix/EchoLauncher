package cn.echomirix.echolauncher.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.echomirix.echolauncher.core.LaunchManager
import cn.echomirix.echolauncher.core.account.AccountType
import cn.echomirix.echolauncher.core.account.microsoft.LocalAuthServer
import cn.echomirix.echolauncher.core.account.microsoft.MicrosoftAuthService
import cn.echomirix.echolauncher.core.config.AppConstant
import cn.echomirix.echolauncher.core.config.LauncherConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChangeAccountDialog(
    appConfig: LauncherConfig,
    onDismiss: () -> Unit,
    onConfirm: (tempName: String, tempType: AccountType) -> Unit
) {
    var tempName by remember { mutableStateOf(appConfig.playerName) }
    var tempType by remember { mutableStateOf(appConfig.accountType) }
    var accountMenuExpanded by remember { mutableStateOf(false) }
    var isAuthenticating by remember { mutableStateOf(false) }
    var isLoginException by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("账号切换与管理", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 下拉菜单选类型
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = when (tempType) {
                            AccountType.OFFLINE -> "离线模式"; AccountType.LITTLESKIN -> "LittleSkin (TODO)"; else -> "Microsoft"
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
                }
                else if (tempType == AccountType.MICROSOFT) {
                    var exception : Exception? by remember { mutableStateOf(null) }
                    Button(
                        onClick = {
                            isAuthenticating = true
                            coroutineScope.launch {
                                try {
                                    // 1. 启动本地服务器，调起浏览器，并等待玩家拖拽完成！
                                    val code = withContext(Dispatchers.IO) {
                                        LocalAuthServer.startAndGetCode()
                                    }

                                    // 2. 拿到 code 了，直接去请求微软服务器！
                                    val profile = MicrosoftAuthService.authenticate(code, AppConstant.HttpClient)

                                    println("登录成功：${profile.name}")
                                    onDismiss() // 关闭弹窗

                                } catch (e: Exception) {
                                    isLoginException = true
                                    exception = e
                                    println("登录失败或取消: ${e.message}")
                                } finally {
                                    isAuthenticating = false
                                }
                            }
                        },
                        enabled = !isAuthenticating
                    ) {
                        Text(if (isAuthenticating) "请在浏览器中完成验证..." else "微软登录")
                    }
                    if (isLoginException) {
                        Text(
                            text = "微软登录失败，可能是登录过程中取消了授权，或者发生了网络异常。\n错误详情: ${exception?.message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }else {
                    Text(
                        text = "该登录方式核心逻辑暂未实现，请切回离线模式！",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
                Button(onClick = { onConfirm(tempName, tempType) }, enabled = tempType == AccountType.OFFLINE) {
                    Text("保存并使用")
                }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun GlobalGameDialogs() {
    val crashReports by LaunchManager.crashReports.collectAsState()

    // 如果有崩溃报告，我们一次只显示第一个（队列机制）
    val currentReport = crashReports.firstOrNull()

    if (currentReport != null) {
        AlertDialog(
            onDismissRequest = {
                // 当 dismissOnClickOutside 为 false 时，这里主要响应的是系统返回键（Esc等）
                LaunchManager.dismissCrashReport(currentReport.id)
            },
            // 关键修改：禁止点击外部关闭弹窗
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnClickOutside = false,
                dismissOnBackPress = false // 如果你想连 Esc 键也禁用，可以加上这行
            ),
            title = {
                Text("游戏非正常退出", color = MaterialTheme.colorScheme.error)
            },
            text = {
                Column {
                    Text("很抱歉，您的游戏 [${currentReport.versionName}] 崩溃了。")
                    Text("崩溃描述：${currentReport.description ?: "无"}")
                    Text("退出代码：${currentReport.exitCode}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("通常这是由 Mod 冲突、内存不足或 Java 崩溃引起的。")
                }
            },
            confirmButton = {
                Button(onClick = {
                    LaunchManager.dismissCrashReport(currentReport.id)
                }) {
                    Text("我知道了")
                }
            },
            dismissButton = {
                if (currentReport.logFile != null && currentReport.logFile.exists()) {
                    OutlinedButton(onClick = {
                        try {
                            java.awt.Desktop.getDesktop().open(currentReport.logFile)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }) {
                        Text("查看日志文件")
                    }
                }
            }
        )
    }
}