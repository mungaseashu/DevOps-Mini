# Native Windows PowerShell Traffic Simulator
param(
    [int]$Requests = 50,
    [string]$BaseUrl = "http://localhost:8080"
)

Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host "  Online Retail Traffic Surge Simulation ($Requests Requests)" -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan

$products = @("PROD-001", "PROD-002", "PROD-003", "PROD-004", "PROD-005")
$successCount = 0
$failCount = 0
$latencies = @()

$sw = [System.Diagnostics.Stopwatch]::StartNew()

for ($i = 1; $i -le $Requests; $i++) {
    $reqSw = [System.Diagnostics.Stopwatch]::StartNew()
    $selectedProductId = $products[(Get-Random -Minimum 0 -Maximum $products.Count)]
    
    $body = @{
        customerName = "PS_Shopper_$i"
        customerEmail = "shopper$i@surge.io"
        items = @(@{ productId = $selectedProductId; quantity = (Get-Random -Minimum 1 -Maximum 3) })
    } | ConvertTo-Json -Compress

    try {
        $response = Invoke-RestMethod -Uri "$BaseUrl/api/orders" -Method Post -Body $body -ContentType "application/json" -TimeoutSec 5
        $reqSw.Stop()
        $lat = [math]::Round($reqSw.Elapsed.TotalMilliseconds, 1)
        $latencies += $lat
        $successCount++
        Write-Host "[$i/$Requests] Order: $($response.orderId) | Latency: $($lat)ms" -ForegroundColor Green
    }
    catch {
        $reqSw.Stop()
        $failCount++
        Write-Host "[$i/$Requests] Request Failed: $($_.Exception.Message)" -ForegroundColor Red
    }
    Start-Sleep -Milliseconds (Get-Random -Minimum 20 -Maximum 80)
}

$sw.Stop()
$totalSec = [math]::Round($sw.Elapsed.TotalSeconds, 2)
$rps = [math]::Round($Requests / $totalSec, 2)
$avgLat = if ($latencies.Count -gt 0) { [math]::Round(($latencies | Measure-Object -Average).Average, 1) } else { 0 }

Write-Host ""
Write-Host "------------- SIMULATION SUMMARY -------------" -ForegroundColor Yellow
Write-Host "Total Requests Processed : $Requests"
Write-Host "Successful Orders        : $successCount"
Write-Host "Failed Requests          : $failCount"
Write-Host "Total Elapsed Time       : $totalSec s"
Write-Host "Throughput Rate          : $rps Req/Sec"
Write-Host "Average Latency          : $avgLat ms"
Write-Host "----------------------------------------------" -ForegroundColor Yellow
