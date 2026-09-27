param()

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path

function Invoke-Git {
    & git -C $repo @args
    if ($LASTEXITCODE -ne 0) {
        throw "Git command failed with exit code $LASTEXITCODE. Resolve any reported conflicts before continuing."
    }
}

$branch = (& git -C $repo branch --show-current).Trim()
if ($LASTEXITCODE -ne 0 -or $branch -ne 'economy/26.3-seamless') {
    throw 'Switch to economy/26.3-seamless before updating.'
}
if (& git -C $repo status --porcelain) {
    throw 'Commit or stash local changes before updating.'
}

Invoke-Git fetch upstream
Invoke-Git remote set-head upstream -a
$upstreamHead = (& git -C $repo symbolic-ref --short refs/remotes/upstream/HEAD).Trim()
if ($LASTEXITCODE -ne 0 -or -not $upstreamHead.StartsWith('upstream/')) {
    throw 'Could not identify the current Velocity-CTD default branch.'
}
Write-Output "Merging $upstreamHead into $branch"
Invoke-Git merge --no-edit --no-ff $upstreamHead

& (Join-Path $repo 'gradlew.bat') :velocity-proxy:check :velocity-proxy:shadowJar --no-daemon --max-workers=2 --console=plain
if ($LASTEXITCODE -ne 0) {
    throw 'Proxy checks or build failed. Review the upstream update before using the JAR.'
}
Write-Output 'Built proxy/build/libs/velocity-proxy-*-all.jar. Run localhost protocol and real-client checks before pushing.'
