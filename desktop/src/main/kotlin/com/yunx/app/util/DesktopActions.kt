package com.yunx.app.util

import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI

/**
 * 桌面端系统能力统一入口：剪贴板 / 打开文件与链接 / 目录选择器。
 * 替代 Android 的 ClipboardManager、Intent.ACTION_VIEW、SAF 选择器。
 */
object DesktopActions {

    /** 复制文本到系统剪贴板 */
    fun copyToClipboard(text: String) {
        runCatching {
            Toolkit.getDefaultToolkit().systemClipboard
                .setContents(StringSelection(text), null)
        }
    }

    /** 读取系统剪贴板文本（无文本内容时返回 null） */
    fun readClipboard(): String? = runCatching {
        Toolkit.getDefaultToolkit().systemClipboard
            .getData(java.awt.datatransfer.DataFlavor.stringFlavor) as? String
    }.getOrNull()

    /** 用系统默认应用打开文件（下载完成后的「打开」） */
    fun openFile(path: String): Boolean {
        val file = File(path)
        if (!file.exists()) return false
        return runCatching {
            java.awt.Desktop.getDesktop().open(file)
            true
        }.getOrDefault(false)
    }

    /** 在资源管理器中显示文件（选中） */
    fun revealFile(path: String): Boolean {
        val file = File(path)
        if (!file.exists()) return false
        return runCatching {
            val cmd = arrayOf("explorer.exe", "/select,", file.absolutePath)
            ProcessBuilder(*cmd).start()
            true
        }.getOrDefault(false)
    }

    /** 用系统默认浏览器打开链接 */
    fun openUrl(url: String) {
        runCatching {
            java.awt.Desktop.getDesktop().browse(URI(url))
        }
    }

    /**
     * 弹出目录选择对话框（Swing FileDialog，模式运行在 EDT 上等待用户选择）。
     * @return 选中目录绝对路径；取消返回 null
     */
    fun pickDirectory(): String? {
        val dialog = FileDialog(null as Frame?, "选择文件夹", FileDialog.LOAD)
        dialog.isMultipleMode = false
        // LOAD 模式在部分平台不能选目录；用 System 属性强制目录模式
        System.setProperty("apple.awt.fileDialogForDirectories", "true")
        dialog.isVisible = true
        val dir = dialog.directory
        val file = dialog.file
        dialog.dispose()
        return when {
            dir == null -> null
            file == null -> dir
            else -> File(dir, file).absolutePath
        }
    }

    /** 弹出文件选择对话框（导入备份等）。返回绝对路径或 null */
    fun pickFile(): String? {
        val dialog = FileDialog(null as Frame?, "选择文件", FileDialog.LOAD)
        dialog.isVisible = true
        val dir = dialog.directory
        val file = dialog.file
        dialog.dispose()
        return if (dir != null && file != null) File(dir, file).absolutePath else null
    }

    /** 系统默认下载目录 */
    val defaultDownloadDir: File = File(System.getProperty("user.home"), "Downloads")
}
