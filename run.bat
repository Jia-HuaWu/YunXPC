@echo off
rem YunXPC 构建/运行便捷脚本（自动设置本机 JDK 与信任库）
set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.11.9-hotspot"
set "GRADLE_OPTS=-Djavax.net.ssl.trustStore=E:/DeepseekHarness/YunXPC/trust/cacerts -Djavax.net.ssl.trustStorePassword=changeit"
cd /d "%~dp0"

if "%1"=="run" (
    call gradlew.bat :desktop:run --console=plain
) else if "%1"=="package" (
    call gradlew.bat :desktop:packageDistributionForCurrentOS --console=plain
    echo.
    echo 产物目录: desktop\build\compose\binaries\main\app\YunXPC\
) else if "%1"=="build" (
    call gradlew.bat :desktop:build --console=plain
) else (
    echo 用法: run.bat [run^|build^|package]
    echo   run     编译并启动桌面应用
    echo   build   仅编译
    echo   package 打包 Windows 分发目录
)
