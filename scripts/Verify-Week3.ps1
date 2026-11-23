param(
    [uri]$BaseUrl = 'http://127.0.0.1:18080/api/',
    [switch]$AllowRemote
)

$ErrorActionPreference = 'Stop'
if ($BaseUrl.Scheme -notin @('http', 'https') -or -not $BaseUrl.AbsoluteUri.EndsWith('/') -or
    $BaseUrl.UserInfo -or $BaseUrl.Query -or $BaseUrl.Fragment) {
    throw 'BaseUrl 必须是以 / 结尾且不含凭据、查询或片段的 HTTP(S) 地址。'
}
if (-not $BaseUrl.IsLoopback -and -not $AllowRemote) {
    throw '本脚本会创建测试账户和物品。默认仅允许本地联调；明确验收远端时才加 -AllowRemote。'
}
$checks = [Collections.Generic.List[object]]::new()

function Invoke-BorrowApi {
    param([string]$Name, [string]$Method, [string]$Path, $Body, [string]$Token,
          [int]$ExpectedStatus = 200, [int]$ExpectedCode = 0)
    $arguments = @{
        Uri = [uri]::new($BaseUrl, $Path)
        Method = $Method
        TimeoutSec = 30
        SkipHttpErrorCheck = $true
        Headers = @{}
        ContentType = 'application/json; charset=utf-8'
    }
    if ($Token) { $arguments.Headers.Authorization = "Bearer $Token" }
    if ($null -ne $Body) {
        $arguments.Body = [Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 8 -Compress))
    }
    $response = Invoke-WebRequest @arguments
    $envelope = $response.Content | ConvertFrom-Json
    if ([int]$response.StatusCode -ne $ExpectedStatus -or $envelope.code -ne $ExpectedCode) {
        throw "$Name 失败：HTTP $($response.StatusCode)，code=$($envelope.code)"
    }
    $checks.Add([pscustomobject]@{ check = $Name; httpStatus = [int]$response.StatusCode; code = $envelope.code; result = 'PASS' })
    return $envelope.data
}

function Assert-BorrowValue {
    param([string]$Name, [bool]$Condition)
    if (-not $Condition) { throw "$Name 失败" }
    $checks.Add([pscustomobject]@{ check = $Name; result = 'PASS' })
}

