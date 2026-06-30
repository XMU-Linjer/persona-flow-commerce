param()

$ErrorActionPreference = "Continue"

function Get-ProjectRoot {
    return (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
}

function Test-HttpEndpoint {
    param(
        [string]$Name,
        [string]$Url
    )

    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
        if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 400) {
            Write-Host ("{0,-18} UP    {1}" -f $Name, $Url) -ForegroundColor Green
            return
        }

        Write-Host ("{0,-18} DOWN  HTTP {1} {2}" -f $Name, $response.StatusCode, $Url) -ForegroundColor Red
    }
    catch {
        Write-Host ("{0,-18} DOWN  {1}" -f $Name, $Url) -ForegroundColor Red
        Write-Host ("  " + $_.Exception.Message) -ForegroundColor DarkGray
    }
}

function Test-FirstAvailableEndpoint {
    param(
        [string]$Name,
        [string[]]$Urls,
        [string]$DownHint
    )

    foreach ($url in $Urls) {
        try {
            $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 400) {
                Write-Host ("{0,-18} UP    {1}" -f $Name, $url) -ForegroundColor Green
                return
            }
        }
        catch {
            # Try the next possible Vite URL.
        }
    }

    Write-Host ("{0,-18} DOWN" -f $Name) -ForegroundColor Red
    Write-Host ("  " + $DownHint) -ForegroundColor DarkGray
}

$root = Get-ProjectRoot

Write-Host "PersonaFlow Commerce dev health check" -ForegroundColor Cyan
Write-Host "Project root: $root"

Push-Location $root
try {
    Write-Host "`nDocker compose services:" -ForegroundColor Cyan
    docker compose ps
    if ($LASTEXITCODE -eq 0) {
        Write-Host "docker compose    UP    command succeeded" -ForegroundColor Green
    }
    else {
        Write-Host "docker compose    DOWN  command failed" -ForegroundColor Red
    }
}
finally {
    Pop-Location
}

Write-Host "`nHTTP endpoints:" -ForegroundColor Cyan
Test-HttpEndpoint -Name "Python Agent" -Url "http://127.0.0.1:8001/health"
Test-HttpEndpoint -Name "Java Backend" -Url "http://127.0.0.1:8080/actuator/health"
Test-FirstAvailableEndpoint -Name "Vue Frontend" -Urls @(
    "http://127.0.0.1:5173/",
    "http://localhost:5173/",
    "http://[::1]:5173/",
    "http://127.0.0.1:5174/",
    "http://localhost:5174/",
    "http://[::1]:5174/",
    "http://127.0.0.1:5175/",
    "http://localhost:5175/",
    "http://[::1]:5175/",
    "http://127.0.0.1:5176/",
    "http://localhost:5176/",
    "http://[::1]:5176/"
) -DownHint "Could not reach Vite on 5173-5176 via 127.0.0.1, localhost, or [::1]. Check the Local URL printed in the Vue terminal."

Write-Host "`nIf Vue is DOWN but its terminal shows another port, open that Vite Local URL instead." -ForegroundColor Yellow
