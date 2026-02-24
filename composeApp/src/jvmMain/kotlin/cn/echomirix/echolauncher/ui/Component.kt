package cn.echomirix.echolauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Minimize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import cn.echomirix.echolauncher.core.config.AppConfig


object Component {

    @Composable
    fun LauncherNavBar(
        currentScreen: Screen,
        onScreenSelected: (Screen) -> Unit
    ) {
        NavigationBar(
            modifier = Modifier.fillMaxWidth().height(AppConfig.NAVBAR_HEIGHT.dp).background(MaterialTheme.colorScheme.surface),
        ) {
            Screen.entries.forEach { s ->
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
        onMinimize: () -> Unit
    ) {
        WindowDraggableArea(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary),
            content = {
                Row(
                    modifier = Modifier.fillMaxWidth().height(AppConfig.TOPBAR_HEIGHT.dp),
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
}