[CmdletBinding()]
param(
    [string]$BundlePath
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$failures = [System.Collections.Generic.List[string]]::new()

function Add-Failure([string]$Message) {
    $script:failures.Add($Message)
    Write-Host "FAIL: $Message" -ForegroundColor Red
}

function Assert-LastExitCode([string]$Label) {
    if ($LASTEXITCODE -ne 0) {
        throw "$Label failed with exit code $LASTEXITCODE"
    }
}

Push-Location $repoRoot
try {
    $branch = (git branch --show-current).Trim()
    Assert-LastExitCode 'Read Git branch'
    if ($branch -notin @('master', 'development/timego-public-foundation')) {
        Add-Failure "Release verification is not allowed from branch '$branch'."
    }

    $status = @(git status --porcelain=v1 --untracked-files=all)
    Assert-LastExitCode 'Read Git status'
    if ($status.Count -gt 0) {
        Add-Failure 'The worktree is not clean.'
    }

    $buildScriptPath = Join-Path $repoRoot 'app/build.gradle.kts'
    $buildScript = Get-Content -Raw -LiteralPath $buildScriptPath
    if ($buildScript -notmatch 'applicationId\s*=\s*"com\.lsing\.timego"') {
        Add-Failure 'Expected applicationId com.lsing.timego was not found.'
    }

    $targetMatch = [regex]::Match($buildScript, 'targetSdk\s*=\s*(\d+)')
    if (-not $targetMatch.Success -or [int]$targetMatch.Groups[1].Value -lt 36) {
        Add-Failure 'targetSdk must be at least API 36 for the current Play submission requirement.'
    }

    $versionMatch = [regex]::Match($buildScript, 'versionCode\s*=\s*(\d+)')
    if (-not $versionMatch.Success -or [int]$versionMatch.Groups[1].Value -lt 1) {
        Add-Failure 'versionCode must be a positive integer.'
    }

    $requiredDocuments = @(
        'docs/release/PLAY_RELEASE.md',
        'docs/release/privacy-policy-draft.md',
        'docs/release/data-safety-matrix.md',
        'docs/release/play-console-checklist.md',
        'docs/release/account-deletion-page.md'
    )
    foreach ($relativePath in $requiredDocuments) {
        if (-not (Test-Path -LiteralPath (Join-Path $repoRoot $relativePath) -PathType Leaf)) {
            Add-Failure "Missing release document: $relativePath"
        }
    }

    $trackedSecrets = @(git ls-files '*.jks' '*.keystore' '*.p12' 'keystore.properties')
    Assert-LastExitCode 'Scan tracked signing files'
    if ($trackedSecrets.Count -gt 0) {
        Add-Failure "Tracked signing material found: $($trackedSecrets -join ', ')"
    }

    if ($failures.Count -gt 0) {
        throw "Release preflight failed with $($failures.Count) finding(s)."
    }

    if (-not $env:JAVA_HOME) {
        $androidStudioJbr = 'C:\Program Files\Android\Android Studio\jbr'
        if (Test-Path -LiteralPath $androidStudioJbr -PathType Container) {
            $env:JAVA_HOME = $androidStudioJbr
        } else {
            throw 'JAVA_HOME is not set and the Android Studio JBR was not found.'
        }
    }

    foreach ($task in @('testDebugUnitTest', 'lintDebug', 'assembleDebug', 'assembleRelease')) {
        Write-Host "Running Gradle $task..." -ForegroundColor Cyan
        & .\gradlew.bat $task
        Assert-LastExitCode "Gradle $task"
    }

    if ($BundlePath) {
        $resolvedBundle = (Resolve-Path -LiteralPath $BundlePath).Path
        if ([IO.Path]::GetExtension($resolvedBundle) -ne '.aab') {
            throw "BundlePath must point to an .aab file: $resolvedBundle"
        }

        $jarSigner = Join-Path $env:JAVA_HOME 'bin\jarsigner.exe'
        if (-not (Test-Path -LiteralPath $jarSigner -PathType Leaf)) {
            throw 'INCOMPLETE: jarsigner is unavailable, so the supplied AAB signature was not verified.'
        }
        & $jarSigner -verify -strict $resolvedBundle
        Assert-LastExitCode 'AAB signature verification'

        $bundletool = Get-Command bundletool -ErrorAction SilentlyContinue
        if (-not $bundletool) {
            throw 'INCOMPLETE: bundletool is unavailable, so the supplied AAB was not validated with bundletool.'
        }
        & $bundletool.Source validate --bundle=$resolvedBundle
        Assert-LastExitCode 'bundletool AAB validation'
    }

    Write-Host "PASS: TimeGo Play release repository gate passed on $branch." -ForegroundColor Green
} finally {
    Pop-Location
}
