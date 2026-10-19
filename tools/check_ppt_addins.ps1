try {
    $ppt = [System.Activator]::CreateInstance([System.Type]::GetTypeFromProgID('PowerPoint.Application'))
    Write-Host "Connected to PowerPoint.Application: Version $($ppt.Version)"
    $addins = $ppt.COMAddIns
    Write-Host "Total COM AddIns count: $($addins.Count)"
    for ($i = 1; $i -le $addins.Count; $i++) {
        $item = $addins.Item($i)
        Write-Host "[$i] ProgId: '$($item.ProgId)', Description: '$($item.Description)', Connected: $($item.Connect)"
        if ($item.ProgId -eq 'PocketPresenter.AddIn') {
            if (-not $item.Connect) {
                Write-Host "Connecting PocketPresenter.AddIn..."
                $item.Connect = $true
                Write-Host "Successfully connected! Connected = $($item.Connect)"
            }
        }
    }
} catch {
    Write-Host "Error: $_"
    Write-Host $_.Exception.ToString()
}
