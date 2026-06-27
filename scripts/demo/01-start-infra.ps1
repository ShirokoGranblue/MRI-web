$ErrorActionPreference = "Stop"
docker compose up -d mysql redis nacos minio
Write-Host "Waiting for Nacos, Redis, MySQL and MinIO..."
Start-Sleep -Seconds 20
docker compose ps
