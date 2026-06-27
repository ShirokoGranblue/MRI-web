$ErrorActionPreference = "Stop"
$docs = @(
  "http://localhost:9001/v3/api-docs",
  "http://localhost:9002/v3/api-docs",
  "http://localhost:9003/v3/api-docs",
  "http://localhost:9004/v3/api-docs",
  "http://localhost:9005/v3/api-docs"
)
$total = 0
foreach ($url in $docs) {
  $json = Invoke-RestMethod -Uri $url
  $count = 0
  foreach ($path in $json.paths.PSObject.Properties) {
    $count += @($path.Value.PSObject.Properties).Count
  }
  Write-Host "$url -> $count APIs"
  $total += $count
}
Write-Host "Total APIs: $total"
if ($total -lt 50) { throw "API count is below 50" }
