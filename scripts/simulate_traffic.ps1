# CloudKicks Sneaker Drop Traffic Surge Simulator (PowerShell)
param(
    [int]$Requests = 50,
    [string]$BaseUrl = "http://localhost:8080"
)

Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host "  CloudKicks: Sneaker Drop Traffic Surge ($Requests Orders)" -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan

$shoeIds = @("SHOE-001", "SHOE-002", "SHOE-003", "SHOE-004", "SHOE-005")
$sizes = @("US 8", "US 9", "US 9.5", "US 10", "US 10.5", "US 11", "US 12")
$successCount = 0
$failCount = 0
$latencies = @()

$sw = [System.Diagnostics.Stopwatch]::StartNew()

for ($i = 1; $i -le $Requests; $i++) {
    $reqSw = [System.Diagnostics.Stopwatch]::StartNew()
    $selectedShoe = $shoeIds[(Get-Random -Minimum 0 -Maximum $shoeIds.Count)]
    $selectedSize = $sizes[(Get-Random -Minimum 0 -Maximum $sizes.Count)]
    
    $body = @{
        customerName = "SneakerBot_$i"
        customerEmail = "buyer$i@snkrs-rush.io"
        items = @(@{ productId = $selectedShoe; size = $selectedSize; quantity = 1 })
    } | ConvertTo-Json -Compress

    try {
        $response = Invoke-RestMethod -Uri "$BaseUrl/api/orders" -Method Post -Body $body -ContentType "application/json" -TimeoutSec 5
        $reqSw.Stop()
        $lat = [math]::Round($reqSw.Elapsed.TotalMilliseconds, 1)
        $latencies += $lat
        $successCount++
        Write-Host "[$i/$Requests] Sneaker Order: $($response.orderId) | Latency: $($lat)ms" -ForegroundColor Green
    }
    catch {
        $reqSw.Stop()
        $failCount++
        Write-Host "[$i/$Requests] Checkout Failed: $($_.Exception.Message)" -ForegroundColor Red
    }
    Start-Sleep -Milliseconds (Get-Random -Minimum 20 -Maximum 80)
}

$sw.Stop()
$totalSec = [math]::Round($sw.Elapsed.TotalSeconds, 2)
$rps = [math]::Round($Requests / $totalSec, 2)
$avgLat = if ($latencies.Count -gt 0) { [math]::Round(($latencies | Measure-Object -Average).Average, 1) } else { 0 }

Write-Host ""
Write-Host "------------- SNEAKER DROP REPORT -------------" -ForegroundColor Yellow
Write-Host "Total Drop Requests      : $Requests"
Write-Host "Successful Sneaker Buys  : $successCount"
Write-Host "Failed Transactions      : $failCount"
Write-Host "Total Elapsed Time       : $totalSec s"
Write-Host "Throughput Rate          : $rps Req/Sec"
Write-Host "Average Checkout Latency : $avgLat ms"
Write-Host "-----------------------------------------------" -ForegroundColor Yellow
