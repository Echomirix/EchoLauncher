package cn.echomirix.echolauncher.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import cafe.adriel.voyager.core.screen.Screen
import cn.echomirix.echolauncher.core.config.LocalAppConfig


public interface TabScreen : Screen {
    val index: Int
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