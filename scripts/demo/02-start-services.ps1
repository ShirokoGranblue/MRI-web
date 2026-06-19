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
  "mri-auth-service",
  "mri-patient-service",
  "mri-exam-service",
  "mri-image-service",
  "mri-report-service",
  "mri-gateway"
)
foreach ($module in $modules) {
  $stdout = Join-Path $outputDir "$module.out.log"
  $stderr = Join-Path $outputDir "$module.err.log"
  $process = Start-Process -FilePath "mvn.cmd" `
    -ArgumentList @("-pl", $module, "spring-boot:run") `
    -WorkingDirectory $root `
    -WindowStyle Hidden `
    -RedirectStandardOutput $stdout `
    -RedirectStandardError $stderr `
    -PassThru
  Set-Content -LiteralPath (Join-Path $outputDir "$module.pid") -Value $process.Id -NoNewline
  Write-Host "Started $module, pid=$($process.Id), logs=$stdout / $stderr"
  Start-Sleep -Seconds 8
}
Write-Host "Services are starting. Check Nacos at http://localhost:8848/nacos"
