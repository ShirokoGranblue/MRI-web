param(
  [Parameter(Mandatory = $true)]
  [string]$RemoteUrl
)
$ErrorActionPreference = "Stop"
if (-not (Test-Path ".git")) {
  git init
}
git status --short
git add .
git commit -m "feat: implement MRI image management microservices demo"
if (-not (git remote | Select-String -SimpleMatch "origin")) {
  git remote add origin $RemoteUrl
} else {
  git remote set-url origin $RemoteUrl
}
git branch -M main
git push -u origin main
