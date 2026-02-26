# ------------------------------------------------------------------------
# ProGuard 规则配置：忽略第三方库（OkHttp/Ktor/SLF4J等）中缺失的平台特有依赖
# ------------------------------------------------------------------------

# 忽略 OkHttp 在桌面端不需要的各种安全库和 Android 类警告
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn android.**
-dontwarn dalvik.system.CloseGuard

# 忽略 SLF4J 相关的警告（如果没有引入专门的 slf4j 实现）
-dontwarn org.slf4j.**

# 忽略 Ktor 和 Coroutines 底层的警告
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.internal.**