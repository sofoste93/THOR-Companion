param([string]$Version = "1.0.0")

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

function Assert-ProjectChild([string]$Path) {
    $root = [IO.Path]::GetFullPath($projectRoot).TrimEnd('\') + '\'
    $resolved = [IO.Path]::GetFullPath($Path)
    if (-not $resolved.StartsWith($root, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to modify a path outside the project: $resolved"
    }
    return $resolved
}

mvn clean verify

$inputDirectory = Assert-ProjectChild (Join-Path $projectRoot "target\jpackage-input")
$destination = Assert-ProjectChild (Join-Path $projectRoot "dist")
Remove-Item $inputDirectory -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item $destination -Recurse -Force -ErrorAction SilentlyContinue
New-Item $inputDirectory -ItemType Directory | Out-Null
New-Item $destination -ItemType Directory | Out-Null
Copy-Item "target\thor-companion-$Version-all.jar" (Join-Path $inputDirectory "app.jar")

jpackage --type app-image --name "THOR Companion" --app-version $Version `
    --vendor "Sofoste" --input $inputDirectory --main-jar "app.jar" `
    --main-class "com.sofoste.thorcompanion.Launcher" --dest $destination

$archive = Join-Path $destination "THOR-Companion-Windows-x64.zip"
Compress-Archive -LiteralPath (Join-Path $destination "THOR Companion") -DestinationPath $archive
Write-Output "Package created: $archive"
