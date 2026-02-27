package cn.echomirix.echolauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import cafe.adriel.voyager.navigator.Navigator
import cn.echomirix.echolauncher.core.config.AppConstant
import cn.echomirix.echolauncher.core.config.ConfigManager
import cn.echomirix.echolauncher.core.config.LocalAppConfig
import cn.echomirix.echolauncher.ui.*
import cn.echomirix.echolauncher.ui.screen.HomeScreen

@Composable
@Preview
fun WindowScope.App(
    onClose: () -> Unit = {},
    onMinimize: () -> Unit = {}
) {
    val appConfig by ConfigManager.configFlow.collectAsState()
    CompositionLocalProvider(LocalAppConfig provides appConfig) {
        MaterialTheme {
            Navigator(HomeScreen()) { navigator ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(AppConstant.WINDOW_ROUND_CORNER_RADIUS.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .border(
                            1.dp,
                            Color.Gray.copy(alpha = 0.2f),
                            RoundedCornerShape(AppConstant.WINDOW_ROUND_CORNER_RADIUS.dp)
                        )
                ) {
                    Scaffold(
                        topBar = {
                            CustomTopBar(onClose, onMinimize, Color(appConfig.primaryColor))
                        },
                        bottomBar = {
                            val currentTab = navigator.lastItem

                            LauncherNavBar(
                                currentScreen = currentTab.fromScreen(),
                                onScreenSelected = { targetTab ->
                                    navigator.replaceAll(targetTab.screen)
                                }
                            )
                        },
                        containerColor = Color.Transparent
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            DirectionalTabTransition(navigator)
                        }
                    }

                    GlobalGameDialogs()
                }
            }
        }
    }
}