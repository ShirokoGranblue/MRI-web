$ErrorActionPreference = "Stop"
$token = (Get-Content -Raw "target-demo-output-token.txt").Trim()
$headers = @{ Authorization = "Bearer $token" }
$examBody = @{ patientId = 1; examItem = "头颅MRI增强"; clinicalDiagnosis = "头痛待查"; priority = "普通" } | ConvertTo-Json
$exam = Invoke-RestMethod -Method Post -Headers $headers -Uri "http://localhost:8080/api/exams" -ContentType "application/json" -Body $examBody
$examId = $exam.data.id
Write-Host "Exam created through exam -> patient remote call: $examId"
$studyBody = @{ examOrderId = $examId; studyInstanceUid = "1.2.156.112605.$(Get-Random)"; description = "头颅MRI增强" } | ConvertTo-Json
$study = Invoke-RestMethod -Method Post -Headers $headers -Uri "http://localhost:8080/api/images/studies" -ContentType "application/json" -Body $studyBody
$studyId = $study.data.id
Write-Host "Study archived through image -> exam remote call: $studyId"
$reportBody = @{ examOrderId = $examId; studyId = $studyId; findings = "未见明确异常强化。"; impression = "建议结合临床。" } | ConvertTo-Json
$report = Invoke-RestMethod -Method Post -Headers $headers -Uri "http://localhost:8080/api/reports" -ContentType "application/json" -Body $reportBody
Invoke-RestMethod -Method Post -Headers $headers -Uri "http://localhost:8080/api/reports/$($report.data.id)/publish" | ConvertTo-Json -Depth 8
