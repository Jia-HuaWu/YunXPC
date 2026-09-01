package com.yunx.app.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yunx.app.data.network.UCConstants
import com.yunx.app.ui.jcef.JcefHolder
import com.yunx.app.ui.jcef.JcefLoginPane
import com.yunx.app.ui.viewmodel.UCAccountViewModel

/**
 * UC 网盘登录页：内嵌浏览器优先，粘贴/自动导入兜底。
 */
@Composable
fun UCLoginScreen(
    viewModel: UCAccountViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    var useBrowser by remember { mutableStateOf(JcefHolder.isAvailable()) }
    if (useBrowser) {
        JcefLoginPane(
            loginUrl = UCConstants.LOGIN_URL,
            domains = listOf(".uc.cn"),
            requiredKeys = listOf("__pus", "__puus"),
            platform = "UC",
            onSave = { cookie -> viewModel.saveUCAccount(cookie) },
            onBack = onBack,
            onSaved = onSaved,
            onSwitchToPaste = { useBrowser = false }
        )
    } else {
        CookieLoginContent(
            title = "UC 网盘登录",
            loginUrl = UCConstants.LOGIN_URL,
            platform = "UC",
            cookieRequirement = "Cookie 需包含 __pus= 与 __puus=",
            validityHint = "Cookie 失效后需重新登录",
            onSave = { cookie -> viewModel.saveUCAccount(cookie) },
            onBack = onBack,
            onSaved = onSaved
        )
    }
}
