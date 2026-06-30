param()

$ErrorActionPreference = "Stop"

function Get-ProjectRoot {
    return (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
}

function Get-PidFile {
    return (Join-Path ([System.IO.Path]::GetTempPath()) "persona-flow-commerce-dev-pids.json")
}

function Start-DevWindow {
    param(
        [string]$Title,
        [string]$WorkingDirectory,
        [string]$Command
    )

    $script = @"
`$Host.UI.RawUI.WindowTitle = "$Title"
Set-Location -LiteralPath "$WorkingDirectory"
$Command
"@

    $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($script))
    return Start-Process -FilePath "powershell.exe" `
        -ArgumentList @("-NoExit", "-ExecutionPolicy", "Bypass", "-EncodedCommand", $encoded) `
        -PassThru
}

$root = Get-ProjectRoot
$pidFile = Get-PidFile

if (-not (Test-Path (Join-Path $root "docker-compose.yml"))) {
    throw "Cannot find docker-compose.yml. Please run this script from inside the PersonaFlow Commerce repository."
}

$agentDir = Join-Path $root "persona-agent-service"
$serverDir = Join-Path $root "persona-commerce-server"
$webDir = Join-Path $root "persona-commerce-web"

Write-Host "PersonaFlow Commerce dev startup" -ForegroundColor Cyan
Write-Host "Project root: $root"

Push-Location $root
try {
    Write-Host "`nStarting Docker dependencies..." -ForegroundColor Cyan
    docker compose up -d
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose up -d failed."
    }
}
finally {
    Pop-Location
}

$processes = @()

Write-Host "`nOpening Python Agent window..." -ForegroundColor Cyan
$pythonProcess = Start-DevWindow `
    -Title "PersonaFlow Python Agent" `
    -WorkingDirectory $agentDir `
    -Command '$env:PYTHONPATH = "src"; python -m uvicorn persona_agent_service.main:app --host 127.0.0.1 --port 8001'
$processes += [pscustomobject]@{
    name = "python-agent"
    pid = $pythonProcess.Id
    port = 8001
}

Write-Host "Opening Java backend window..." -ForegroundColor Cyan
$javaProcess = Start-DevWindow `
    -Title "PersonaFlow Java Backend" `
    -WorkingDirectory $serverDir `
    -Command '.\mvnw.cmd spring-boot:run'
$processes += [pscustomobject]@{
    name = "java-backend"
    pid = $javaProcess.Id
    port = 8080
}

Write-Host "Opening Vue frontend window..." -ForegroundColor Cyan
$vueProcess = Start-DevWindow `
    -Title "PersonaFlow Vue Frontend" `
    -WorkingDirectory $webDir `
    -Command 'npm.cmd run dev -- --host 127.0.0.1 --port 5173'
$processes += [pscustomobject]@{
    name = "vue-frontend"
    pid = $vueProcess.Id
    port = 5173
}

$state = [pscustomobject]@{
    startedAt = (Get-Date).ToString("o")
    root = $root
    processes = $processes
}

$state | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $pidFile -Encoding UTF8

Write-Host "`nStartup commands have been launched." -ForegroundColor Green
Write-Host "Use scripts\check-dev.ps1 after a short warm-up to verify health."
Write-Host "Dev PID file: $pidFile"
Write-Host ""
Write-Host "Vue:           http://127.0.0.1:5173/"
Write-Host "Java health:   http://127.0.0.1:8080/actuator/health"
Write-Host "Python health: http://127.0.0.1:8001/health"
Write-Host "RabbitMQ UI:   http://127.0.0.1:15672"
Write-Host ""
Write-Host "Vue is started with --host 127.0.0.1 --port 5173." -ForegroundColor Yellow
Write-Host "If port 5173 is already in use, Vite may switch to another port. Use the Local URL printed in the Vue terminal as the source of truth." -ForegroundColor Yellow
