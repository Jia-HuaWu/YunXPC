package com.yunx.app.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yunx.app.data.network.QuarkConstants
import com.yunx.app.ui.jcef.JcefHolder
import com.yunx.app.ui.jcef.JcefLoginPane
import com.yunx.app.ui.viewmodel.QuarkAccountViewModel

/**
 * 夸克网盘登录页：
 * - 首选内嵌 Chromium 浏览器（JCEF）直接登录，自动检测 Cookie；
 * - 不可用时回退「粘贴 Cookie / 自动导入浏览器 Cookie」。
 * Cookie 需包含 __pus= 与 __puus=。
 */
@Composable
fun QuarkLoginScreen(
    viewModel: QuarkAccountViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    var useBrowser by remember { mutableStateOf(JcefHolder.isAvailable()) }
    if (useBrowser) {
        JcefLoginPane(
            loginUrl = QuarkConstants.LOGIN_URL,
            domains = listOf(".quark.cn"),
            requiredKeys = listOf("__pus", "__puus"),
            platform = "QUARK",
            onSave = { cookie -> viewModel.saveQuarkAccount(cookie) },
            onBack = onBack,
            onSaved = onSaved,
            onSwitchToPaste = { useBrowser = false }
        )
    } else {
        CookieLoginContent(
            title = "夸克网盘登录",
            loginUrl = QuarkConstants.LOGIN_URL,
            platform = "QUARK",
            cookieRequirement = "Cookie 需包含 __pus= 与 __puus=",
            validityHint = "Cookie 约 30 天有效，失效后需重新登录",
            onSave = { cookie -> viewModel.saveQuarkAccount(cookie) },
            onBack = onBack,
            onSaved = onSaved
        )
    }
}
