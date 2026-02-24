package cn.echomirix.echolauncher

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import cn.echomirix.echolauncher.core.config.AppConfig
import java.awt.Dimension

fun main() = application {
    val windowState = WindowState(
        size = DpSize(AppConfig.WINDOW_WIDTH_DEFAULT.dp, AppConfig.WINDOW_HEIGHT_DEFAULT.dp),
        position = WindowPosition.Aligned(Alignment.Center),
    )
    Window(
        onCloseRequest = ::exitApplication,
        title = AppConfig.APP_NAME,
        resizable = true,
        state = windowState,
        undecorated = true,
        transparent = true
    ) {
        window.minimumSize = AppConfig.WINDOW_MIN_SIZE
        App(
            onClose = ::exitApplication,
            onMinimize = { windowState.isMinimized = true }
        )
    }
}