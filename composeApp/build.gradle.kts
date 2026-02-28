import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    kotlin("plugin.serialization") version "2.3.0"
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            val ktorVersion = "2.3.12"
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            // commonMain.dependencies 中：
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

            // Ktor 网络库（下载和API请求）
            implementation("io.ktor:ktor-client-core:${ktorVersion}")
            implementation("io.ktor:ktor-client-content-negotiation:${ktorVersion}")
            implementation("io.ktor:ktor-serialization-kotlinx-json:${ktorVersion}")
            implementation("io.ktor:ktor-client-okhttp:${ktorVersion}")
// Server 端
            implementation("io.ktor:ktor-server-core:${ktorVersion}")
            implementation("io.ktor:ktor-server-netty:${ktorVersion}")

            // commonMain.dependencies 中：
            implementation("cafe.adriel.voyager:voyager-navigator:1.0.0")
            implementation("cafe.adriel.voyager:voyager-screenmodel:1.0.0")
            implementation("cafe.adriel.voyager:voyager-transitions:1.0.0")
            implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
            implementation("org.slf4j:slf4j-api:2.0.13")
            implementation("ch.qos.logback:logback-classic:1.5.6")
            // commonMain.dependencies 中：
            implementation("io.coil-kt.coil3:coil-compose:3.0.0-alpha06")
            implementation("io.coil-kt.coil3:coil-network-ktor:3.0.0-alpha06")
        }
    }
}


compose.desktop {
    application {
        mainClass = "cn.echomirix.echolauncher.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
            packageName = "cn.echomirix.echolauncher"
            packageVersion = "1.0.0"
            modules("java.naming", "jdk.unsupported", "java.management")
        }

/*        buildTypes.release.proguard {
            // 让 ProGuard 读取刚才创建的规则文件
            configurationFiles.from(project.file("proguard-rules.pro"))
        }*/
    }
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
