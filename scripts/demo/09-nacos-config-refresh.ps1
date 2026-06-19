$ErrorActionPreference = "Stop"
$content = "mri:`n  image:`n    watermark: 医院MRI影像系统-课堂实时配置`n    download-enabled: false`n  cache:`n    study-ttl-seconds: 120"
Invoke-RestMethod -Method Post -Uri "http://localhost:8848/nacos/v1/cs/configs" -Body @{
  dataId = "mri-image-service.yaml"
  group = "DEFAULT_GROUP"
  content = $content
  type = "yaml"
}
Write-Host "Config updated. Wait 5 seconds and query demo config."
Start-Sleep -Seconds 5
$token = (Get-Content -Raw "target-demo-output-token.txt").Trim()
Invoke-RestMethod -Headers @{ Authorization = "Bearer $token" } -Uri "http://localhost:8080/api/images/demo/config" | ConvertTo-Json -Depth 8
