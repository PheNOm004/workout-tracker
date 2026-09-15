[CmdletBinding()]
param(
    [switch]$Firebase
)

$ErrorActionPreference = 'Stop'
$required = @(
    'TIMEGO_UPLOAD_STORE_FILE',
    'TIMEGO_UPLOAD_STORE_PASSWORD',
    'TIMEGO_UPLOAD_KEY_ALIAS',
    'TIMEGO_UPLOAD_KEY_PASSWORD'
)
$missing = @($required | Where-Object { [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_)) })
if ($missing.Count -gt 0) {
    throw "Signed Play AAB cannot be built; missing environment variables: $($missing -join ', '). See docs/release/PLAY_RELEASE.md."
}

$env:JAVA_HOME = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'C:\Program Files\Android\Android Studio\jbr' }
if (-not (Test-Path -LiteralPath $env:JAVA_HOME -PathType Container)) { throw "JAVA_HOME does not exist: $env:JAVA_HOME" }

& (Join-Path $PSScriptRoot 'verify-play-release.ps1')
if ($LASTEXITCODE -ne 0) { throw 'Play repository preflight failed.' }
$gradleArgs = @('bundleRelease')
if ($Firebase) { $gradleArgs += '-PtimegoFirebase=true' }
& (Join-Path (Split-Path $PSScriptRoot -Parent) 'gradlew.bat') @gradleArgs
if ($LASTEXITCODE -ne 0) { throw 'Signed bundleRelease failed.' }

$bundle = Join-Path (Split-Path $PSScriptRoot -Parent) 'app\build\outputs\bundle\release\app-release.aab'
if (-not (Test-Path -LiteralPath $bundle -PathType Leaf)) { throw "Gradle completed but AAB was not found: $bundle" }
Write-Host "PASS: signed Play AAB created at $bundle" -ForegroundColor Green
