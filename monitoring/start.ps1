# Local dev only — starts Prometheus, Alertmanager, and Grafana as standalone containers.
# Usage:
#   $env:TELEGRAM_BOT_TOKEN = "your_token"
#   $env:TELEGRAM_CHAT_ID   = "your_chat_id"
#   .\monitoring\start.ps1
param()

$ErrorActionPreference = "Stop"

$token  = $env:TELEGRAM_BOT_TOKEN
$chatId = $env:TELEGRAM_CHAT_ID

if (-not $token -or -not $chatId) {
    Write-Error "Set TELEGRAM_BOT_TOKEN and TELEGRAM_CHAT_ID before running this script."
    exit 1
}

# Absolute path with forward slashes — required for Docker Desktop volume mounts on Windows
$scriptDir = (Split-Path -Parent $MyInvocation.MyCommand.Path) -replace '\\', '/'
$genDir    = "$scriptDir/.generated"
New-Item -ItemType Directory -Force -Path ($genDir -replace '/', '\') | Out-Null

# Substitute placeholders in config templates
$hosts = @{
    '${BFF_IP}'          = 'host.docker.internal'
    '${USER_IP}'         = 'host.docker.internal'
    '${STORE_IP}'        = 'host.docker.internal'
    '${ORDER_IP}'        = 'host.docker.internal'
    '${NOTIFICATION_IP}' = 'host.docker.internal'
}

$prometheusConfig = Get-Content "$scriptDir/prometheus.yml" -Raw
foreach ($k in $hosts.Keys) { $prometheusConfig = $prometheusConfig.Replace($k, $hosts[$k]) }
$genDirWin = $genDir -replace '/', '\'
$utf8NoBom = New-Object System.Text.Encoding.UTF8Encoding $false
[System.IO.File]::WriteAllText("$genDirWin\prometheus.yml",   $prometheusConfig,   $utf8NoBom)

$alertmanagerConfig = Get-Content "$scriptDir/alertmanager.yml" -Raw
$alertmanagerConfig = $alertmanagerConfig.Replace('${TELEGRAM_BOT_TOKEN}', $token).Replace('${TELEGRAM_CHAT_ID}', $chatId)
[System.IO.File]::WriteAllText("$genDirWin\alertmanager.yml", $alertmanagerConfig, $utf8NoBom)

# Create shared network
docker network create monitoring 2>$null

# Prometheus
docker run -d --name prometheus --network monitoring `
    -p 9090:9090 `
    -v "${genDir}/prometheus.yml:/etc/prometheus/prometheus.yml:ro" `
    -v "${scriptDir}/alerting-rules.yml:/etc/prometheus/alerting-rules.yml:ro" `
    prom/prometheus:v2.53.4 `
    --config.file=/etc/prometheus/prometheus.yml `
    --storage.tsdb.path=/prometheus `
    --web.console.libraries=/usr/share/prometheus/console_libraries `
    --web.console.templates=/usr/share/prometheus/consoles `
    --web.enable-remote-write-receiver

# Alertmanager
docker run -d --name alertmanager --network monitoring `
    -p 9093:9093 `
    -v "${genDir}/alertmanager.yml:/etc/alertmanager/alertmanager.yml:ro" `
    prom/alertmanager:v0.27.0

# Grafana
docker run -d --name grafana --network monitoring `
    -p 3000:3000 `
    -v "${scriptDir}/grafana/provisioning:/etc/grafana/provisioning:ro" `
    -v "${scriptDir}/grafana/dashboards:/var/lib/grafana/dashboards:ro" `
    grafana/grafana:11.4.0

Write-Host "Prometheus   -> http://localhost:9090"
Write-Host "Alertmanager -> http://localhost:9093"
Write-Host "Grafana      -> http://localhost:3000  (admin / admin)"
