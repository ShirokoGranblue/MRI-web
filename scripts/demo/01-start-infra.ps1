$ErrorActionPreference = "Stop"
docker compose up -d mysql redis nacos minio
if ($LASTEXITCODE -ne 0) {
  throw "MRI infrastructure startup failed."
}
Write-Host "Waiting for Nacos, Redis, MySQL and MinIO..."
Start-Sleep -Seconds 20

$migrations = @(
  "02-patient-account-migration.sql",
  "03-mri-enhancement-migration.sql"
)
foreach ($migrationName in $migrations) {
  $migration = Join-Path $PSScriptRoot "..\..\docker\mysql\init\$migrationName"
  Write-Host "Applying idempotent migration: $migrationName"
  Get-Content -LiteralPath $migration -Raw | docker exec -i mri-mysql mysql -uroot -proot123456 mri_cloud
  if ($LASTEXITCODE -ne 0) {
    throw "MRI database migration failed: $migrationName"
  }
}

docker compose ps
if ($LASTEXITCODE -ne 0) {
  throw "MRI infrastructure status check failed."
}
