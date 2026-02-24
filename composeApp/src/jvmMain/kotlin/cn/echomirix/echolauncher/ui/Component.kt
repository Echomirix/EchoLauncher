package cn.echomirix.echolauncher.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowLeft
import androidx.compose.material.icons.rounded.ArrowLeft
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
import cafe.adriel.voyager.navigator.Navigator
import cn.echomirix.echolauncher.core.config.AppConfig


@Composable
fun LauncherNavBar(
    currentScreen: Screens,
    onScreenSelected: (Screens) -> Unit
) {
    NavigationBar(
        modifier = Modifier.fillMaxWidth().height(AppConfig.NAVBAR_HEIGHT.dp).background(MaterialTheme.colorScheme.surface),
    ) {
        Screens.entries.forEach { s ->
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

// 3. 核心大招：手搓一个专属于平级 Tab 的高级路由转场组件！
// 抛弃官方那个只懂 Push/Pop 的 SlideTransition！
@Composable
fun DirectionalTabTransition(navigator: Navigator) {
    AnimatedContent(
        targetState = navigator.lastItem,
        transitionSpec = {
            // 拿到当前页面和目标页面的索引，要是强转失败说明不是 Tab 页，默认索引当 0 算
            val initialIndex = (initialState as? IndexedScreen)?.index ?: 0
            val targetIndex = (targetState as? IndexedScreen)?.index ?: 0

            // 核心灵魂：索引相减判断方向！
            val direction = if (targetIndex > initialIndex) 1 else -1

            (slideInHorizontally { width -> direction * width } + fadeIn()) togetherWith
                    (slideOutHorizontally { width -> -direction * width } + fadeOut())
        },
        label = "DirectionalTabTransition"
    ) { screen ->
        // 交还给 Voyager 去管理页面的生命周期和状态恢复
        navigator.saveableState("transition", screen) {
            screen.Content()
        }
    }
}