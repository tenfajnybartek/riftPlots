[CmdletBinding()]
param([switch]$AcceptEula, [switch]$PrepareOnly, [string]$ServerDirectory = 'build/integration-server')
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$serverRoot = [IO.Path]::GetFullPath((Join-Path $projectRoot $ServerDirectory))
$buildRoot = [IO.Path]::GetFullPath((Join-Path $projectRoot 'build')) + [IO.Path]::DirectorySeparatorChar
if (-not $serverRoot.StartsWith($buildRoot, [StringComparison]::OrdinalIgnoreCase)) { throw 'Integration server must stay inside this project build directory.' }
New-Item -ItemType Directory -Force -Path (Join-Path $serverRoot 'plugins') | Out-Null
$dependencies = @(
    @{ Path = 'paper-26.3-246.jar'; Algorithm = 'SHA256'; Hash = 'd158542561e440c4026be7ccb32f551ee88313864792f38ea0ce5d1b88f1b27c'; Url = 'https://fill-data.papermc.io/v1/objects/d158542561e440c4026be7ccb32f551ee88313864792f38ea0ce5d1b88f1b27c/paper-26.3-246.jar' },
    @{ Path = 'plugins/worldedit-bukkit-7.4.6.jar'; Algorithm = 'SHA512'; Hash = '1fe3975132857093f971fed9efc601299bfa76fa7885f52a542fee41e7a0165ca9cb3cbcf5ae244474887d774d650b0db6f8f79aa80f98b245dea44f1c1b4152'; Url = 'https://cdn.modrinth.com/data/1u6JkXh5/versions/YFnqiUlL/worldedit-bukkit-7.4.6.jar' },
    @{ Path = 'plugins/worldguard-bukkit-7.0.21.jar'; Algorithm = 'SHA512'; Hash = '4253b97627f93ea007d91e977c15f5f4a3219865589083fa3b523d8b15a2cfe26d71076c8eb98c0122e7fde618ad3fc44cb986dbd40a222f09678b91a98245c2'; Url = 'https://cdn.modrinth.com/data/DKY9btbd/versions/fiHLM6oY/worldguard-bukkit-7.0.21.jar' },
    @{ Path = 'plugins/Vault.jar'; Algorithm = 'SHA256'; Hash = 'a6b5ed97f43a5cf5bbaf00a7c8cd23c5afc9bd003f849875af8b36e6cf77d01d'; Url = 'https://github.com/MilkBowl/Vault/releases/download/1.7.3/Vault.jar' }
)
foreach ($dependency in $dependencies) {
    $destination = Join-Path $serverRoot $dependency.Path
    if (-not (Test-Path -LiteralPath $destination)) {
        $temporary = $destination + '.download'
        Invoke-WebRequest -UseBasicParsing -Uri $dependency.Url -OutFile $temporary
        if ((Get-FileHash -LiteralPath $temporary -Algorithm $dependency.Algorithm).Hash.ToLowerInvariant() -ne $dependency.Hash) { throw "Dependency checksum mismatch: $($dependency.Path)" }
        Move-Item -LiteralPath $temporary -Destination $destination
    }
    if ((Get-FileHash -LiteralPath $destination -Algorithm $dependency.Algorithm).Hash.ToLowerInvariant() -ne $dependency.Hash) { throw "Dependency checksum mismatch: $($dependency.Path)" }
}
$paper = Join-Path $serverRoot 'paper-26.3-246.jar'
if (-not (Test-Path -LiteralPath $paper)) { throw 'Prepare the pinned Paper 26.3 build 246 JAR before starting.' }
$paperHash = (Get-FileHash -LiteralPath $paper -Algorithm SHA256).Hash.ToLowerInvariant()
if ($paperHash -ne 'd158542561e440c4026be7ccb32f551ee88313864792f38ea0ce5d1b88f1b27c') { throw 'Paper checksum mismatch.' }
$utf8 = New-Object System.Text.UTF8Encoding($false)
$properties = Join-Path $serverRoot 'server.properties'
if (-not (Test-Path -LiteralPath $properties)) {
    [IO.File]::WriteAllText($properties, @'
server-ip=127.0.0.1
server-port=25576
online-mode=false
level-name=riftplots-test
level-type=minecraft:flat
generator-settings={"layers":[{"block":"minecraft:bedrock","height":1}],"biome":"minecraft:plains"}
generate-structures=false
view-distance=2
simulation-distance=2
spawn-chunk-radius=0
spawn-protection=0
max-players=2
max-tick-time=120000
enable-rcon=false
enable-query=false
motd=Isolated riftPlots integration test
'@, $utf8)
}
if (-not ((Get-Content -LiteralPath $properties) -match '^server-ip=127\.0\.0\.1$')) { throw 'Integration server must bind only to 127.0.0.1.' }
$pluginJar = Join-Path $projectRoot 'build/libs/riftPlots-1.0.0.jar'
$probeJar = Join-Path $projectRoot 'build/integration/riftplots-probe.jar'
if (-not (Test-Path -LiteralPath $pluginJar) -or -not (Test-Path -LiteralPath $probeJar)) { throw 'Run gradlew build integrationProbeJar before preparing the server.' }
Copy-Item -LiteralPath $pluginJar -Destination (Join-Path $serverRoot 'plugins/riftPlots.jar')
Copy-Item -LiteralPath $probeJar -Destination (Join-Path $serverRoot 'plugins/RiftPlotsProbe.jar')
if ($PrepareOnly) { Write-Host "Prepared isolated test directory: $serverRoot"; return }
$eulaFile = Join-Path $serverRoot 'eula.txt'
if ($AcceptEula) { [IO.File]::WriteAllText($eulaFile, "# Accepted explicitly for the isolated test server`neula=true`n", $utf8) }
if (-not (Test-Path -LiteralPath $eulaFile) -or -not ((Get-Content -LiteralPath $eulaFile) -match '^eula=true$')) {
    throw 'Minecraft EULA acceptance is required before starting. Read https://www.minecraft.net/en-us/eula and use -AcceptEula only after explicit acceptance.'
}
Push-Location -LiteralPath $serverRoot
try {
    $startedAt = [DateTime]::UtcNow
    & java '--enable-native-access=ALL-UNNAMED' '-Dterminal.jline=false' '-Dterminal.ansi=false' -Xms512M -Xmx1536M -jar $paper --nogui
    if ($LASTEXITCODE -ne 0) { throw "Test server exited with code $LASTEXITCODE" }
    $report = Join-Path $serverRoot 'plugins/RiftPlotsProbe/report.yml'
    if (-not (Test-Path -LiteralPath $report) -or (Get-Item -LiteralPath $report).LastWriteTimeUtc -lt $startedAt -or -not ((Get-Content -LiteralPath $report) -match '^passed: true$')) { throw 'Integration probe failed; inspect the server log and probe report.' }
}
finally { Pop-Location }
