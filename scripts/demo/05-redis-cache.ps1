$ErrorActionPreference = "Stop"
$token = (Get-Content -Raw "target-demo-output-token.txt").Trim()
$headers = @{ Authorization = "Bearer $token" }
$base = "http://localhost:8080"

# 空库时自动创建一条影像检查，用于缓存演示（不再依赖预置样例数据）
$list = Invoke-RestMethod -Headers $headers -Uri "$base/api/images/studies?page=1&size=1"
$studyId = $list.data.records[0].id
if (-not $studyId) {
  Write-Host "No study found, bootstrapping demo study..."
  $patient = Invoke-RestMethod -Headers $headers -Method Post -Uri "$base/api/patients" -ContentType "application/json" -Body (@{ patientNo = "DEMO-CACHE-$(Get-Random)"; name = "缓存演示"; gender = "男"; birthDate = "1990-01-01"; phone = "13800000000" } | ConvertTo-Json)
  $patientId = $patient.data.id
  $exam = Invoke-RestMethod -Headers $headers -Method Post -Uri "$base/api/exams" -ContentType "application/json" -Body (@{ patientId = $patientId; examItem = "头颅MRI平扫"; clinicalDiagnosis = "缓存演示"; priority = "普通" } | ConvertTo-Json)
  $examId = $exam.data.id
  Invoke-RestMethod -Headers $headers -Method Post -Uri "$base/api/exams/$examId/start" | Out-Null
  Invoke-RestMethod -Headers $headers -Method Post -Uri "$base/api/exams/$examId/complete" | Out-Null
  $study = Invoke-RestMethod -Headers $headers -Method Post -Uri "$base/api/images/studies" -ContentType "application/json" -Body (@{ examOrderId = $examId; description = "缓存演示影像" } | ConvertTo-Json)
  $studyId = $study.data.id
}

Write-Host "First request reads Study $studyId and fills cache"
Invoke-RestMethod -Headers $headers -Uri "$base/api/images/studies/$studyId" | ConvertTo-Json -Depth 8
Write-Host "Second request should hit service-side cache"
Invoke-RestMethod -Headers $headers -Uri "$base/api/images/studies/$studyId/cache-demo" | ConvertTo-Json -Depth 8
Write-Host "Redis keys:"
docker exec mri-redis redis-cli keys "mri:*"
