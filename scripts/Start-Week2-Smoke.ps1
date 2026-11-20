param(
    [string]$JavaHome = 'C:\Program Files\Java\jdk-17',
    [int]$Port = 18080
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$jarPath = Join-Path $projectRoot 'server/target/app.jar'
$h2Path = Join-Path $projectRoot '.tools/maven-repository/com/h2database/h2/2.3.232/h2-2.3.232.jar'
if (-not (Test-Path -LiteralPath $jarPath) -or -not (Test-Path -LiteralPath $h2Path)) {
    throw '先运行 scripts/Build-Week1.ps1 -Component Server，生成 JAR 和本地测试依赖。'
}
$hash = (Get-FileHash -LiteralPath $jarPath -Algorithm SHA256).Hash.Substring(0, 12)
$runtimePath = Join-Path $projectRoot ".tools/week2-smoke-$hash"
if (-not (Test-Path -LiteralPath $runtimePath)) {
    [IO.Compression.ZipFile]::ExtractToDirectory($jarPath, $runtimePath)
}
$classPath = @((Join-Path $projectRoot 'server/target/test-classes'),
    (Join-Path $runtimePath 'BOOT-INF/classes'), (Join-Path $runtimePath 'BOOT-INF/lib/*'), $h2Path) -join [IO.Path]::PathSeparator
Write-Host "本地 H2 联调服务：http://127.0.0.1:$Port/api/；重启后数据清空，不连接云端数据库。"
& (Join-Path $JavaHome 'bin/java.exe') '-Dfile.encoding=UTF-8' '-Duser.timezone=Asia/Shanghai' '-cp' $classPath `
    'com.linlijiewu.server.ServerApplication' '--spring.profiles.active=test' '--server.address=127.0.0.1' "--server.port=$Port" `
    "--linli.upload-dir=$(Join-Path $runtimePath 'uploads')" '--spring.datasource.url=jdbc:h2:mem:week2-smoke;MODE=MySQL;DB_CLOSE_DELAY=-1'
if ($LASTEXITCODE -ne 0) { throw "本地联调服务退出：$LASTEXITCODE" }
