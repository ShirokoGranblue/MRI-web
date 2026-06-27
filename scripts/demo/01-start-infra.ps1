$ErrorActionPreference = "Stop"
docker compose up -d mysql redis nacos minio
Write-Host "Waiting for Nacos, Redis, MySQL and MinIO..."
Start-Sleep -Seconds 20

$migration = Join-Path $PSScriptRoot "..\..\docker\mysql\init\02-patient-account-migration.sql"
Write-Host "Applying idempotent patient account migration..."
Get-Content -LiteralPath $migration -Raw | docker exec -i mri-mysql mysql -uroot -proot123456 mri_cloud

docker compose ps
