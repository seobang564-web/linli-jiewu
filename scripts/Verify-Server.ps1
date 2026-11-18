param(
    [Parameter(Mandatory = $true)]
    [uri]$BaseUrl
)

$ErrorActionPreference = 'Stop'
if ($BaseUrl.Scheme -notin @('http', 'https') -or -not $BaseUrl.AbsoluteUri.EndsWith('/') -or $BaseUrl.UserInfo) {
    throw 'BaseUrl 必须是以 / 结尾且不含凭据的 HTTP(S) 地址。'
}
foreach ($endpoint in @('hello', 'health')) {
    $url = [uri]::new($BaseUrl, $endpoint)
    $result = Invoke-RestMethod -Uri $url -TimeoutSec 15
    if ($result.code -ne 0 -or -not $result.data) { throw "$endpoint 未返回成功响应" }
    if ($endpoint -eq 'hello' -and $result.data.service -ne 'linli-jiewu') {
        throw 'Hello 响应不是邻里借物服务。'
    }
    if ($endpoint -eq 'health' -and
        ($result.data.status -ne 'UP' -or $result.data.database -ne 'UP' -or $result.data.schema -ne 'UP')) {
        throw '数据库或基础表尚未就绪。'
    }
    Write-Host "PASS $url"
}
