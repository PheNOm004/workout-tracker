[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$ProjectId
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$firebase = Get-Command firebase -ErrorAction SilentlyContinue
if (-not $firebase) { throw 'Firebase CLI is required. Install it separately and authenticate interactively.' }
if ($ProjectId -notmatch '^[a-z0-9][a-z0-9-]{4,28}[a-z0-9]$') { throw 'ProjectId is not a valid Firebase project ID.' }

Push-Location $repoRoot
try {
    foreach ($path in @('firebase.json', 'firebase/firestore.rules', 'functions/package-lock.json')) {
        if (-not (Test-Path -LiteralPath (Join-Path $repoRoot $path) -PathType Leaf)) { throw "Missing deployment input: $path" }
    }
    foreach ($page in @('hosting/privacy/index.html', 'hosting/account-deletion/index.html')) {
        $content = Get-Content -Raw -LiteralPath (Join-Path $repoRoot $page)
        if ($content -match 'PUBLICATION INPUT|Draft for release|placeholder|add the monitored') {
            throw "Refusing deployment: $page still contains release placeholders. Replace it with approved production content first."
        }
    }
    if (-not (Test-Path -LiteralPath (Join-Path $repoRoot 'functions/node_modules') -PathType Container)) {
        Push-Location (Join-Path $repoRoot 'functions')
        try {
            npm ci
            if ($LASTEXITCODE -ne 0) { throw 'npm ci failed while preparing Functions dependencies.' }
        } finally {
            Pop-Location
        }
    }
    $args = @('deploy', '--project', $ProjectId, '--only', 'hosting,firestore,functions')
    & $firebase.Source @args
    if ($LASTEXITCODE -ne 0) { throw "Firebase deployment failed with exit code $LASTEXITCODE." }
} finally {
    Pop-Location
}
