package cn.echomirix.echolauncher.util

/**
 * 将数字格式化为易读形式
 * 例如：
 *
 * 1500 -> "1.5k"
 * 
 * 2000000 -> "2.0M"
 *
 * @return 格式化后的字符串
 */
fun Int.formatNumber(): String {
    return when {
        this >= 1_000_000 -> String.format("%.1fM", this / 1_000_000.0)
        this >= 1_000 -> String.format("%.1fk", this / 1_000.0)
        else -> this.toString()
    }
}

/**
 * 将字符串首字母大写。
 *
 * @return 首字母大写的字符串
 */
// 辅助函数：首字母大写
fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}