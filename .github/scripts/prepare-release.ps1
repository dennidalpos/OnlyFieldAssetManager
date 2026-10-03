param(
    [string]$Workspace = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path,
    [string]$OutputDirectory = (Join-Path $Workspace 'build/release')
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$apk = Join-Path $Workspace 'mobile/app/build/outputs/apk/debug/app-debug.apk'
if (!(Test-Path -LiteralPath $apk -PathType Leaf) -or (Get-Item -LiteralPath $apk).Length -eq 0) {
    throw 'Debug APK missing or empty'
}
$archives = @(Get-ChildItem -LiteralPath (Join-Path $Workspace 'dist') -Filter 'OnlyFieldAssetManager-portable-x64-*.zip' -File)
if ($archives.Count -ne 1) { throw 'Expected exactly one portable Windows ZIP' }

# Check the actual ZIP before copying any release payload.
$zip = [System.IO.Compression.ZipFile]::OpenRead($archives[0].FullName)
try {
    if ($zip.Entries.FullName -match '(^|[/\\])data([/\\]|$)') { throw 'Portable ZIP contains a data directory' }
    if ('OnlyFieldAssetManager/OnlyFieldAssetManager.exe' -notin $zip.Entries.FullName) {
        throw 'Portable executable missing from ZIP'
    }
} finally { $zip.Dispose() }

if ((Test-Path -LiteralPath $OutputDirectory) -and @(Get-ChildItem -LiteralPath $OutputDirectory -Force).Count -ne 0) {
    throw 'Release output directory must be empty'
}
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
Copy-Item -LiteralPath $apk -Destination (Join-Path $OutputDirectory 'OnlyFieldAssetManager-debug.apk')
Copy-Item -LiteralPath $archives[0].FullName -Destination (Join-Path $OutputDirectory $archives[0].Name)
$checksums = Get-ChildItem -LiteralPath $OutputDirectory -File | Sort-Object Name | ForEach-Object {
    '{0}  {1}' -f (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant(), $_.Name
}
$checksums | Set-Content -LiteralPath (Join-Path $OutputDirectory 'SHA256SUMS') -Encoding ascii
Write-Output "Prepared $($checksums.Count) packages with SHA-256 checksums"
