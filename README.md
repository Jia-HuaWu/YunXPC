# YunXPC — 云析桌面版

[YunX（云析）](https://github.com/CYQawa/YunX)（Android 网盘分享链接解析 + 高速下载器，AGPL-3.0）的 **Windows 桌面移植**，基于 **Compose Multiplatform Desktop**。

## 功能

- 解析夸克 / UC / 迅雷 / 百度 / 139 / 123 网盘分享链接（自动识别平台与提取码）
- 分片并发高速下载（默认 32 线程，Range 断点续传、自动重试、全局限速）
- 下载任务管理（暂停 / 继续 / 删除 / 打开文件 / 复制直链）
- 网盘登录（三种方式，任选其一）：
  - **内嵌浏览器登录（推荐）**：夸克 / UC / 百度 / 139 登录页内直接打开真实 Chromium 网页（JCEF），登录后自动检测并保存 Cookie，全程无感
  - **一键自动导入**：扫描本机已装浏览器（Chrome / Edge / Firefox / 360 / QQ 等），自动解密并导入该平台的登录 Cookie（Windows DPAPI；Chrome/Edge 较新版本因「应用绑定加密」可能无法读取，Firefox/360 等始终可用）
  - 手动粘贴 Cookie（兜底）
  - 迅雷（账号密码 + 短信验证）、123（账号密码换 JWT）—— 纯 HTTP，直接表单登录
- 凭证 AES-GCM 加密落库、网盘认证备份/恢复（口令加密）
- 链接收藏、主题（深色模式 + 自定义种子色）

## 技术栈

Kotlin 2.1.0 · Compose Multiplatform 1.7.3 · Gradle 8.13 · JDK 17

| 原 Android 组件 | 桌面替代 |
|---|---|
| Room | sqlite-jdbc（JDBC + StateFlow 手动实现同签名 DAO） |
| AndroidKeyStore 凭证加密 | 本地密钥文件 + AES-GCM（格式 `yunx:v1:` 兼容） |
| SharedPreferences | java.util.prefs |
| WebView 登录 | 内嵌 Chromium（JCEF 132）+ 浏览器 Cookie 自动导入（DPAPI）+ 手动粘贴 |
| MediaStore / SAF 保存 | 普通文件系统（默认 `~/Downloads`） |
| 前台服务 / 通知 / WakeLock | 无（窗口进程常驻） |

## 登录方式说明

夸克/UC/百度/139 登录页优先使用**内嵌 Chromium 浏览器**（应用内直接登录，自动抓 Cookie）。
内嵌浏览器初始化失败时自动回退到「自动导入浏览器 Cookie / 手动粘贴」。

「自动导入」工作原理：直接读取本机浏览器的 Cookie 数据库并用 Windows DPAPI 解密：
- Firefox、360、QQ、搜狗等浏览器：始终可用
- Chrome 127+ / 较新 Edge：启用「应用绑定加密」，本应用无法解密（会提示改用其他浏览器或手动粘贴）

## 构建与运行

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-17.0.11.9-hotspot'
$env:GRADLE_OPTS="-Djavax.net.ssl.trustStore=E:/DeepseekHarness/YunXPC/trust/cacerts -Djavax.net.ssl.trustStorePassword=changeit"
.\gradlew.bat :desktop:run
```

> 注：本机无法直连 services.gradle.org 的证书链（github.com 分支证书不在 JDK cacerts），
> 已将 Windows 证书库全部根证书导入项目本地信任库 `trust/cacerts`，并通过
> `gradle.properties` 的 `org.gradle.jvmargs` 与上述 `GRADLE_OPTS` 生效。
> 仓库镜像走阿里云（`settings.gradle.kts`），首次构建会自动拉取依赖。

## 打包分发

**安装器（每用户安装，无需管理员权限）：**
```powershell
.\gradlew.bat :desktop:packageDistributionForCurrentOS
# 产物：desktop/build/compose/binaries/main/exe/YunXPC-1.0.0.exe（约 118MB，含 JRE）
# 支持静默安装：YunXPC-1.0.0.exe /quiet（安装到 %LOCALAPPDATA%\YunXPC）
```

**便携版（免安装，整个文件夹拷走即用）：**
```powershell
.\portable-package.ps1
# 产物：release\YunXPC\（双击 YunXPC.exe 运行）
```

> 打包注意事项（本机踩坑记录）：
> 1. jpackage 参数文件不支持非 ASCII 描述（报 `Input length = 1`），`description` 保持纯英文；
> 2. sqlite-jdbc 反射加载 `java.sql.Driver`，jlink 检测不到 → 必须显式 `modules("java.sql", "java.naming")`；
> 3. 手动组装 app-image 时需从 `skiko-awt-runtime-windows-x64` jar 解包 `skiko-windows-x64.dll` 与 `icudtl.dat` 到 app 目录（脚本已处理）；
> 4. 安装器依赖 WiX（首次自动从 GitHub 下载）；若 `~/.gradle/compose-jb/wix311.zip` 已下载但未解包，需手动解压到同目录 `wix311\`。

## GitHub 上传与克隆构建

仓库已配置 `.gitignore`，提交体积仅几 MB（源码 + Gradle Wrapper + `desktop/libs/material-color-utilities-1.0.0.jar` 2.1MB）。
`trust/`、`release/`、`portable-libs/`、`.gradle-home/`、`desktop/build/` 等大目录均被忽略。

**上传后在其他机器克隆构建，需要调整三处本机特定配置：**

1. `gradle/wrapper/gradle-wrapper.properties` 的 `distributionUrl` 当前指向本机离线 zip
   （`file:///E:/.../gradle-8.13-b.zip`）→ 改为官方地址：
   `distributionUrl=https\://services.gradle.org/distributions/gradle-8.13-bin.zip`
2. `gradle.properties` 里 `org.gradle.java.home` 与 `org.gradle.jvmargs` 中的
   `-Djavax.net.ssl.trustStore=...` 指向本机 JDK / 信任库 → 删除或改为本机实际路径
   （正常网络环境不需要本地信任库，直接删除 truststore 参数即可）
3. `settings.gradle.kts` 的仓库列表第一顺位是阿里云镜像（国内加速）——不需要可删除，
   保留也完全无害（镜像缺失时自动回退官方仓库）

> 本机（开发机）网络无法直连部分官方仓库证书链，才需要上述本地 truststore 与离线发行版；
> 正常环境克隆后执行 `.\gradlew.bat :desktop:run` 即可构建运行。

## 数据目录

`%USERPROFILE%\.yunx-pc\`：`yunx.db`（任务/凭证库）、`credential.key`（加密密钥）、
`cache/download_tmp`（下载分片）、`files/yunx-pc.log`（运行日志）。
设置存于 Windows 注册表 `HKEY_CURRENT_USER\Software\JavaSoft\Prefs\yunx`。

## 与上游的差异

1. 登录：夸克/UC/百度/139 改为粘贴 Cookie（上游 Android 版为 WebView 提取）；迅雷验证页由系统浏览器承载。
2. 移除：通知栏、锁屏保活、电池优化引导、动态取色（Material You）、应用图标切换、APK 更新检测、崩溃独立进程。
3. `keepDownloadWhenLocked` 等设置项保留但桌面无实际语义。

## 许可

本项目基于 [GNU AGPL-3.0](https://www.gnu.org/licenses/agpl-3.0.html) 上游 [CYQawa/YunX](https://github.com/CYQawa/YunX) 移植，同样以 AGPL-3.0 分发。仅供个人学习与交流。
