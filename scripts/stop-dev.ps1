param()

$ErrorActionPreference = "Continue"

function Get-ProjectRoot {
    return (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
}

function Get-PidFile {
    return (Join-Path ([System.IO.Path]::GetTempPath()) "persona-flow-commerce-dev-pids.json")
}

function Stop-RecordedProcesses {
    param([string]$PidFile)

    if (-not (Test-Path -LiteralPath $PidFile)) {
        Write-Host "No PID file found. Falling back to port-based stop." -ForegroundColor Yellow
        return $false
    }

    try {
        $state = Get-Content -Raw -LiteralPath $PidFile | ConvertFrom-Json
        foreach ($entry in $state.processes) {
            $process = Get-Process -Id $entry.pid -ErrorAction SilentlyContinue
            if ($null -eq $process) {
                Write-Host ("{0,-18} already stopped (PID {1})" -f $entry.name, $entry.pid) -ForegroundColor DarkGray
                continue
            }

            Write-Host ("Stopping {0,-18} PID {1}" -f $entry.name, $entry.pid) -ForegroundColor Cyan
            Stop-Process -Id $entry.pid -Force -ErrorAction SilentlyContinue
        }

        Remove-Item -LiteralPath $PidFile -Force -ErrorAction SilentlyContinue
        return $true
    }
    catch {
        Write-Host "Failed to read PID file. Falling back to port-based stop." -ForegroundColor Yellow
        Write-Host ("  " + $_.Exception.Message) -ForegroundColor DarkGray
        return $false
    }
}

function Stop-ProcessByPort {
    param([int[]]$Ports)

    foreach ($port in $Ports) {
        $connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
        if (-not $connections) {
            Write-Host ("Port {0,-5} no listening process found" -f $port) -ForegroundColor DarkGray
            continue
        }

        foreach ($connection in ($connections | Sort-Object OwningProcess -Unique)) {
            $process = Get-Process -Id $connection.OwningProcess -ErrorAction SilentlyContinue
            if ($null -eq $process) {
                continue
            }

            Write-Host ("Port {0,-5} stopping PID {1} ({2})" -f $port, $process.Id, $process.ProcessName) -ForegroundColor Yellow
            Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
        }
    }
}

$root = Get-ProjectRoot
$pidFile = Get-PidFile
$legacyPidFile = Join-Path $PSScriptRoot ".dev-pids.json"

if ((-not (Test-Path -LiteralPath $pidFile)) -and (Test-Path -LiteralPath $legacyPidFile)) {
    Write-Host "Using legacy PID file from scripts directory and removing it after stop." -ForegroundColor Yellow
    $pidFile = $legacyPidFile
}

Write-Host "PersonaFlow Commerce dev stop" -ForegroundColor Cyan
Write-Host "Project root: $root"

$usedPidFile = Stop-RecordedProcesses -PidFile $pidFile

if (-not $usedPidFile) {
    Write-Host "`nChecking common dev ports and stopping remaining listeners if any..." -ForegroundColor Yellow
    Write-Host "Ports checked: 5173, 5174, 5175, 5176, 8080, 8001. This is a fallback; review the printed process names if something unexpected appears." -ForegroundColor Yellow
    Stop-ProcessByPort -Ports @(5173, 5174, 5175, 5176, 8080, 8001)
}
else {
    Write-Host "`nStopped recorded dev windows. Port-based fallback was skipped to avoid touching unrelated local processes." -ForegroundColor Green
}

Push-Location $root
try {
    Write-Host "`nStopping Docker compose services with 'docker compose stop'..." -ForegroundColor Cyan
    docker compose stop
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Docker services stopped. Volumes were not removed." -ForegroundColor Green
    }
    else {
        Write-Host "docker compose stop failed. Volumes were not removed." -ForegroundColor Red
    }
}
finally {
    Pop-Location
}

Write-Host "`nDone. This script never runs 'docker compose down -v'." -ForegroundColor Green
