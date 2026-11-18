param(
    [ValidateSet('All', 'Server', 'Android')]
    [string]$Component = 'All',
    [string]$JavaHome,
    [string]$AndroidSdk
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$javaCandidates = @($JavaHome, $env:JAVA_HOME)
foreach ($vendor in @('Microsoft', 'Eclipse Adoptium', 'Java')) {
    $vendorPath = Join-Path $env:ProgramFiles $vendor
    if (Test-Path -LiteralPath $vendorPath) {
        $javaCandidates += Get-ChildItem -LiteralPath $vendorPath -Directory | Select-Object -ExpandProperty FullName
    }
}
$selectedJava = $javaCandidates | Where-Object {
    $_ -and (Test-Path -LiteralPath (Join-Path $_ 'release')) -and
    (Select-String -LiteralPath (Join-Path $_ 'release') -Pattern '^JAVA_VERSION="17[."]' -Quiet)
} | Select-Object -First 1
if (-not $selectedJava) {
    throw '未找到 JDK 17。安装后使用 -JavaHome 指定 JDK 目录。'
}
$env:JAVA_HOME = $selectedJava
$env:PATH = "$(Join-Path $selectedJava 'bin');$env:PATH"
Write-Host "Using JDK 17: $selectedJava"

if ($Component -in @('All', 'Server')) {
    $env:MAVEN_USER_HOME = Join-Path $projectRoot '.tools/maven-user-home'
    $mavenRepository = Join-Path $projectRoot '.tools/maven-repository'
    Push-Location (Join-Path $projectRoot 'server')
    try {
        & .\mvnw.cmd "-Dmaven.repo.local=$mavenRepository" '-B' '-ntp' 'verify'
        if ($LASTEXITCODE -ne 0) { throw "服务端构建失败，退出码 $LASTEXITCODE" }
    } finally {
        Pop-Location
    }
}

if ($Component -in @('All', 'Android')) {
    if ($AndroidSdk) { $env:ANDROID_HOME = $AndroidSdk }
    $env:GRADLE_USER_HOME = Join-Path $env:USERPROFILE '.gradle'
    Push-Location (Join-Path $projectRoot 'android')
    try {
        & .\gradlew.bat '--no-daemon' '--console=plain' ':app:assembleDebug' ':app:testDebugUnitTest' ':app:lintDebug'
        if ($LASTEXITCODE -ne 0) { throw "Android 构建失败，退出码 $LASTEXITCODE" }
    } finally {
        Pop-Location
    }
}
