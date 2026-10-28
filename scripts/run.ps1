# GitBox: build, launch the emulator, install and start the app.
# Usage:
#   .\scripts\run.ps1                 # start first AVD, build debug, install, run
#   .\scripts\run.ps1 -Avd Pixel_9    # use a specific AVD
#   .\scripts\run.ps1 -SkipEmulator   # emulator already running (or use a real device)
#   .\scripts\run.ps1 -Release        # build & install the release APK instead

param(
    [string]$Avd = "",
    [switch]$SkipEmulator,
    [switch]$Release
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    $sdk = $env:ANDROID_HOME
    if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
    if (-not $sdk) { $sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk" }
    if (-not (Test-Path $sdk)) { throw "Android SDK not found. Set ANDROID_HOME first (tried: $sdk)" }

    $adb = Join-Path $sdk "platform-tools\adb.exe"
    if (-not (Test-Path $adb)) { throw "adb not found: $adb" }

    if (-not $SkipEmulator) {
        $emulator = Join-Path $sdk "emulator\emulator.exe"
        if (-not (Test-Path $emulator)) { throw "emulator not found: $emulator" }

        $online = & $adb devices | Select-String -Pattern "^emulator-\d+\s+device$"
        if (-not $online) {
            if ($Avd -eq "") {
                # 用 @() 保证拿到的是数组，避免单条结果被当成字符串取到第一个字符
                $avds = @(& $emulator -list-avds | Where-Object { $_ -and $_.Trim() -ne "" })
                if ($avds.Count -eq 0) { throw "No AVD found. Create one with avdmanager or Android Studio first." }
                $Avd = $avds[0].Trim()
            }
            Write-Host "Starting emulator: $Avd" -ForegroundColor Cyan
            Start-Process -FilePath $emulator -ArgumentList "-avd", $Avd | Out-Null
            & $adb wait-for-device | Out-Null
            Write-Host "Waiting for Android to boot..." -ForegroundColor Cyan
            do {
                Start-Sleep -Seconds 2
                $boot = (& $adb shell getprop sys.boot_completed 2>$null) -join ""
            } while ($boot -notmatch "1")
        } else {
            Write-Host "Emulator already running." -ForegroundColor Cyan
        }
    }

    if ($Release) {
        Write-Host "Building release APK..." -ForegroundColor Cyan
        & .\gradlew.bat :app:assembleRelease
        $apkDir = Join-Path $root "app\build\outputs\apk\release"
        $pattern = "GitBox.*.release.apk"
    } else {
        Write-Host "Building debug APK..." -ForegroundColor Cyan
        & .\gradlew.bat :app:assembleDebug
        $apkDir = Join-Path $root "app\build\outputs\apk\debug"
        $pattern = "GitBox.*.debug.apk"
    }
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed." }

    # 文件名带版本号：GitBox.0.1.1.debug.apk
    $apkFile = Get-ChildItem -Path $apkDir -Filter $pattern -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $apkFile) { throw "APK not found in $apkDir (pattern: $pattern)" }
    $apk = $apkFile.FullName

    Write-Host "Installing: $apk" -ForegroundColor Cyan
    & $adb install -r $apk
    if ($LASTEXITCODE -ne 0) { throw "adb install failed." }

    & $adb shell am start -n com.v0tx.gitbox/.MainActivity | Out-Null
    Write-Host "GitBox started." -ForegroundColor Green
}
finally {
    Pop-Location
}
