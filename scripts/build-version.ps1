param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string] $Version,

    [switch] $NoClean
)

$ErrorActionPreference = "Stop"

$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$Targets = @{
    "master"    = @{ Path = $Root; Version = "master" }
    "current"   = @{ Path = $Root; Version = "master" }
    "1.21"      = @{ Path = $Root; Version = "master" }
    "1.21.6"    = @{ Path = (Join-Path $Root "versions\1.21.6-8"); Version = "1.21.6-8" }
    "1.21.6-8"  = @{ Path = (Join-Path $Root "versions\1.21.6-8"); Version = "1.21.6-8" }
    "1.21.9"    = @{ Path = (Join-Path $Root "versions\1.21.9-11"); Version = "1.21.9-11" }
    "1.21.9-11" = @{ Path = (Join-Path $Root "versions\1.21.9-11"); Version = "1.21.9-11" }
    "26.1"      = @{ Path = (Join-Path $Root "versions\26.1"); Version = "26.1" }
}

if ($Version -eq "all") {
    & (Join-Path $PSScriptRoot "build-all.ps1") -NoClean:$NoClean
    exit $LASTEXITCODE
}

if (-not $Targets.ContainsKey($Version)) {
    $knownVersions = ($Targets.Keys | Sort-Object) -join ", "
    throw "Unknown version '$Version'. Known versions: $knownVersions, all"
}

$Target = $Targets[$Version]
$ProjectDir = $Target.Path
$OutputVersion = $Target.Version

if (-not (Test-Path $ProjectDir)) {
    throw "Version folder does not exist: $ProjectDir"
}

$GradleBat = Join-Path $ProjectDir "gradlew.bat"
if (-not (Test-Path $GradleBat)) {
    throw "Gradle wrapper was not found: $GradleBat"
}

$OldJavaHome = $env:JAVA_HOME
$OldPath = $env:Path
$Jdk21 = "C:\Program Files\Java\jdk-21.0.10"

try {
    $GradleProperties = Join-Path $ProjectDir "gradle.properties"
    $UsesPinnedJava = (Test-Path $GradleProperties) -and ((Get-Content $GradleProperties) -match "^org\.gradle\.java\.home=")

    if (-not $UsesPinnedJava -and (Test-Path $Jdk21)) {
        $env:JAVA_HOME = $Jdk21
        $env:Path = "$env:JAVA_HOME\bin;$env:Path"
    }

    $GradleArgs = @()
    if (-not $NoClean) {
        $GradleArgs += "clean"
    }
    $GradleArgs += "build"

    Write-Host "Building $OutputVersion in $ProjectDir"
    Push-Location $ProjectDir
    try {
        & $GradleBat @GradleArgs
        $ExitCode = $LASTEXITCODE
    } finally {
        Pop-Location
    }

    if ($ExitCode -ne 0) {
        exit $ExitCode
    }

    $DistDir = Join-Path $Root "dist"
    New-Item -ItemType Directory -Force $DistDir | Out-Null
    Get-ChildItem $DistDir -Filter "*-$OutputVersion*.jar" -File | Remove-Item -Force

    $JarFiles = Get-ChildItem (Join-Path $ProjectDir "build\libs") -Filter "*.jar" -File
    foreach ($JarFile in $JarFiles) {
        $OutputName = if ($JarFile.BaseName.EndsWith("-sources")) {
            $BaseName = $JarFile.BaseName.Substring(0, $JarFile.BaseName.Length - "-sources".Length)
            "$BaseName-$OutputVersion-sources$($JarFile.Extension)"
        } else {
            "$($JarFile.BaseName)-$OutputVersion$($JarFile.Extension)"
        }

        Copy-Item -Force $JarFile.FullName (Join-Path $DistDir $OutputName)
    }

    Write-Host "Copied versioned jars to $DistDir"
} finally {
    $env:JAVA_HOME = $OldJavaHome
    $env:Path = $OldPath
}
