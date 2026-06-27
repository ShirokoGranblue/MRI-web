param(
  [int]$GatewayPort = 8080
)

$ErrorActionPreference = "Stop"
$root = (Get-Location).Path
$outputDir = Join-Path $root "target-demo-output"
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

Write-Host "Packaging modules before starting services..."
mvn -DskipTests install
if ($LASTEXITCODE -ne 0) {
  throw "Maven package failed. Fix build errors before starting services."
}

$modules = @(
  @{ Name = "mri-auth-service"; Arguments = @() },
  @{ Name = "mri-patient-service"; Arguments = @() },
  @{ Name = "mri-exam-service"; Arguments = @() },
  @{ Name = "mri-image-service"; Arguments = @() },
  @{ Name = "mri-report-service"; Arguments = @() },
  @{ Name = "mri-gateway"; Arguments = @("-Dspring-boot.run.arguments=--server.port=$GatewayPort") }
)
foreach ($module in $modules) {
  $stdout = Join-Path $outputDir "$($module.Name).out.log"
  $stderr = Join-Path $outputDir "$($module.Name).err.log"
  $argumentList = @("-pl", $module.Name, "spring-boot:run") + $module.Arguments
  $process = Start-Process -FilePath "mvn.cmd" `
    -ArgumentList $argumentList `
    -WorkingDirectory $root `
    -WindowStyle Hidden `
    -RedirectStandardOutput $stdout `
    -RedirectStandardError $stderr `
    -PassThru
  Set-Content -LiteralPath (Join-Path $outputDir "$($module.Name).pid") -Value $process.Id -NoNewline
  Write-Host "Started $($module.Name), pid=$($process.Id), logs=$stdout / $stderr"
  Start-Sleep -Seconds 8
}
Write-Host "Services are starting. Gateway: http://localhost:$GatewayPort/api/**"
Write-Host "Check Nacos at http://localhost:8848/nacos"
