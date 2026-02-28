package cn.echomirix.echolauncher.core.account.microsoft

/**
 * @see <a href="https://github.com/AlphaBs/XboxAuthNet/blob/main/src/XboxLive/XboxGameTitles.cs">XboxAuthNet</a>
 */
enum class MicrosoftClientId(val id: String) {
    MinecraftNintendoSwitch("00000000441cc96b"),
    MinecraftJava("00000000402b5328"),
    XboxAppIOS("000000004c12ae6f"),
    XboxGamePassIOS("000000004c20a908");
}