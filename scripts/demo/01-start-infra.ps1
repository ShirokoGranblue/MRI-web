$ErrorActionPreference = "Stop"
docker compose up -d mysql redis nacos
Write-Host "Waiting for Nacos, Redis and MySQL..."
Start-Sleep -Seconds 20
docker compose ps
