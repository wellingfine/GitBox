# GitBox: shut down running emulators.
# Usage: .\scripts\stop.ps1

$ErrorActionPreference = "Stop"

$sdk = $env:ANDROID_HOME
if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
if (-not $sdk) { $sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk" }
$adb = Join-Path $sdk "platform-tools\adb.exe"
if (-not (Test-Path $adb)) { throw "adb not found: $adb" }

$devices = & $adb devices | Select-String -Pattern "^emulator-\d+\s+device$"
if (-not $devices) {
    Write-Host "No emulator running."
    exit 0
}

foreach ($line in $devices) {
    $serial = ($line -split "\s+")[0]
    Write-Host "Killing $serial"
    & $adb -s $serial emu kill | Out-Null
}
Write-Host "Done." -ForegroundColor Green
