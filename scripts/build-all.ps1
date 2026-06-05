param(
    [switch] $NoClean
)

$ErrorActionPreference = "Stop"
$Versions = @("master", "1.21.6-8", "1.21.9-11", "26.1")

foreach ($Version in $Versions) {
    & (Join-Path $PSScriptRoot "build-version.ps1") $Version -NoClean:$NoClean
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}
