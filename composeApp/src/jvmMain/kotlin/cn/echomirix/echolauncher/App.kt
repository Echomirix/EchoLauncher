package cn.echomirix.echolauncher

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
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import cn.echomirix.echolauncher.core.config.AppConfig
import cn.echomirix.echolauncher.ui.CustomTopBar
import cn.echomirix.echolauncher.ui.DirectionalTabTransition
import cn.echomirix.echolauncher.ui.HomeScreen
import cn.echomirix.echolauncher.ui.LauncherNavBar
import cn.echomirix.echolauncher.ui.fromScreen

@Composable
@Preview
fun WindowScope.App(
    onClose: () -> Unit = {},
    onMinimize: () -> Unit = {}
) {
    MaterialTheme {
        // 1. 把路由的祖宗 Navigator 请到最顶层！
        // 把那个脱裤子放屁的 var currentScreen by remember 给老子删了！
        Navigator(HomeScreen()) { navigator ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(AppConfig.WINDOW_ROUND_CORNER_RADIUS.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(AppConfig.WINDOW_ROUND_CORNER_RADIUS.dp))
            ) {
                Scaffold(
                    topBar = {
                        CustomTopBar(onClose, onMinimize)
                    },
                    bottomBar = {
                        // 2. 状态同步：根据 navigator 当前的页面来决定高亮哪个 Tab
                        // 你自己写个映射，或者直接拿 navigator.lastItem 做判断
                        val currentTab = navigator.lastItem

                        LauncherNavBar(
                            currentScreen = currentTab.fromScreen(),
                            onScreenSelected = { targetTab ->
                                // 3. 狠狠地命令 Navigator 去跳转页面！别再去改什么没用的变量了！
                                // 用 replaceAll 可以防止底部导航栏无限堆叠路由栈
                                navigator.replaceAll(targetTab.screen)
                            }
                        )
                    },
                    containerColor = Color.Transparent
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        // 4. 这里只负责执行转场动画，别再嵌套声明 Navigator 了！
                        DirectionalTabTransition(navigator)
                    }
                }
            }
        }
    }
}