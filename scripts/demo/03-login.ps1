$ErrorActionPreference = "Stop"
$body = @{ username = "admin"; password = "admin123" } | ConvertTo-Json
$response = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/auth/login" -ContentType "application/json" -Body $body
$token = $response.data.token
Set-Content -LiteralPath "target-demo-output-token.txt" -Value $token -NoNewline
Write-Host "Login OK. Token saved to target-demo-output-token.txt"
$response | ConvertTo-Json -Depth 8
