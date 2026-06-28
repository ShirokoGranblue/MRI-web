$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$sqlPath = Join-Path $PSScriptRoot "clear-business-data.sql"
$storagePath = Join-Path $repoRoot "storage\mri-images"

docker info *> $null
if ($LASTEXITCODE -ne 0) {
    throw "Docker engine is unavailable. No runtime data was cleared. Start Docker Desktop first."
}

Write-Host "Clearing MRI database business data and patient accounts..."
Get-Content -LiteralPath $sqlPath -Raw | docker exec -i mri-mysql mysql -uroot -proot123456 mri_cloud
if ($LASTEXITCODE -ne 0) {
    throw "MRI database cleanup failed."
}

Write-Host "Clearing MRI Redis database..."
docker exec mri-redis redis-cli FLUSHDB
if ($LASTEXITCODE -ne 0) {
    throw "MRI Redis cleanup failed."
}

Write-Host "Clearing MRI MinIO objects while preserving the bucket..."
docker run --rm --network container:mri-minio --entrypoint /bin/sh minio/mc -c "mc alias set local http://127.0.0.1:9000 mri mri123456; mc rm --recursive --force local/mri-images; mc mb --ignore-existing local/mri-images; mc ls local/mri-images"
if ($LASTEXITCODE -ne 0) {
    throw "MRI MinIO cleanup failed."
}

if (Test-Path -LiteralPath $storagePath) {
    $resolvedStorage = (Resolve-Path -LiteralPath $storagePath).Path
    if (-not $resolvedStorage.StartsWith($repoRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to clear storage outside the repository: $resolvedStorage"
    }
    Get-ChildItem -LiteralPath $resolvedStorage -File -Force |
        Where-Object { $_.Name -ne ".gitkeep" } |
        Remove-Item -Force
}

Write-Host "Runtime data cleared. Run the zero-data verification queries before delivery."
