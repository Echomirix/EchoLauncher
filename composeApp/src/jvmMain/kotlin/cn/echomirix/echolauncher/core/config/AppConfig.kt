package cn.echomirix.echolauncher.core.config

import java.awt.Dimension

object AppConfig {
    const val APP_NAME = "EchoLauncher"
    const val APP_VERSION = "1.0.0"

    const val WINDOW_WIDTH_DEFAULT = 800
    const val WINDOW_HEIGHT_DEFAULT = 550

    const val WINDOW_MIN_WIDTH = 600
    const val WINDOW_MIN_HEIGHT = 450
    val WINDOW_MIN_SIZE = Dimension(WINDOW_MIN_WIDTH, WINDOW_MIN_HEIGHT)

    const val WINDOW_ROUND_CORNER_RADIUS = 16
    const val TOPBAR_HEIGHT = 48
    const val NAVBAR_HEIGHT = 60

    const val UI_DEFAULT_PADDING = 10
    const val CARD_DEFAULT_PADDING_HORIZONTAL = 40
    const val CARD_DEFAULT_PADDING_VERTICAL = 30
}