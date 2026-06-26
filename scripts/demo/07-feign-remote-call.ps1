$ErrorActionPreference = "Stop"
$token = (Get-Content -Raw "target-demo-output-token.txt").Trim()
$headers = @{ Authorization = "Bearer $token" }
$examBody = @{ patientId = 1; examItem = "头颅MRI增强"; clinicalDiagnosis = "头痛待查"; priority = "普通" } | ConvertTo-Json
$exam = Invoke-RestMethod -Method Post -Headers $headers -Uri "http://localhost:8080/api/exams" -ContentType "application/json" -Body $examBody
$examId = $exam.data.id
Write-Host "Exam created through exam -> patient remote call: $examId"
Invoke-RestMethod -Method Post -Headers $headers -Uri "http://localhost:8080/api/exams/$examId/start" | Out-Null
Invoke-RestMethod -Method Post -Headers $headers -Uri "http://localhost:8080/api/exams/$examId/complete" | Out-Null
Write-Host "Exam status moved to COMPLETED before report publishing"
$studyBody = @{ examOrderId = $examId; studyInstanceUid = "1.2.156.112605.$(Get-Random)"; description = "头颅MRI增强" } | ConvertTo-Json
$study = Invoke-RestMethod -Method Post -Headers $headers -Uri "http://localhost:8080/api/images/studies" -ContentType "application/json" -Body $studyBody
$studyId = $study.data.id
Write-Host "Study archived through image -> exam remote call: $studyId"
$reportBody = @{ examOrderId = $examId; studyId = $studyId; findings = "未见明确异常强化。"; impression = "建议结合临床。" } | ConvertTo-Json
$report = Invoke-RestMethod -Method Post -Headers $headers -Uri "http://localhost:8080/api/reports" -ContentType "application/json" -Body $reportBody
$reportId = $report.data.id
Invoke-RestMethod -Method Post -Headers $headers -Uri "http://localhost:8080/api/reports/$reportId/submit" | Out-Null
Invoke-RestMethod -Method Post -Headers $headers -Uri "http://localhost:8080/api/reports/$reportId/approve" | Out-Null
Invoke-RestMethod -Method Post -Headers $headers -Uri "http://localhost:8080/api/reports/$reportId/publish" | ConvertTo-Json -Depth 8
