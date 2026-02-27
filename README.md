<div align="center">

# 🌠 EchoLauncher

**基于 Jetpack Compose Desktop 打造的现代化、丝滑流畅的 Minecraft 启动器**

[![Kotlin Version](https://img.shields.io/badge/Kotlin-2.0.0--RC1-blueviolet.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Desktop-4285F4?logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/lp/compose-multiplatform/)

[//]: # ([![License]&#40;https://img.shields.io/badge/License-GPL%203.0-green.svg&#41;]&#40;LICENSE&#41;)
[//]: # ([![Platform]&#40;https://img.shields.io/badge/Platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey.svg&#41;]&#40;#&#41;)

</div>

---

## TODO
- [ ] 游戏、mod加载器一键下载
- [ ] 版本隔离与配置管理
- [ ] 正版登录/第三方登录
- [ ] Modrinth / CurseForge 模组、整合包浏览与安装
- [ ] 基于版本智能选择/安装Java环境
- [ ] 崩溃日志 AI 诊断助理
- [ ] 基于玩家皮肤的动态主题
- [ ] 自定义主题
- [ ] P2P 联机与配置同步

[//]: # (## 💡 简介)

[//]: # ()
[//]: # (**EchoLauncher** 是一款旨在打破传统、追求极致视觉体验与运行效率的第三方 Minecraft 启动器。)

[//]: # (采用新一代声明式 UI 框架 **Jetpack Compose Desktop** 编写，天生具备丝滑的动画过渡与极低的渲染开销。结合 Kotlin 协程与 Ktor 异步网络引擎，为您带来快如闪电的下载与启动体验。)

<br>

[//]: # (## ✨ 核心特性)

[//]: # ()
[//]: # (- 🎨 **极致优雅的现代化 UI**)

[//]: # (    - 完全遵循 Material Design 3 设计规范。)

[//]: # (    - 支持 **主题颜色动态取色**与深度自定义，告别千篇一律的界面。)

[//]: # (    - 极其流畅的页面切换动画与交互反馈。)

[//]: # (- 🚀 **极速并发下载**)

[//]: # (    - 基于 `Ktor` + 协程的高并发下载引擎，智能补全缺失的 Libraries 与 Assets。)

[//]: # (    - 原生级多线程解压，拒绝启动时的漫长等待。)

[//]: # (- 🛡️ **安全的隔离与探测**)

[//]: # (    - 默认支持 **版本隔离**，让你的 Mods 和 Saves 井水不犯河水。)

[//]: # (    - 智能的 **Java 环境探测器**，一键扫描本机所有可用 JDK。)

[//]: # (- 🩺 **智能崩溃诊断** *&#40;即将到来/开发中&#41;*)

[//]: # (    - 游戏崩溃不再只是一堆看不懂的乱码。Echo 会在崩溃时第一时间捕获 Exit Code，并提取日志特征为您提供人类可读的修复建议。)

[//]: # (- 📦 **Modrinth / CurseForge 集成** *&#40;开发中&#41;*)

[//]: # (    - 原生集成的 Mod 浏览与管理界面，一键下载安装。)

<br>

[//]: # (## 🛠️ 技术栈)

[//]: # ()
[//]: # (EchoLauncher 是一个供 Kotlin 与 Compose 爱好者学习和探索的绝佳范例。本项目深度应用了以下技术：)

[//]: # ()
[//]: # (- **核心语言:** [Kotlin]&#40;https://kotlinlang.org/&#41;)

[//]: # (- **UI 框架:** [Compose Multiplatform &#40;Desktop&#41;]&#40;https://www.jetbrains.com/lp/compose-multiplatform/&#41;)

[//]: # (- **路由导航:** [Voyager]&#40;https://voyager.adriel.cafe/&#41;)

[//]: # (- **网络与并发:** [Ktor]&#40;https://ktor.io/&#41; + [Kotlinx.coroutines]&#40;https://github.com/Kotlin/kotlinx.coroutines&#41;)

[//]: # (- **JSON 解析:** [Kotlinx.serialization]&#40;https://github.com/Kotlin/kotlinx.serialization&#41;)

[//]: # (- **日志系统:** [Kotlin-logging]&#40;https://github.com/oshai/kotlin-logging&#41; + [Logback]&#40;https://logback.qos.ch/&#41;)

[//]: # ()
[//]: # (<br>)

[//]: # (## 📥 下载与安装)

[//]: # ()
[//]: # (### 玩家用户)

[//]: # (前往 [Releases 页面]&#40;https://github.com/Echomirix/EchoLauncher/releases&#41; 下载适用于你操作系统的打包程序：)

[//]: # (- **Windows:** `.exe` 或 `.msi`)

[//]: # (- **macOS:** `.dmg`)

[//]: # (- **Linux:** `.deb`)

[//]: # ()
[//]: # (> **注意：** 运行 EchoLauncher 本身自带精简的 JRE 环境，无需额外安装 Java 即可打开启动器。但在启动 Minecraft 时，你仍需要为游戏指定合适的 Java 版本。)

### 开发者编译
如果你想从源码构建或修改本项目：

1. 克隆仓库：
   ```bash
   git clone https://github.com/Echomirix/EchoLauncher.git
   cd EchoLauncher
   ```
2. 使用 Gradle 运行（自动热重载）：
   ```bash
   ./gradlew run
   ```
3. 打包为本地可执行文件：
   ```bash
   ./gradlew packageDistributionForCurrentOS
   ```

<br>