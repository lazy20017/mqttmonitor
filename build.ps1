# MQTT Monitor Android Build Script
# Usage: .\build.ps1

$ErrorActionPreference = "Stop"
$PROJECT_ROOT = $PSScriptRoot

# Set Java path
$JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
if (Test-Path $JAVA_HOME) {
    $env:JAVA_HOME = $JAVA_HOME
    $env:PATH = "$JAVA_HOME\bin;$env:PATH"
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  MQTT Monitor Android Build Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Change to project directory
Set-Location $PROJECT_ROOT

# Check Gradle Wrapper
if (-not (Test-Path "gradlew.bat")) {
    Write-Host "Error: gradlew.bat not found" -ForegroundColor Red
    exit 1
}

Write-Host "Using Java: $JAVA_HOME" -ForegroundColor Gray
Write-Host ""

# Parse arguments
$task = "assembleDebug"
$clean = $false
$install = $false

if ($args -contains "-clean" -or $args -contains "clean") { $clean = $true }
if ($args -contains "-r" -or $args -contains "release") { $task = "assembleRelease" }
if ($args -contains "install") { $install = $true }

# Clean if needed
if ($clean) {
    Write-Host "Cleaning build directory..." -ForegroundColor Yellow
    & .\gradlew.bat clean
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Clean failed!" -ForegroundColor Red
        exit 1
    }
}

# Stop old Gradle daemon
Write-Host "Stopping old Gradle processes..." -ForegroundColor Gray
& .\gradlew.bat --stop 2>$null

# Build
Write-Host ""
Write-Host "Building $task..." -ForegroundColor Yellow
& .\gradlew.bat $task

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}

# Output APK location
$apkDir = if ($task -eq "assembleDebug") {
    "app\build\outputs\apk\debug"
} else {
    "app\build\outputs\apk\release"
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  Build Successful!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "APK Output Directory:" -ForegroundColor Cyan
Write-Host "  $PROJECT_ROOT\$apkDir" -ForegroundColor White

# List APK files
Get-ChildItem -Path $apkDir -Filter "*.apk" | ForEach-Object {
    $size = [math]::Round($_.Length / 1MB, 2)
    Write-Host "  - $($_.Name) ($size MB)" -ForegroundColor Gray
}

# Install if needed
if ($install) {
    Write-Host ""
    Write-Host "Installing to device..." -ForegroundColor Yellow

    Get-ChildItem -Path $apkDir -Filter "*.apk" | Select-Object -First 1 | ForEach-Object {
        adb install -r $_.FullName
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Install success!" -ForegroundColor Green
            adb shell am start -n org.archuser.mqttnotify/.MainActivity
        } else {
            Write-Host "Install failed! Check device connection" -ForegroundColor Red
        }
    }
}

Write-Host ""
