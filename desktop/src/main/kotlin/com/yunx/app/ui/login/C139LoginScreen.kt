package com.yunx.app.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yunx.app.data.network.C139Constants
import com.yunx.app.ui.jcef.JcefHolder
import com.yunx.app.ui.jcef.JcefLoginPane
import com.yunx.app.ui.viewmodel.C139AccountViewModel

/**
 * 139 网盘（和彩云）登录页：内嵌浏览器优先，粘贴/自动导入兜底。
 * Cookie 需包含 Os_SSo_Sid 与 RMKEY（或 authorization=）。
 */
@Composable
fun C139LoginScreen(
    viewModel: C139AccountViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    var useBrowser by remember { mutableStateOf(JcefHolder.isAvailable()) }
    if (useBrowser) {
        JcefLoginPane(
            loginUrl = C139Constants.LOGIN_URL,
            domains = listOf("mail.10086.cn", "yun.139.com", ".10086.cn"),
            requiredKeys = listOf("Os_SSo_Sid", "RMKEY"),
            platform = "C139",
            onSave = { cookie -> viewModel.saveC139Account(cookie) },
            onBack = onBack,
            onSaved = onSaved,
            onSwitchToPaste = { useBrowser = false }
        )
    } else {
        CookieLoginContent(
            title = "139 网盘登录",
            loginUrl = C139Constants.LOGIN_URL,
            platform = "C139",
            cookieRequirement = "Cookie 需包含 Os_SSo_Sid 与 RMKEY（或 authorization=）",
            validityHint = "建议从 mail.10086.cn 登录态复制完整 Cookie",
            onSave = { cookie -> viewModel.saveC139Account(cookie) },
            onBack = onBack,
            onSaved = onSaved
        )
    }
}
