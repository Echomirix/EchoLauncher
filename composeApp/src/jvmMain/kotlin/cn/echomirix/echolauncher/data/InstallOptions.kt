package cn.echomirix.echolauncher.data

data class InstallOptions(
    val forge: Boolean = false,
    val neoForge: Boolean = false,
    val fabric: Boolean = false,
    val quilt: Boolean = false,
    val optiFine: Boolean = false,

    val installClient: Boolean = true,
    val installLibraries: Boolean = true,
    val installAssets: Boolean = true,

    val customName: String = "",
    val javaArgs: String = "-Xmx2G",
    val loaderVersion: String = ""
)

data class EnabledState(
    val forge: Boolean,
    val neoForge: Boolean,
    val fabric: Boolean,
    val quilt: Boolean,
    val optiFine: Boolean,
)

// 精确的互斥逻辑
fun enabledState(o: InstallOptions): EnabledState {
    return EnabledState(
        // 主加载器互斥：一旦选中了另外三种之一，当前的就被禁用
        forge = !o.neoForge && !o.fabric && !o.quilt,
        neoForge = !o.forge && !o.fabric && !o.quilt,

        // Fabric/Quilt 除了和其他主加载器互斥，还和 OptiFine 互斥
        fabric = !o.forge && !o.neoForge && !o.quilt && !o.optiFine,
        quilt = !o.forge && !o.neoForge && !o.fabric && !o.optiFine,

        // OptiFine 仅仅和 Fabric/Quilt 互斥（可以与 Forge/NeoForge 或 原版 共存）
        optiFine = !o.fabric && !o.quilt
    )
}

fun loaderSummary(o: InstallOptions): String {
    val parts = buildList {
        if (o.forge) add("Forge")
        if (o.neoForge) add("NeoForge")
        if (o.fabric) add("Fabric")
        if (o.quilt) add("Quilt")
        if (o.optiFine) add("OptiFine")
    }
    return if (parts.isEmpty()) "Vanilla (原版)" else parts.joinToString(" + ")
}