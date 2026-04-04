# Auto-Pull Tickets from Android Device
# This script runs in the background and downloads tickets automatically

$adbPath = "E:\SDK\platform-tools\adb.exe"
$devicePath = "/sdcard/Android/data/com.sktc.ticketprinter/files/tickets/"
$localFolder = "E:\App\saved_tickets"
$checkIntervalSeconds = 3

# Create local folder if it doesn't exist
if (-not (Test-Path $localFolder)) {
    New-Item -ItemType Directory -Path $localFolder -Force | Out-Null
    Write-Host "Created folder: $localFolder"
}

Write-Host "=" * 60
Write-Host "TICKET AUTO-PULL SERVICE STARTED"
Write-Host "Watching for new tickets every $checkIntervalSeconds seconds..."
Write-Host "Local Folder: $localFolder"
Write-Host "Device Path: $devicePath"
Write-Host "Press Ctrl+C to stop"
Write-Host "=" * 60
Write-Host ""

$lastTicketCount = 0

while ($true) {
    try {
        # List all tickets on device (both PNG and JPG)
        $deviceTickets = & $adbPath shell "find $devicePath -type f \( -name '*.png' -o -name '*.jpg' \) 2>/dev/null" 2>$null | Where-Object { $_ -match "Ticket_" }
        $currentCount = ($deviceTickets | Measure-Object).Count
        
        if ($currentCount -gt 0) {
            # Check if new tickets were added
            if ($currentCount -gt $lastTicketCount) {
                $newTicketCount = $currentCount - $lastTicketCount
                Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Found $newTicketCount new ticket(s)" -ForegroundColor Green
                
                # Pull all tickets
                & $adbPath pull $devicePath $localFolder 2>&1 | ForEach-Object {
                    if ($_ -match "pulling") {
                        Write-Host "[$(Get-Date -Format 'HH:mm:ss')] $_" -ForegroundColor Cyan
                    }
                }
                
                $lastTicketCount = $currentCount
                
                # Show downloaded files (both PNG and JPG)
                Get-ChildItem -Path $localFolder -Recurse -Filter "*.png" -ErrorAction SilentlyContinue | ForEach-Object {
                    Write-Host "  ✓ $(Split-Path $_.FullName -Leaf)" -ForegroundColor Yellow
                }
                Get-ChildItem -Path $localFolder -Recurse -Filter "*.jpg" -ErrorAction SilentlyContinue | ForEach-Object {
                    Write-Host "  ✓ $(Split-Path $_.FullName -Leaf)" -ForegroundColor Yellow
                }
            }
        }
    }
    catch {
        Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Error: $_" -ForegroundColor Red
    }
    
    Start-Sleep -Seconds $checkIntervalSeconds
}
