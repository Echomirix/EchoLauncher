package cn.echomirix.echolauncher.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(val title: String, val icon: ImageVector) {
    HOME("主页", Icons.Rounded.Home),
    DOWNLOAD("下载", Icons.Rounded.Download),
    SETTINGS("设置", Icons.Rounded.Settings),
    ABOUT("关于", Icons.Rounded.Info)
}