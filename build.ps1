$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$ProjectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ToolRoot = Join-Path $env:USERPROFILE '.minetogether-build-tools'
$JdkRoot = Join-Path $ToolRoot 'jdk21'
$GradleVersion = '8.10.2'
$GradleRoot = Join-Path $ToolRoot "gradle-$GradleVersion"
New-Item -ItemType Directory -Force -Path $ToolRoot | Out-Null

function Find-Jdk21 {
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    $javacCommand = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($javacCommand) { $candidates += (Split-Path -Parent (Split-Path -Parent $javacCommand.Source)) }
    if ($env:ProgramFiles) {
        $candidates += (Join-Path $env:ProgramFiles 'Eclipse Adoptium')
        $candidates += (Join-Path $env:ProgramFiles 'Microsoft')
        $candidates += (Join-Path $env:ProgramFiles 'Java')
    }
    $candidates += $JdkRoot

    foreach ($candidate in ($candidates | Select-Object -Unique)) {
        if (-not $candidate -or -not (Test-Path $candidate)) { continue }
        if ((Test-Path (Join-Path $candidate 'bin\javac.exe')) -and
            ((& (Join-Path $candidate 'bin\javac.exe') -version 2>&1 | Out-String) -match '21')) {
            return $candidate
        }
        if (Test-Path $candidate -PathType Container) {
            $found = Get-ChildItem -Path $candidate -Directory -ErrorAction SilentlyContinue |
                Where-Object { Test-Path (Join-Path $_.FullName 'bin\javac.exe') } |
                Select-Object -First 1
            if ($found) {
                $version = & (Join-Path $found.FullName 'bin\javac.exe') -version 2>&1 | Out-String
                if ($version -match '21') { return $found.FullName }
            }
        }
    }
    return $null
}

Write-Host '[1/4] Kiem tra JDK 21...' -ForegroundColor Cyan
$JdkHome = Find-Jdk21
if (-not $JdkHome) {
    Write-Host 'Chua co JDK 21. Dang tai JDK 21 (Temurin)...' -ForegroundColor Yellow
    New-Item -ItemType Directory -Force -Path $JdkRoot | Out-Null
    $JdkZip = Join-Path $ToolRoot 'temurin-jdk21.zip'
    Invoke-WebRequest -Uri 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse' -OutFile $JdkZip
    Expand-Archive -LiteralPath $JdkZip -DestinationPath $JdkRoot -Force
    Remove-Item $JdkZip -Force
    $JdkHome = Find-Jdk21
}
if (-not $JdkHome) { throw 'Khong tim thay JDK 21.' }
$env:JAVA_HOME = $JdkHome
$env:Path = (Join-Path $JdkHome 'bin') + ';' + $env:Path
& (Join-Path $JdkHome 'bin\java.exe') -version
& (Join-Path $JdkHome 'bin\javac.exe') -version

Write-Host '[2/4] Kiem tra Gradle...' -ForegroundColor Cyan
if (-not (Test-Path (Join-Path $GradleRoot 'bin\gradle.bat'))) {
    $GradleZip = Join-Path $ToolRoot "gradle-$GradleVersion-bin.zip"
    Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip" -OutFile $GradleZip
    Expand-Archive -LiteralPath $GradleZip -DestinationPath $ToolRoot -Force
    Remove-Item $GradleZip -Force
}
$GradleExe = Join-Path $GradleRoot 'bin\gradle.bat'
if (-not (Test-Path $GradleExe)) { throw 'Khong tim thay Gradle.' }

Write-Host '[3/4] Build bang Fabric Loom...' -ForegroundColor Cyan
Push-Location $ProjectDir
try {
    & $GradleExe --no-daemon clean build
    if ($LASTEXITCODE -ne 0) { throw 'Gradle build that bai.' }

    $Candidates = Get-ChildItem (Join-Path $ProjectDir 'build\libs') -Filter '*.jar' |
        Where-Object { $_.Name -notmatch 'sources|dev' } |
        Sort-Object Length -Descending
    if (-not $Candidates -or $Candidates[0].Length -lt 10000) {
        throw 'Khong tim thay JAR remapped co kich thuoc hop le.'
    }
    $BuiltJar = $Candidates[0].FullName

    Write-Host '[4/4] Kiem tra va sao chep JAR...' -ForegroundColor Cyan
    $JarExe = Join-Path $JdkHome 'bin\jar.exe'
    $JarList = & $JarExe tf $BuiltJar
    if ($LASTEXITCODE -ne 0 -or ($JarList -notcontains 'fabric.mod.json') -or
        ($JarList -notcontains 'com/minetogether/MineTogetherClient.class') -or
        ($JarList -notcontains 'com/minetogether/screen/MineTogetherScreen.class')) {
        throw 'JAR khong co du metadata va class MineTogether.'
    }

    $ReleaseDir = Join-Path $ProjectDir 'release'
    New-Item -ItemType Directory -Force -Path $ReleaseDir | Out-Null
    $ReleaseJar = Join-Path $ReleaseDir 'MineTogether-0.3.0.jar'
    Copy-Item -LiteralPath $BuiltJar -Destination $ReleaseJar -Force
    Write-Host "JAR da tao: $ReleaseJar" -ForegroundColor Green
}
finally { Pop-Location }
