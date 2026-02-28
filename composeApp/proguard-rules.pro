# 1. 忽略找不到类的警告
-dontwarn jakarta.servlet.**
-dontwarn org.jboss.marshalling.**
-dontwarn io.netty.**
-dontwarn ch.qos.logback.**
-dontwarn org.apache.commons.logging.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.apache.log4j.**
-dontwarn com.aayushatharva.brotli4j.**
-dontwarn com.jcraft.jzlib.**
-dontwarn net.jpountz.**
-dontwarn com.ning.compress.**
-dontwarn lzma.sdk.**
-dontwarn com.github.luben.zstd.**
-dontwarn com.google.protobuf.**
-dontwarn org.eclipse.jetty.npn.**
-dontwarn reactor.blockhound.**
-dontwarn org.bouncycastle.**
-dontwarn dalvik.system.**
-dontwarn sun.security.**

# 2. 忽略重复类的警告
-dontnote **

# 3. 保护你的主类不被混淆掉
#-keep class cn.echomirix.echolauncher.MainKt {
#    public static void main(java.lang.String[]);
#}

# 4. 保护 Ktor / Compose 核心反射需要的类
-keep class io.ktor.** { *; }
-keep class kotlin.reflect.** { *; }