$ErrorActionPreference = "Stop"
$token = (Get-Content -Raw "target-demo-output-token.txt").Trim()
$base = "http://localhost:8080"
Write-Host "No token should be rejected:"
try {
  Invoke-RestMethod -Uri "$base/api/patients?page=1&size=10"
} catch {
  Write-Host "Rejected as expected: $($_.Exception.Message)"
}
Write-Host "Bearer token through gateway:"
Invoke-RestMethod -Headers @{ Authorization = "Bearer $token" } -Uri "$base/api/patients?page=1&size=10" | ConvertTo-Json -Depth 8
