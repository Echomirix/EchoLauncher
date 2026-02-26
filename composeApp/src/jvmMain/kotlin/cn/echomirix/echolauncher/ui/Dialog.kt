package cn.echomirix.echolauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.echomirix.echolauncher.core.account.AccountType
import cn.echomirix.echolauncher.core.config.LauncherConfig

@Composable
fun ChangeAccountDialog(
    appConfig: LauncherConfig,
    onDismiss: () -> Unit,
    onConfirm: (tempName: String, tempType: AccountType) -> Unit
) {
    var tempName by remember { mutableStateOf(appConfig.playerName) }
    var tempType by remember { mutableStateOf(appConfig.accountType) }
    var accountMenuExpanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("账号切换与管理", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 下拉菜单选类型
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = when (tempType) {
                            AccountType.OFFLINE -> "离线模式"; AccountType.LITTLESKIN -> "LittleSkin (TODO)"; else -> "Microsoft (TODO)"
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
                } else {
                    Text(
                        text = "该登录方式核心逻辑暂未实现，请切回离线模式！",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(tempName, tempType) }) {
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