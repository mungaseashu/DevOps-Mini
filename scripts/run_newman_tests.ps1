# Run Postman API Tests via Newman
param(
    [string]$TargetUrl = "http://localhost:8080"
)

Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host "  Executing Postman Automated API Tests with Newman" -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan

$collectionPath = "$PSScriptRoot/../postman/retail-api-collection.json"
$envPath = "$PSScriptRoot/../postman/retail-environment.json"

if (Get-Command newman -ErrorAction SilentlyContinue) {
    Write-Host "[*] Running with local Newman CLI..." -ForegroundColor Green
    newman run $collectionPath -e $envPath --env-var "baseUrl=$TargetUrl" --reporters cli
}
elseif (Get-Command docker -ErrorAction SilentlyContinue) {
    Write-Host "[*] Running with Postman Newman Docker Container..." -ForegroundColor Yellow
    $colDir = (Resolve-Path "$PSScriptRoot/../postman").Path
    docker run --rm --network="host" -v "${colDir}:/etc/newman" postman/newman run /etc/newman/retail-api-collection.json -e /etc/newman/retail-environment.json --env-var "baseUrl=$TargetUrl"
}
else {
    Write-Host "[!] Neither Newman nor Docker found. Please install newman (`npm install -g newman`) or Docker." -ForegroundColor Red
}
