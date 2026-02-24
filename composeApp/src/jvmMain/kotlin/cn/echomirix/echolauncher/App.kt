package cn.echomirix.echolauncher

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import cn.echomirix.echolauncher.core.config.AppConfig
import cn.echomirix.echolauncher.ui.Component
import cn.echomirix.echolauncher.ui.Component.CustomTopBar
import cn.echomirix.echolauncher.ui.Interface
import cn.echomirix.echolauncher.ui.Screen
import cn.echomirix.echolauncher.ui.Screen.*

@Composable
@Preview
fun WindowScope.App(
    onClose: () -> Unit = {},
    onMinimize: () -> Unit = {}
) {
    MaterialTheme {
        var currentScreen by remember { mutableStateOf(HOME) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                // 1. 设置你想要的圆角大小，这里设置为 12.dp (你也可以用 16.dp 等)
                .clip(RoundedCornerShape(AppConfig.WINDOW_ROUND_CORNER_RADIUS.dp))
                // 2. 为裁剪后的区域填充背景颜色
                .background(MaterialTheme.colorScheme.background)
                // 3. (可选) 给窗口加一层极细的边框，让它在白色背景下更有质感
                .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(AppConfig.WINDOW_ROUND_CORNER_RADIUS.dp))
        ) {
            Scaffold(
                topBar = {
                    CustomTopBar(onClose, onMinimize)
                },
                bottomBar = {
                    Component.LauncherNavBar(currentScreen, onScreenSelected = { currentScreen = it })
                },
                containerColor = Color.Transparent
            ) { innerPadding ->
                AnimatedContent(
                    targetState = currentScreen,
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    transitionSpec = {
                        if (targetState.ordinal > initialState.ordinal) {
                            // 上切下 -> 新页面从右侧滑入，老页面向左侧滑出
                            slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) togetherWith
                                    slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth })
                        } else {
                            // 下切上 -> 新页面从左侧滑入，老页面向右侧滑出
                            slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }) togetherWith
                                    slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth })
                        }
                    }) { targetScreen ->
                    when (targetScreen) {
                        HOME -> Interface.HomeScreen()
                        DOWNLOAD -> Interface.DownloadScreen()
                        SETTINGS -> Interface.SettingsScreen()
                        ABOUT -> Interface.AboutScreen()
                    }
                }
            }
        }
    }
}