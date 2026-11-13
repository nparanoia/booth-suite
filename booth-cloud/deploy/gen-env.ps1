# 生成 deploy/.env（含 Nacos 必填 token）。已存在则拒绝覆盖，除非 -Force。
param(
    [switch]$Force
)

$ErrorActionPreference = "Stop"
$here = $PSScriptRoot
$example = Join-Path $here ".env.example"
$target = Join-Path $here ".env"

if (-not (Test-Path $example)) {
    throw "缺少 $example"
}
if ((Test-Path $target) -and -not $Force) {
    Write-Host "已存在 deploy/.env。若要重生成请加 -Force（会覆盖）。"
    exit 0
}

function New-Base64Token([int]$bytes = 32) {
    $buf = New-Object byte[] $bytes
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($buf)
    return [Convert]::ToBase64String($buf)
}

$token = New-Base64Token 32
$identity = -join ((1..32) | ForEach-Object { "abcdefghijklmnopqrstuvwxyz0123456789"[(Get-Random -Max 36)] })

$lines = Get-Content $example
$out = foreach ($line in $lines) {
    if ($line -match '^\s*NACOS_AUTH_TOKEN\s*=') {
        "NACOS_AUTH_TOKEN=$token"
    } elseif ($line -match '^\s*NACOS_AUTH_IDENTITY_VALUE\s*=') {
        "NACOS_AUTH_IDENTITY_VALUE=$identity"
    } elseif ($line -match '^\s*BOOTH_PAY_MOCK_ENABLED\s*=') {
        "BOOTH_PAY_MOCK_ENABLED=true"
    } elseif ($line -match '^\s*BOOTH_PAY_ALLOW_DEV_SECRET\s*=') {
        "BOOTH_PAY_ALLOW_DEV_SECRET=true"
    } elseif ($line -match '^\s*API_DOCS_ENABLED\s*=') {
        "API_DOCS_ENABLED=true"
    } elseif ($line -match '^\s*MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE\s*=') {
        "MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info"
    } else {
        $line
    }
}
$utf8 = New-Object System.Text.UTF8Encoding $false; [IO.File]::WriteAllLines($target, @($out), $utf8)
Write-Host "已写入 $target"
Write-Host "NACOS_AUTH_TOKEN 与 NACOS_AUTH_IDENTITY_VALUE 已随机生成；勿提交到 Git。"
Write-Host "本机演示已默认打开：Mock 支付 + API 文档（Swagger/Knife4j）。Actuator 仍仅 health,info。"

