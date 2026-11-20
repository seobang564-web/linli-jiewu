$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$jarPath = Join-Path $projectRoot 'server/target/app.jar'
if (-not (Test-Path -LiteralPath $jarPath)) {
    throw '没有可部署的 app.jar。先运行 .\scripts\Build-Week1.ps1 -Component Server。'
}
$stagePath = Join-Path $projectRoot '.artifacts/linli-week2-deploy'
New-Item -ItemType Directory -Path $stagePath -Force | Out-Null
Copy-Item -LiteralPath $jarPath -Destination (Join-Path $stagePath 'app.jar') -Force
Copy-Item -LiteralPath (Join-Path $projectRoot 'server/src/main/resources/application-example.yml') -Destination (Join-Path $stagePath 'application-prod.yml') -Force
foreach ($name in @('deploy.sh', 'linli.service')) {
    Copy-Item -LiteralPath (Join-Path $projectRoot "server/deploy/$name") -Destination (Join-Path $stagePath $name) -Force
}
Copy-Item -LiteralPath (Join-Path $projectRoot 'docs/migrations/002_images.sql') -Destination (Join-Path $stagePath '002_images.sql') -Force
$names = @('app.jar', 'application-prod.yml', 'deploy.sh', 'linli.service', '002_images.sql')
$manifest = foreach ($name in $names) {
    $hash = (Get-FileHash -LiteralPath (Join-Path $stagePath $name) -Algorithm SHA256).Hash.ToLowerInvariant()
    "$hash  $name"
}
[IO.File]::WriteAllText((Join-Path $stagePath 'SHA256SUMS'), ($manifest -join "`n") + "`n", [Text.UTF8Encoding]::new($false))
$archivePath = Join-Path $projectRoot '.artifacts/linli-week2-deploy.zip'
$files = @($names + 'SHA256SUMS') | ForEach-Object { Join-Path $stagePath $_ }
Compress-Archive -LiteralPath $files -DestinationPath $archivePath -Force
Write-Host "部署包已生成：$archivePath"
Write-Host '包内包含保留已有数据的 002_images.sql；不含数据库密码或 JWT 密钥。部署前先执行升级脚本。'
