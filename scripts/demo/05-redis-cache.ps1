$ErrorActionPreference = "Stop"
$token = (Get-Content -Raw "target-demo-output-token.txt").Trim()
$headers = @{ Authorization = "Bearer $token" }
Write-Host "First request reads Study and fills cache"
Invoke-RestMethod -Headers $headers -Uri "http://localhost:8080/api/images/studies/1" | ConvertTo-Json -Depth 8
Write-Host "Second request should hit service-side cache"
Invoke-RestMethod -Headers $headers -Uri "http://localhost:8080/api/images/studies/1/cache-demo" | ConvertTo-Json -Depth 8
Write-Host "Redis keys:"
docker exec mri-redis redis-cli keys "mri:*"
