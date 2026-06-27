$ErrorActionPreference = "Stop"
$token = (Get-Content -Raw "target-demo-output-token.txt").Trim()
$headers = @{ Authorization = "Bearer $token" }
$base = "http://localhost:8080"

# 空库时先建档一名演示患者，供检查申请的远程患者校验使用（不再依赖预置样例数据）
$patientBody = @{ patientNo = "DEMO-FEIGN-$(Get-Random)"; name = "远程调用演示"; gender = "男"; birthDate = "1990-01-01"; phone = "13800000000" } | ConvertTo-Json
$patient = Invoke-RestMethod -Method Post -Headers $headers -Uri "$base/api/patients" -ContentType "application/json" -Body $patientBody
$patientId = $patient.data.id
Write-Host "Patient created: $patientId (exam -> patient remote validation)"
$examBody = @{ patientId = $patientId; examItem = "头颅MRI增强"; clinicalDiagnosis = "头痛待查"; priority = "普通" } | ConvertTo-Json
$exam = Invoke-RestMethod -Method Post -Headers $headers -Uri "$base/api/exams" -ContentType "application/json" -Body $examBody
$examId = $exam.data.id
Write-Host "Exam created through exam -> patient remote call: $examId"
Invoke-RestMethod -Method Post -Headers $headers -Uri "$base/api/exams/$examId/start" | Out-Null
Invoke-RestMethod -Method Post -Headers $headers -Uri "$base/api/exams/$examId/complete" | Out-Null
Write-Host "Exam status moved to COMPLETED before report publishing"
$studyBody = @{ examOrderId = $examId; studyInstanceUid = "1.2.156.112605.$(Get-Random)"; description = "头颅MRI增强" } | ConvertTo-Json
$study = Invoke-RestMethod -Method Post -Headers $headers -Uri "$base/api/images/studies" -ContentType "application/json" -Body $studyBody
$studyId = $study.data.id
Write-Host "Study archived through image -> exam remote call: $studyId"
$reportBody = @{ examOrderId = $examId; studyId = $studyId; findings = "未见明确异常强化。"; impression = "建议结合临床。" } | ConvertTo-Json
$report = Invoke-RestMethod -Method Post -Headers $headers -Uri "$base/api/reports" -ContentType "application/json" -Body $reportBody
$reportId = $report.data.id
Invoke-RestMethod -Method Post -Headers $headers -Uri "$base/api/reports/$reportId/submit" | Out-Null
Invoke-RestMethod -Method Post -Headers $headers -Uri "$base/api/reports/$reportId/approve" | Out-Null
Invoke-RestMethod -Method Post -Headers $headers -Uri "$base/api/reports/$reportId/publish" | ConvertTo-Json -Depth 8
