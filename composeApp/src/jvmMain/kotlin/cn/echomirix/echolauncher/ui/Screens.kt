package cn.echomirix.echolauncher.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import cafe.adriel.voyager.core.screen.Screen
import cn.echomirix.echolauncher.ui.Screens.entries

fun Screen.fromScreen(): Screens {
    return entries.find { it.screen::class == this::class } ?: Screens.HOME
}

enum class Screens(val title: String, val icon: ImageVector, val screen: Screen) {
    HOME("主页", Icons.Rounded.Home, HomeScreen()),
    DOWNLOAD("下载", Icons.Rounded.Download, DownloadScreen()),
    SETTINGS("设置", Icons.Rounded.Settings, SettingsScreen()),
    ABOUT("关于", Icons.Rounded.Info, AboutScreen());

}