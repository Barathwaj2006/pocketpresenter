# PocketPresenter — Gate 0 Live Hardware Feasibility Test Runner
# Verifies Proof 0.1 (Bluetooth RFCOMM), Proof 0.2 (PowerPoint Slideshow Control), and Proof 0.3 (Volume Buttons)

param(
    [string]$PortName = "COM4",
    [int]$BaudRate = 9600
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   PocketPresenter — Gate 0 Live Connectivity Test        " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Connect to PowerPoint
Write-Host "`n[1/3] Checking Microsoft PowerPoint..." -NoNewline
try {
    $ppt = [System.Runtime.InteropServices.Marshal]::GetActiveObject("PowerPoint.Application")
    Write-Host " CONNECTED" -ForegroundColor Green
    Write-Host "      PowerPoint Version: $($ppt.Version)" -ForegroundColor Gray
    if ($ppt.Presentations.Count -gt 0) {
        Write-Host "      Active Presentation: $($ppt.ActivePresentation.Name)" -ForegroundColor Gray
        $slideShow = $ppt.SlideShowWindows.Count -gt 0
        Write-Host "      Slide Show Running: $(if ($slideShow) { 'YES (F5 Active)' } else { 'NO (Press F5 in PowerPoint)' })" -ForegroundColor $(if ($slideShow) { 'Green' } else { 'Yellow' })
    } else {
        Write-Host "      (Tip: Open any presentation in PowerPoint and press F5)" -ForegroundColor Yellow
    }
} catch {
    Write-Host " NOT RUNNING" -ForegroundColor Red
    Write-Host "      Please open Microsoft PowerPoint on your PC first!" -ForegroundColor Yellow
}

# 2. Connect to Phone Bluetooth Serial Port
Write-Host "`n[2/3] Opening Bluetooth Serial Port ($PortName)..." -NoNewline
try {
    $port = New-Object System.IO.Ports.SerialPort $PortName, $BaudRate, [System.IO.Ports.Parity]::None, 8, [System.IO.Ports.StopBits]::One
    $port.ReadTimeout = 1000
    $port.WriteTimeout = 5000
    $port.NewLine = "`n"
    $port.Open()
    Write-Host " OPENED" -ForegroundColor Green
} catch {
    Write-Host " FAILED" -ForegroundColor Red
    Write-Host "      Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "      Check: Ensure Bluetooth is ON on your PC and Phone is paired." -ForegroundColor Yellow
    exit 1
}

# 3. Perform Protocol Handshake
Write-Host "`n[3/3] Sending 'hello' handshake to phone..." -NoNewline
try {
    $hello = '{"v":1,"type":"hello","seq":0,"payload":{"client":"addin","appVersion":"0.1.0","capabilities":["pointer","slides","effects"]}}'
    $port.WriteLine($hello)
    Write-Host " SENT" -ForegroundColor Green
} catch {
    Write-Host " FAILED TO WRITE" -ForegroundColor Red
    $port.Close()
    exit 1
}

Write-Host "`nListening for responses from your phone (Press Ctrl+C to stop)..." -ForegroundColor Cyan
Write-Host "----------------------------------------------------------"

$handshakeComplete = $false
$sessionId = $null

try {
    while ($true) {
        try {
            $line = $port.ReadLine()
            if ([string]::IsNullOrWhiteSpace($line)) { continue }

            $line = $line.Trim()
            
            # Parse JSON
            try {
                $msg = $line | ConvertFrom-Json
            } catch {
                Write-Host "Raw: $line" -ForegroundColor DarkGray
                continue
            }

            switch ($msg.type) {
                "hello.ack" {
                    $handshakeComplete = $true
                    $sessionId = $msg.payload.sessionId
                    Write-Host "`n>>> [HANDSHAKE SUCCESS] Connected to PocketPresenter!" -ForegroundColor Green
                    Write-Host "    Session ID : $sessionId" -ForegroundColor Green
                    Write-Host "    Capabilities: $($msg.payload.capabilities -join ', ')" -ForegroundColor Green
                    Write-Host "    Status: Phone is now in CONTROL mode!`n" -ForegroundColor Cyan
                }

                "slide.next" {
                    Write-Host ">>> [COMMAND] slide.next (Volume Up / Tap Next)" -ForegroundColor Yellow
                    try {
                        if ($ppt -and $ppt.SlideShowWindows.Count -gt 0) {
                            $ppt.SlideShowWindows.Item(1).View.Next()
                            Write-Host "    --> PowerPoint Advanced to Next Slide!" -ForegroundColor Green
                        } else {
                            Write-Host "    --> (Slide show not active. Press F5 in PowerPoint)" -ForegroundColor DarkYellow
                        }
                    } catch {
                        Write-Host "    --> COM Error: $($_.Exception.Message)" -ForegroundColor Red
                    }
                }

                "slide.previous" {
                    Write-Host ">>> [COMMAND] slide.previous (Volume Down / Tap Prev)" -ForegroundColor Yellow
                    try {
                        if ($ppt -and $ppt.SlideShowWindows.Count -gt 0) {
                            $ppt.SlideShowWindows.Item(1).View.Previous()
                            Write-Host "    --> PowerPoint Moved to Previous Slide!" -ForegroundColor Green
                        } else {
                            Write-Host "    --> (Slide show not active. Press F5 in PowerPoint)" -ForegroundColor DarkYellow
                        }
                    } catch {
                        Write-Host "    --> COM Error: $($_.Exception.Message)" -ForegroundColor Red
                    }
                }

                "pointer.delta" {
                    Write-Host ">>> [POINTER] dx: $($msg.payload.dx), dy: $($msg.payload.dy)" -ForegroundColor Magenta
                }

                "button.down" {
                    Write-Host ">>> [CLICK] Button Down: $($msg.payload.button)" -ForegroundColor White
                }

                "button.up" {
                    Write-Host ">>> [CLICK] Button Up: $($msg.payload.button)" -ForegroundColor White
                }

                "effect.set" {
                    Write-Host ">>> [EFFECT] Selected: $($msg.payload.effect), Intensity: $($msg.payload.intensity)" -ForegroundColor Cyan
                }

                default {
                    Write-Host ">>> [$($msg.type)] $line" -ForegroundColor Gray
                }
            }
        } catch [System.TimeoutException] {
            # Expected on idle timeout, continue listening
        }
    }
} finally {
    Write-Host "`nClosing connection..." -ForegroundColor Yellow
    $port.Close()
    Write-Host "Port closed safely." -ForegroundColor Green
}
