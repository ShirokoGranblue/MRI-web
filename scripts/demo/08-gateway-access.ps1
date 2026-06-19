$ErrorActionPreference = "Stop"
$token = (Get-Content -Raw "target-demo-output-token.txt").Trim()
Write-Host "No token should be rejected:"
try {
  Invoke-RestMethod -Uri "http://localhost:8080/api/patients/1"
} catch {
  Write-Host "Rejected as expected: $($_.Exception.Message)"
}
Write-Host "Bearer token through gateway:"
Invoke-RestMethod -Headers @{ Authorization = "Bearer $token" } -Uri "http://localhost:8080/api/patients/1" | ConvertTo-Json -Depth 8
