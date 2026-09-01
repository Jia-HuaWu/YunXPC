# YunXPC portable (no-install) packaging script
# Output: release\YunXPC\  (copy the whole folder anywhere and run YunXPC.exe)
# Requires: JDK 17+ (jpackage), dependency jars already present in the Gradle cache
# NOTE: keep this file pure ASCII; PowerShell may parse it with the system codepage.
# Gradle writes harmless warnings to stderr; do not treat native stderr as a fatal error.
$ErrorActionPreference = "Continue"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-17.0.11.9-hotspot'
$env:GRADLE_OPTS = "-Djavax.net.ssl.trustStore=E:/DeepseekHarness/YunXPC/trust/cacerts -Djavax.net.ssl.trustStorePassword=changeit"
$JDK = $env:JAVA_HOME

# 1) build main jar + export all runtime dependency jars (Gradle handles the cache itself)
$buildOut = .\gradlew.bat :desktop:jar :desktop:exportRuntimeLibs --console=plain 2>&1 | Out-String
if ($buildOut -match "BUILD FAILED|FAILURE:") { throw "gradle export failed" }

# 2) assemble libs from the exported dependency dir (no cache path guessing)
$libs = Join-Path $root "portable-libs"
if (Test-Path $libs) { Remove-Item $libs -Recurse -Force }
New-Item -ItemType Directory -Force -Path $libs | Out-Null
Copy-Item "$root\desktop\build\libs\desktop.jar" $libs -Force
Get-ChildItem "$root\desktop\build\exportLibs" -File | ForEach-Object { Copy-Item $_.FullName $libs -Force }
$jarCount = (Get-ChildItem $libs -File).Count
Write-Host "libs assembled: $jarCount jars"

# 3) rebuild runtime image to a TEMP dir (jlink creates its output dir itself)
$outDir = Join-Path $root "release\YunXPC"
$runtimeTmp = Join-Path $root "release-runtime-tmp"
if (Test-Path $runtimeTmp) { Remove-Item $runtimeTmp -Recurse -Force }
& "$JDK\bin\jlink.exe" --module-path "$JDK\jmods" `
  --add-modules java.base,java.datatransfer,java.xml,java.prefs,java.desktop,java.logging,jdk.crypto.ec,java.sql,java.naming `
  --strip-debug --no-header-files --no-man-pages --output $runtimeTmp
if ($LASTEXITCODE -ne 0) { throw "jlink failed" }

# 4) jpackage app-image (dest release\YunXPC must not pre-exist)
if (Test-Path $outDir) { Remove-Item $outDir -Recurse -Force }
$argsFile = Join-Path $root "portable.args.txt"
& "$JDK\bin\jpackage.exe" "@$argsFile"
if ($LASTEXITCODE -ne 0) { throw "jpackage failed" }
Remove-Item $runtimeTmp -Recurse -Force

# 5) copy skiko native resources (DLL + icudtl.dat)
Add-Type -AssemblyName System.IO.Compression.FileSystem
$skikoJar = Get-ChildItem $libs -Filter "skiko-awt-runtime-windows-x64*.jar" | Select-Object -First 1 -ExpandProperty FullName
if (-not (Test-Path $skikoJar)) {
    throw "skiko jar not found in $libs"
}
$zip = [System.IO.Compression.ZipFile]::OpenRead($skikoJar)
$appDir = Join-Path $outDir "app"
foreach ($entry in $zip.Entries) {
    if ($entry.FullName -match "\.class$" -or $entry.FullName -match "/$") { continue }
    $leaf = Split-Path $entry.FullName -Leaf
    if ($leaf -eq "") { continue }
    $target = Join-Path $appDir $leaf
    $s = $entry.Open(); $fs = [IO.File]::Create($target); $s.CopyTo($fs); $fs.Close(); $s.Close()
}
$zip.Dispose()

Write-Host ""
Write-Host "Portable package ready: $outDir"
Write-Host "Copy the whole folder anywhere and run YunXPC.exe (no install needed)."
