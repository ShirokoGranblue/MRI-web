$ErrorActionPreference = "Stop"
$root = (Get-Location).Path
$outputDir = Join-Path $root "target-demo-output"
$pidFile = Join-Path $outputDir "mri-image-service.pid"

function Show-NacosServices($title) {
  Write-Host $title
  Invoke-RestMethod "http://localhost:8848/nacos/v1/ns/catalog/services?hasIpCount=true&pageNo=1&pageSize=20" | ConvertTo-Json -Depth 8
}

Show-NacosServices "Registered services before stopping image service:"

if (-not (Test-Path $pidFile)) {
  throw "PID file not found: $pidFile. Run scripts/demo/02-start-services.ps1 first."
}

$imagePid = [int](Get-Content -Raw $pidFile)
if (Get-Process -Id $imagePid -ErrorAction SilentlyContinue) {
  Stop-Process -Id $imagePid
  Write-Host "Stopped mri-image-service, pid=$imagePid. Waiting for Nacos deregistration..."
  Start-Sleep -Seconds 12
  Show-NacosServices "Registered services after stopping image service:"
} else {
  Write-Host "mri-image-service process $imagePid is not running. Showing current service list."
  Show-NacosServices "Current registered services:"
}

$stdout = Join-Path $outputDir "mri-image-service.restart.out.log"
$stderr = Join-Path $outputDir "mri-image-service.restart.err.log"
$process = Start-Process -FilePath "mvn.cmd" `
  -ArgumentList @("-pl", "mri-image-service", "spring-boot:run") `
  -WorkingDirectory $root `
  -WindowStyle Hidden `
  -RedirectStandardOutput $stdout `
  -RedirectStandardError $stderr `
  -PassThru
Set-Content -LiteralPath $pidFile -Value $process.Id -NoNewline
Write-Host "Restarted mri-image-service, pid=$($process.Id). Waiting for Nacos registration..."
Start-Sleep -Seconds 12
Show-NacosServices "Registered services after restarting image service:"
