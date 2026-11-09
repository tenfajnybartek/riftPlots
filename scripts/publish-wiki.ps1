[CmdletBinding()]
param([switch]$Publish)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$exportDirectory = Join-Path $projectRoot 'build/wiki-export'
$wikiCheckout = Join-Path $projectRoot 'build/riftPlots.wiki'
$wikiRemote = 'https://github.com/tenfajnybartek/riftPlots.wiki.git'

function Invoke-WikiGit {
    param([Parameter(Mandatory)][string[]]$GitArguments)
    & git @GitArguments
    if ($LASTEXITCODE -ne 0) { throw "Git command failed: $($GitArguments[0])" }
}

& python (Join-Path $PSScriptRoot 'wiki.py') export
if ($LASTEXITCODE -ne 0) { throw 'Wiki validation/export failed.' }

if (-not $Publish) {
    Write-Host "Prepared wiki: $exportDirectory"
    Write-Host 'Nothing was published. Use -Publish to publish the prepared wiki.'
    return
}

$previousPrompt = $env:GIT_TERMINAL_PROMPT
$previousInteractive = $env:GCM_INTERACTIVE
try {
    $env:GIT_TERMINAL_PROMPT = '0'
    $env:GCM_INTERACTIVE = 'Never'
    $remoteHead = & git ls-remote --symref $wikiRemote HEAD 2>$null
    if ($LASTEXITCODE -ne 0) {
        throw 'Wiki is unavailable. Enable Wiki in repository settings and save the first Home page on GitHub. Also verify Git authentication.'
    }
    $branchLine = $remoteHead | Where-Object { "$_" -match '^ref: refs/heads/(\S+)\s+HEAD$' } | Select-Object -First 1
    if (-not $branchLine -or "$branchLine" -notmatch '^ref: refs/heads/(\S+)\s+HEAD$') {
        throw 'Cannot determine the default Wiki branch.'
    }
    $defaultBranch = $Matches[1]

    if (Test-Path -LiteralPath $wikiCheckout) {
        if (-not (Test-Path -LiteralPath (Join-Path $wikiCheckout '.git'))) {
            throw 'Wiki checkout path already exists and is not a Git repository.'
        }
        $existingRemote = (Invoke-WikiGit -GitArguments @('-C', $wikiCheckout, 'remote', 'get-url', 'origin')) -join "`n"
        if ($existingRemote.Trim() -ne $wikiRemote) { throw 'Unexpected remote in Wiki checkout.' }
        $status = Invoke-WikiGit -GitArguments @('-C', $wikiCheckout, 'status', '--porcelain')
        if ($status) { throw 'Wiki checkout has local changes; review them before publishing.' }
        $branch = (Invoke-WikiGit -GitArguments @('-C', $wikiCheckout, 'branch', '--show-current')) -join "`n"
        if ($branch.Trim() -ne $defaultBranch) { throw 'Wiki checkout is not on the default branch.' }
        Invoke-WikiGit -GitArguments @('-C', $wikiCheckout, 'pull', '--ff-only', 'origin', $defaultBranch)
    } else {
        Invoke-WikiGit -GitArguments @('clone', '--', $wikiRemote, $wikiCheckout)
    }

    $exportRoot = (Resolve-Path -LiteralPath $exportDirectory).Path
    $generatedPaths = @()
    foreach ($file in Get-ChildItem -LiteralPath $exportRoot -Recurse -File) {
        $relative = $file.FullName.Substring($exportRoot.Length + 1)
        $target = Join-Path $wikiCheckout $relative
        $targetDirectory = Split-Path -Parent $target
        New-Item -ItemType Directory -Force -Path $targetDirectory | Out-Null
        Copy-Item -LiteralPath $file.FullName -Destination $target
        $generatedPaths += $relative.Replace('\', '/')
    }
    Invoke-WikiGit -GitArguments (@('-C', $wikiCheckout, 'add', '--') + $generatedPaths)
    & git -C $wikiCheckout diff --cached --quiet
    if ($LASTEXITCODE -eq 1) {
        Invoke-WikiGit -GitArguments @('-C', $wikiCheckout, 'commit', '-m', 'docs: publish comprehensive Polish riftPlots wiki')
    } elseif ($LASTEXITCODE -ne 0) {
        throw 'Cannot inspect the staged Wiki changes.'
    }
    Invoke-WikiGit -GitArguments @('-C', $wikiCheckout, 'push', 'origin', "HEAD:refs/heads/$defaultBranch")
    Write-Host 'Published: https://github.com/tenfajnybartek/riftPlots/wiki'
} finally {
    $env:GIT_TERMINAL_PROMPT = $previousPrompt
    $env:GCM_INTERACTIVE = $previousInteractive
}
