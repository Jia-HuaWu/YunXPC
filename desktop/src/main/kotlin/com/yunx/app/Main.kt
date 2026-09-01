package com.yunx.app

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.yunx.app.data.network.XunleiDeviceFingerprint
import com.yunx.app.ui.MainScreen
import com.yunx.app.ui.jcef.JcefHolder
import com.yunx.app.ui.theme.ComposeEmptyActivityTheme

fun main(args: Array<String>) {
    // 桌面上下文初始化（数据目录等）
    AppContext.init()
    // 迅雷设备指纹（进程启动时初始化一次，等价原 Application.onCreate）
    XunleiDeviceFingerprint.init()

    // 诊断模式：--jcef-smoke [url]，创建内嵌浏览器加载页面并输出 Cookie 统计后退出
    if (args.contains("--jcef-smoke")) {
        val url = args.firstOrNull { it.startsWith("http") } ?: "https://pan.quark.cn"
        runJcefSmoke(url)
        return
    }

    // 内嵌浏览器（JCEF）必须在进程主线程初始化（先于 AWT/Compose 启动）；
    // 首次运行需解包原生库（约 200MB），耗时 10~30 秒属正常；失败自动降级粘贴模式
    JcefHolder.initOnMainThread()

    // 进程退出前尽力释放 JCEF
    Runtime.getRuntime().addShutdownHook(
        Thread { JcefHolder.disposeQuietly() }
    )

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "云析 YunX PC",
            state = WindowState(size = DpSize(1100.dp, 760.dp)),
        ) {
            ComposeEmptyActivityTheme {
                MainScreen()
            }
        }
    }
}

/**
 * 内嵌浏览器诊断冒烟：主线程初始化 JCEF → 创建浏览器加载页面 → 等待 20s → 输出 Cookie 统计。
 */
private fun runJcefSmoke(url: String) {
    println("SMOKE: init JCEF...")
    JcefHolder.initOnMainThread(noSandbox = true)
    val app = JcefHolder.app()
    if (app == null) {
        println("SMOKE: JCEF init FAILED")
        return
    }
    println("SMOKE: creating browser for $url ...")
    val client = app.createClient()
    client.addLoadHandler(object : org.cef.handler.CefLoadHandler {
        override fun onLoadingStateChange(
            browser: org.cef.browser.CefBrowser,
            isLoading: Boolean,
            canGoBack: Boolean,
            canGoForward: Boolean
        ) {
            println("SMOKE: loading=$isLoading url=${browser.url}")
        }

        override fun onLoadStart(
            browser: org.cef.browser.CefBrowser,
            frame: org.cef.browser.CefFrame,
            transitionType: org.cef.network.CefRequest.TransitionType
        ) {
        }

        override fun onLoadEnd(
            browser: org.cef.browser.CefBrowser,
            frame: org.cef.browser.CefFrame,
            httpStatusCode: Int
        ) {
            println("SMOKE: loadEnd status=$httpStatusCode url=${frame.url}")
        }

        override fun onLoadError(
            browser: org.cef.browser.CefBrowser,
            frame: org.cef.browser.CefFrame,
            errorCode: org.cef.handler.CefLoadHandler.ErrorCode,
            errorText: String,
            failedUrl: String
        ) {
            println("SMOKE: LOAD ERROR $errorCode $errorText $failedUrl")
        }
    })
    val browser = client.createBrowser(url, false, false)
    // JCEF 默认延迟到 UI 组件显示时才真正创建；冒烟无 UI，需立即创建
    browser.createImmediately()
    println("SMOKE: browser created, waiting 25s for page load...")
    Thread.sleep(25000)

    // 竞态实证：visitAllCookies 回调异步，0ms 立即读 vs 500ms 后读
    val manager = org.cef.network.CefCookieManager.getGlobalManager()
    fun readCookies(): List<String> {
        val list = java.util.concurrent.ConcurrentLinkedQueue<String>()
        manager.visitAllCookies(
            object : org.cef.callback.CefCookieVisitor {
                override fun visit(
                    cookie: org.cef.network.CefCookie,
                    count: Int,
                    total: Int,
                    delete: org.cef.misc.BoolRef
                ): Boolean {
                    list.add("${cookie.name}=${cookie.value}")
                    return true
                }
            }
        )
        Thread.sleep(500)
        return list.toList()
    }
    // 第一轮：visitAllCookies 返回后先不等待，立即取快照（用另一队列演示竞态）
    val immediate = java.util.concurrent.ConcurrentLinkedQueue<String>()
    manager.visitAllCookies(
        object : org.cef.callback.CefCookieVisitor {
            override fun visit(
                cookie: org.cef.network.CefCookie,
                count: Int,
                total: Int,
                delete: org.cef.misc.BoolRef
            ): Boolean {
                immediate.add("${cookie.name}=${cookie.value}")
                return true
            }
        }
    )
    val immediateCount = immediate.size
    // 等待回调完成后再取一遍
    val waitedCount = readCookies().size
    println("SMOKE: cookies read immediately(no wait)=$immediateCount, after 500ms=$waitedCount")
    println("SMOKE: cookie samples=${readCookies().take(5)}")

    // API 回环验证：写入测试 Cookie 再读取
    val testCookie = org.cef.network.CefCookie(
        "smoke.test", "roundtrip", ".quark.cn", "/",
        true, false, null, null, false, null
    )
    runCatching { manager.setCookie("https://pan.quark.cn", testCookie) }
    val counter = java.util.concurrent.atomic.AtomicInteger(0)
    runCatching {
        manager.visitAllCookies(
            object : org.cef.callback.CefCookieVisitor {
                override fun visit(
                    cookie: org.cef.network.CefCookie,
                    count: Int,
                    total: Int,
                    delete: org.cef.misc.BoolRef
                ): Boolean {
                    counter.incrementAndGet()
                    if (cookie.name == "smoke.test") {
                        println("SMOKE: roundtrip cookie value=${cookie.value}")
                    }
                    return true
                }
            }
        )
    }
    Thread.sleep(500)
    println("SMOKE: total cookies=${counter.get()}")
    runCatching { browser.close(true) }
    runCatching { client.dispose() }
    println("SMOKE: done")
}
