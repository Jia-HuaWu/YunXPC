package com.yunx.app.ui.jcef

import com.yunx.app.AppContext
import com.yunx.app.util.Log
import me.friwi.jcefmaven.CefAppBuilder
import org.cef.CefApp
import org.cef.CefSettings
import java.io.File

/**
 * JCEF（内嵌 Chromium）全局持有者。
 *
 * 必须在进程主线程初始化（CefApp 的主线程约束）——由 MainKt.main() 在进入 Compose 前调用。
 * 初始化失败（如原生库加载异常）时静默降级：登录页回退到「自动导入/手动粘贴」模式。
 */
object JcefHolder {

    private const val TAG = "YunX-JCEF"

    @Volatile
    private var cefApp: CefApp? = null

    @Volatile
    private var initFailure: Throwable? = null

    private val lock = Any()

    fun initOnMainThread(noSandbox: Boolean = false) {
        synchronized(lock) {
            if (cefApp != null || initFailure != null) return
            try {
                val builder = CefAppBuilder()
                builder.setInstallDir(File(AppContext.cacheDir, "jcef-bundle"))
                if (noSandbox) {
                    // 受限/沙箱环境（如 CI）下 Chromium 进程沙箱拿不到目录授权，禁用之（仅诊断冒烟用）
                    builder.addJcefArgs("--no-sandbox")
                }
                val settings = builder.getCefSettings()
                settings.windowless_rendering_enabled = false
                // CEF 要求 cache_path 必须是 root_cache_path 的子目录
                settings.root_cache_path = File(AppContext.cacheDir, "jcef").absolutePath
                settings.cache_path = File(AppContext.cacheDir, "jcef/cache").absolutePath
                settings.log_file = File(AppContext.filesDir, "jcef.log").absolutePath
                settings.log_severity = org.cef.CefSettings.LogSeverity.LOGSEVERITY_WARNING
                cefApp = builder.build()
                Log.i(TAG, "JCEF initialized")
            } catch (t: Throwable) {
                initFailure = t
                Log.e(TAG, "JCEF init failed, fallback to paste mode", t)
            }
        }
    }

    fun app(): CefApp? = cefApp

    fun isAvailable(): Boolean = cefApp != null

    /** 进程退出前释放（尽力而为；进程即将结束，失败无碍） */
    fun disposeQuietly() {
        runCatching { cefApp?.dispose() }.onFailure {
            Log.w(TAG, "CefApp dispose failed: ${it.message}")
        }
        cefApp = null
    }
}
