package cn.echomirix.echolauncher.core.account.microsoft

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CompletableDeferred
import java.awt.Desktop
import java.net.URI

object LocalAuthServer {
    private var server: NettyApplicationEngine? = null

    // 用于将获取到的 code 传回给协程
    private var codeDeferred: CompletableDeferred<String>? = null

    suspend fun startAndGetCode(): String {
        codeDeferred = CompletableDeferred()

        server = embeddedServer(Netty, port = 0, host = "localhost") {
            routing {
                // 1. 本地引导页
                get("/") {
                    val html = """
                        <!DOCTYPE html>
                        <html lang="zh-CN">
                        <head>
                            <meta charset="UTF-8">
                            <title>EchoLauncher - 微软登录</title>
                            <style>
                                body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #1e1e1e; color: #fff; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; margin: 0; }
                                .container { background: #2d2d30; padding: 40px; border-radius: 12px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); text-align: center; max-width: 600px; }
                                .btn { background: #0078D7; color: white; padding: 12px 24px; border: none; border-radius: 6px; font-size: 16px; cursor: pointer; text-decoration: none; display: inline-block; margin: 20px 0; font-weight: bold;}
                                .btn:hover { background: #005a9e; }
                                .drop-zone { margin-top: 30px; border: 2px dashed #0078D7; border-radius: 8px; padding: 40px; background: rgba(0, 120, 215, 0.1); transition: all 0.3s ease; }
                                .drop-zone.dragover { background: rgba(0, 120, 215, 0.3); border-color: #00a8ff; transform: scale(1.02); }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <h2>EchoLauncher 安全登录验证</h2>
                                <p>为了保障您的账号安全，我们将使用您系统自带的浏览器进行验证。</p>
                                
                                <a href="${MicrosoftAuthService.getLoginUrl()}" target="_blank" class="btn">第一步：点击这里打开微软登录网页</a>
                                
                                <p style="color: #aaa; font-size: 14px; text-align: left;">
                                    <strong>操作指引：</strong><br>
                                    1. 在新弹出的页面中完成登录。<br>
                                    2. 登录成功后，页面可能会变成一片空白。<br>
                                    3. <strong>将那个空白页面的网址，按住鼠标左键，直接拖拽到下方虚线框内（或复制粘贴到输入框中）！</strong>
                                </p>

                                <div class="drop-zone" id="dropZone">
                                    <h3>⬇️ 将登录完成后的网址拖拽到这里 ⬇️</h3>
                                    <p>或直接将完整链接粘贴到此处并回车</p>
                                    <input type="text" id="pasteInput" placeholder="在这里粘贴链接..." style="width: 80%; padding: 10px; margin-top: 10px; border-radius: 4px; border: none; outline: none; text-align: center;">
                                </div>
                            </div>

                            <script>
                                const dropZone = document.getElementById('dropZone');
                                const pasteInput = document.getElementById('pasteInput');

                                // 处理拖拽效果
                                dropZone.addEventListener('dragover', (e) => { e.preventDefault(); dropZone.classList.add('dragover'); });
                                dropZone.addEventListener('dragleave', () => dropZone.classList.remove('dragover'));
                                
                                dropZone.addEventListener('drop', (e) => {
                                    e.preventDefault();
                                    dropZone.classList.remove('dragover');
                                    // 尝试从拖拽的数据中获取 URL 文本
                                    const data = e.dataTransfer.getData('text/plain') || e.dataTransfer.getData('text/uri-list');
                                    handleUrl(data);
                                });

                                // 兼容玩家习惯性粘贴
                                pasteInput.addEventListener('keypress', (e) => {
                                    if (e.key === 'Enter') handleUrl(pasteInput.value);
                                });

                                function handleUrl(url) {
                                    if(url && url.includes('code=')) {
                                        document.body.innerHTML = '<h2 style="color:#4CAF50;">授权码已接收！您可以关闭此网页并返回启动器了。</h2>';
                                        // 把抓取到的 url 发送给我们本地后端的 /callback 接口
                                        fetch('/callback?url=' + encodeURIComponent(url));
                                    } else {
                                        alert('这不是正确的授权链接！请确保您拖拽的是登录成功后的地址栏。');
                                    }
                                }
                            </script>
                        </body>
                        </html>
                    """.trimIndent()
                    call.respondText(html, io.ktor.http.ContentType.Text.Html)
                }

                // 2. 接收网页前端发回来的 Code
                get("/callback") {
                    val fullUrl = call.request.queryParameters["url"] ?: ""

                    // 解析出纯净的 code
                    val code = fullUrl.split("&").find { it.contains("code=") }?.substringAfter("code=")

                    if (!code.isNullOrBlank()) {
                        call.respondText("Success", io.ktor.http.ContentType.Text.Plain)
                        codeDeferred?.complete(code) // 唤醒协程，把 code 交给启动器

                        // 拿到 code 后，延迟一下关闭本地服务器，释放端口
                        kotlin.concurrent.thread {
                            Thread.sleep(1000)
                            server?.stop(1000, 2000)
                        }
                    } else {
                        call.respondText("Failed to extract code", status = io.ktor.http.HttpStatusCode.BadRequest)
                    }
                }
            }
        }.start(wait = false)
        val port = server?.resolvedConnectors()?.first()?.port
            ?: throw RuntimeException("无法分配本地端口供验证服务器使用")
        Desktop.getDesktop().browse(URI("http://localhost:$port"))

        // 挂起协程，死死等待网页端把 code 传回来
        return codeDeferred!!.await()
    }

    fun stop() {
        server?.stop(1000, 2000)
        codeDeferred?.cancel()
    }
}