Invoke-BorrowApi '服务健康' GET 'health' | Out-Null
$suffix = [guid]::NewGuid().ToString('N').Substring(0, 12)
$password = [guid]::NewGuid().ToString('N')
$owner = Invoke-BorrowApi '注册物主' POST 'auth/register' @{
    username = "w3o$suffix"; password = $password; nickname = '【测试】第三周物主'; contact = '【测试】物主联系方式'
}
$borrower = Invoke-BorrowApi '注册借用者' POST 'auth/register' @{
    username = "w3b$suffix"; password = $password; nickname = '【测试】第三周借用者'; contact = '【测试】借用者联系方式'
}
$item = Invoke-BorrowApi '发布测试物品' POST 'items' @{
    title = "【测试】第三周借还帐篷-$suffix"; category = 'CAMPING'; description = '真实 HTTP 借还验收'
    pickupLat = 30.5123456; pickupLng = 114.4123456; pickupLabel = '【测试】第三周交接点'
} $owner.token
$nearbyPath = 'items?lat=30.5123456&lng=114.4123456&radius=1000&keyword=' + [uri]::EscapeDataString($item.title)
$nearby = Invoke-BorrowApi '附近真实物品检索' GET $nearbyPath
Assert-BorrowValue '附近结果包含新发布物品' (@($nearby.list | Where-Object { $_.id -eq $item.id }).Count -eq 1)
$application = @{
    itemId = $item.id; dueAt = [DateTimeOffset]::UtcNow.AddDays(3).ToString('o')
    note = '【测试】第三周借用'; requestId = [guid]::NewGuid().ToString()
}
$order = Invoke-BorrowApi '提交借用申请' POST 'borrows' $application $borrower.token
$id = $order.id
Assert-BorrowValue '待确认隐藏双方联系方式' ($order.status -eq 'PENDING' -and $null -eq $order.owner.contact -and $null -eq $order.borrower.contact)
$reserved = Invoke-BorrowApi '预约后附近可借列表' GET $nearbyPath
Assert-BorrowValue '预约物品从可借列表移除' ($reserved.total -eq 0)
$repeated = Invoke-BorrowApi '重复申请幂等' POST 'borrows' $application $borrower.token
Assert-BorrowValue '重复申请返回同一订单' ($repeated.id -eq $id)
Invoke-BorrowApi '借用者不能接受申请' POST "borrows/$id/accept" $null $borrower.token 403 40301 | Out-Null
Invoke-BorrowApi '活动订单禁止物品下架' POST "items/$($item.id)/offline" @{} $owner.token 409 40901 | Out-Null
$order = Invoke-BorrowApi '物主接受申请' POST "borrows/$id/accept" $null $owner.token
Assert-BorrowValue '接受后双方可见联系方式' ($order.status -eq 'ACCEPTED' -and $order.owner.contact -and $order.borrower.contact)
Invoke-BorrowApi '重复接受返回状态冲突' POST "borrows/$id/accept" $null $owner.token 409 40901 | Out-Null
Invoke-BorrowApi '物主确认交接' POST "borrows/$id/handover" $null $owner.token | Out-Null
$lent = Invoke-BorrowApi '查看借出物品状态' GET "items/$($item.id)" $null $borrower.token
Assert-BorrowValue '交接后物品借出中' ($lent.status -eq 'LENT')
Invoke-BorrowApi '借用者申请归还' POST "borrows/$id/return" $null $borrower.token | Out-Null
Invoke-BorrowApi '物主拒绝验收' POST "borrows/$id/deny-return" @{ reason = '【测试】需补充清洁' } $owner.token | Out-Null
Invoke-BorrowApi '借用者再次申请归还' POST "borrows/$id/return" $null $borrower.token | Out-Null
$order = Invoke-BorrowApi '物主确认归还' POST "borrows/$id/confirm-return" $null $owner.token
Assert-BorrowValue '订单完成' ($order.status -eq 'COMPLETED' -and -not $order.overdue)
$available = Invoke-BorrowApi '查看归还后物品' GET "items/$($item.id)" $null $borrower.token
Assert-BorrowValue '归还后物品恢复可借' ($available.status -eq 'AVAILABLE')
$restored = Invoke-BorrowApi '归还后附近可借列表' GET $nearbyPath
Assert-BorrowValue '归还后重新出现在可借列表' (@($restored.list | Where-Object { $_.id -eq $item.id }).Count -eq 1)
$logs = @(Invoke-BorrowApi '双方读取订单日志' GET "borrows/$id/logs" $null $borrower.token)
Assert-BorrowValue '七次状态日志顺序正确' (($logs.toStatus -join ',') -eq 'PENDING,ACCEPTED,BORROWING,RETURNING,BORROWING,RETURNING,COMPLETED')
$mine = Invoke-BorrowApi '物主订单分页' GET 'borrows?role=owner&status=COMPLETED&page=1&size=20' $null $owner.token
Assert-BorrowValue '本人订单列表包含完成订单' (@($mine.list | Where-Object { $_.id -eq $id }).Count -eq 1)
$repeated = Invoke-BorrowApi '结束后重试原申请' POST 'borrows' $application $borrower.token
Assert-BorrowValue '原幂等号返回已完成订单' ($repeated.id -eq $id -and $repeated.status -eq 'COMPLETED')
Invoke-BorrowApi '验收测试物品下架' POST "items/$($item.id)/offline" @{} $owner.token | Out-Null

$projectRoot = Split-Path -Parent $PSScriptRoot
$resultPath = Join-Path $projectRoot '.artifacts/week3-http-results.json'
New-Item -ItemType Directory -Path (Split-Path -Parent $resultPath) -Force | Out-Null
$result = [ordered]@{
    checkedAt = [DateTimeOffset]::Now.ToString('o'); loopback = $BaseUrl.IsLoopback
    itemId = $item.id; borrowId = $id; passed = $checks.Count; checks = @($checks.ToArray())
}
[IO.File]::WriteAllText($resultPath, ($result | ConvertTo-Json -Depth 8), [Text.UTF8Encoding]::new($false))
Write-Host "PASS：第三周真实 HTTP 验收 $($checks.Count) 项；结果：$resultPath（不包含密码或 Token）"
