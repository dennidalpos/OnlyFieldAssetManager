$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$scratchRoot = Join-Path $repoRoot 'build/tmp'
$fixture = Join-Path $scratchRoot ('portable-data-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $fixture -Force | Out-Null
try {
    $source = Join-Path $fixture 'build/compose/binaries/main/app/OnlyFieldAssetManager'
    $target = Join-Path $fixture 'dist/OnlyFieldAssetManager'
    foreach ($base in @($source, $target)) {
        foreach ($path in @('data/projects/same.ofam', 'data/media/same.png', 'data/settings.properties')) {
            $file = Join-Path $base $path
            New-Item -ItemType Directory -Path (Split-Path $file) -Force | Out-Null
            [IO.File]::WriteAllText($file, $(if ($base -eq $source) { 'SOURCE' } else { 'TARGET' }))
        }
    }
    [IO.File]::WriteAllText((Join-Path $source 'data/source-only.txt'), 'SOURCE')
    [IO.File]::WriteAllText((Join-Path $target 'data/target-only.txt'), 'TARGET')
    [IO.File]::WriteAllText((Join-Path $source 'OnlyFieldAssetManager.exe'), 'SYNTHETIC_RUNTIME')
    [IO.File]::WriteAllText((Join-Path $target 'obsolete.txt'), 'OLD_RUNTIME')
    $before = @{}
    Get-ChildItem -LiteralPath (Join-Path $target 'data') -File -Recurse | ForEach-Object {
        $before[$_.FullName] = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash
    }
    # Execute the production task definitions against an isolated synthetic runtime.
    $definition = Get-Content -LiteralPath (Join-Path $repoRoot 'pc/app/build.gradle.kts') -Raw
    $definition = $definition.Substring($definition.IndexOf('val portableAppName ='))
    $definition = $definition.Replace('    dependsOn("createDistributable")', '')
    $definition = $definition.Replace('compose.desktop.application.nativeDistributions.packageVersion', '"test"')
    [IO.File]::WriteAllText((Join-Path $fixture 'build.gradle.kts'), $definition)
    [IO.File]::WriteAllText((Join-Path $fixture 'settings.gradle.kts'), 'rootProject.name = "portable-data-test"')
    & (Join-Path $repoRoot 'gradlew.bat') -p $fixture packagePortable --no-parallel --max-workers=1
    if ($LASTEXITCODE -ne 0) { throw "Gradle fixture failed: $LASTEXITCODE" }
    foreach ($file in $before.Keys) {
        if (!(Test-Path -LiteralPath $file) -or (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash -ne $before[$file]) {
            throw "Destination data changed: $file"
        }
    }
    if (Test-Path -LiteralPath (Join-Path $target 'data/source-only.txt')) { throw 'Source data was introduced' }
    if (Test-Path -LiteralPath (Join-Path $target 'obsolete.txt')) { throw 'Obsolete runtime was retained' }
    if ((Get-ChildItem -LiteralPath (Join-Path $target 'data') -File -Recurse).Count -ne $before.Count) { throw 'Data inventory changed' }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead((Join-Path $fixture 'dist/OnlyFieldAssetManager-portable-x64-test.zip'))
    try {
        if ($archive.Entries.FullName -match '^OnlyFieldAssetManager/data/') { throw 'ZIP contains data' }
        if ('OnlyFieldAssetManager/OnlyFieldAssetManager.exe' -notin $archive.Entries.FullName) { throw 'ZIP runtime missing' }
    } finally { $archive.Dispose() }
    Write-Output 'PASS: destination hashes/inventory unchanged, source data excluded, ZIP contains runtime only'
} finally {
    $resolvedFixture = (Resolve-Path -LiteralPath $fixture).Path
    if (!$resolvedFixture.StartsWith($scratchRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Fixture cleanup escaped build/tmp'
    }
    Remove-Item -LiteralPath $resolvedFixture -Recurse -Force
}
