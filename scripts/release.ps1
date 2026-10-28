# GitBox: bump version (+1) and build the release APK.
# Usage:
#   .\scripts\release.ps1             # bump version then assembleRelease
#   .\scripts\release.ps1 -Install    # also install to the connected device/emulator
#   .\scripts\release.ps1 -NoBump     # keep current version, just build

param(
    [switch]$Install,
    [switch]$NoBump
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    if (-not $NoBump) {
        Write-Host "Bumping version..." -ForegroundColor Cyan
        & .\gradlew.bat bumpVersion
        if ($LASTEXITCODE -ne 0) { throw "bumpVersion failed." }
    }

    $propsText = Get-Content (Join-Path $root "gradle.properties") -Raw
    $versionName = [regex]::Match($propsText, "(?m)^gitboxVersionName=(.*)$").Groups[1].Value.Trim()
    $versionCode = [regex]::Match($propsText, "(?m)^gitboxVersionCode=(.*)$").Groups[1].Value.Trim()
    Write-Host "Building GitBox v$versionName (versionCode $versionCode)" -ForegroundColor Cyan

    & .\gradlew.bat :app:assembleRelease
    if ($LASTEXITCODE -ne 0) { throw "assembleRelease failed." }

    $apkDir = Join-Path $root "app\build\outputs\apk\release"
    $apkFile = Get-ChildItem -Path $apkDir -Filter "GitBox.*.release.apk" -File |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $apkFile) { throw "APK not found in $apkDir" }
    $apk = $apkFile.FullName
    Write-Host "Release APK: $apk" -ForegroundColor Green

    if ($Install) {
        $sdk = $env:ANDROID_HOME
        if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
        if (-not $sdk) { $sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk" }
        $adb = Join-Path $sdk "platform-tools\adb.exe"
        if (-not (Test-Path $adb)) { throw "adb not found: $adb" }
        & $adb install -r $apk
        & $adb shell am start -n com.v0tx.gitbox/.MainActivity | Out-Null
        Write-Host "Installed and started." -ForegroundColor Green
    }
}
finally {
    Pop-Location
}
