# 本机起 booth-cloud 业务 jar（与 Docker 业务容器二选一，勿同时占 8080-8083）
# 推荐 Docker 开箱：docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d
# powershell -File booth-cloud\deploy\start-apps.ps1
$ErrorActionPreference = "Stop"

$jdk = if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
  "$env:JAVA_HOME\bin\java.exe"
} elseif (Test-Path "D:\JAVA\jdk17\bin\java.exe") {
  "D:\JAVA\jdk17\bin\java.exe"
} else {
  throw "需要 JDK 17。请设置 JAVA_HOME，或安装到 D:\JAVA\jdk17"
}

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$env:JAVA_HOME = Split-Path (Split-Path $jdk)
$env:Path = "$(Split-Path $jdk);$env:Path"
# 本机 jar 演示：打开 Mock 支付与 API 文档；Actuator 仍默认 health,info
$env:BOOTH_PAY_MOCK_ENABLED = "true"
$env:BOOTH_PAY_ALLOW_DEV_SECRET = "true"
$env:API_DOCS_ENABLED = "true"
if (-not $env:MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE) {
  $env:MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE = "health,info"
}

$apps = @(
  "booth-user\target\booth-user-1.0.0-SNAPSHOT.jar",
  "booth-product\target\booth-product-1.0.0-SNAPSHOT.jar",
  "booth-trade\target\booth-trade-1.0.0-SNAPSHOT.jar",
  "booth-gateway\target\booth-gateway-1.0.0-SNAPSHOT.jar"
)

foreach ($rel in $apps) {
  $jar = Join-Path $root $rel
  if (-not (Test-Path $jar)) {
    throw "缺少 $jar ，先在 booth-cloud 根目录执行: mvn -DskipTests package"
  }
  Write-Host "starting $rel ..."
  Start-Process -FilePath $jdk -ArgumentList @("-jar", $jar) -WorkingDirectory (Split-Path $jar) -WindowStyle Minimized
}

Write-Host ""
Write-Host "已启动 user / product / trade / gateway。"
Write-Host "Docker 全量后端: docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d"
Write-Host "前端:   cd booth-web; npm run dev"
Write-Host "Agent:  cd booth-agent; docker compose up -d --build  （或本机 java -jar）"
