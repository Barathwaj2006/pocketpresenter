# Register COM Add-In in HKCU
$clsid = "{4B9A180E-7C29-4F54-BAE6-9DC777B6BE14}"
$progId = "PocketPresenter.AddIn"
$className = "PocketPresenter.AddIn.ThisAddIn"
$asmFullName = "PocketPresenter.AddIn, Version=1.0.0.0, Culture=neutral, PublicKeyToken=null"
$runtimeVersion = "v4.0.30319"
$dllPath = (Resolve-Path "windows-addin\src\PocketPresenter.AddIn\bin\Release\net48\PocketPresenter.AddIn.dll").Path
$codeBase = ([System.Uri]([System.IO.FileInfo]$dllPath).FullName).AbsoluteUri

Write-Host "Registering COM AddIn for $dllPath"
Write-Host "CodeBase: $codeBase"

# 1. ProgID
$progKey = "HKCU:\Software\Classes\$progId"
New-Item -Path $progKey -Force | Out-Null
Set-ItemProperty -Path $progKey -Name "(default)" -Value $className
New-Item -Path "$progKey\CLSID" -Force | Out-Null
Set-ItemProperty -Path "$progKey\CLSID" -Name "(default)" -Value $clsid

# 2. CLSID
$clsidKey = "HKCU:\Software\Classes\CLSID\$clsid"
New-Item -Path $clsidKey -Force | Out-Null
Set-ItemProperty -Path $clsidKey -Name "(default)" -Value $className

# 3. InprocServer32
$inprocKey = "$clsidKey\InprocServer32"
New-Item -Path $inprocKey -Force | Out-Null
Set-ItemProperty -Path $inprocKey -Name "(default)" -Value "mscoree.dll"
Set-ItemProperty -Path $inprocKey -Name "ThreadingModel" -Value "Both"
Set-ItemProperty -Path $inprocKey -Name "Class" -Value $className
Set-ItemProperty -Path $inprocKey -Name "Assembly" -Value $asmFullName
Set-ItemProperty -Path $inprocKey -Name "RuntimeVersion" -Value $runtimeVersion
Set-ItemProperty -Path $inprocKey -Name "CodeBase" -Value $codeBase

# 4. InprocServer32\1.0.0.0
$verKey = "$inprocKey\1.0.0.0"
New-Item -Path $verKey -Force | Out-Null
Set-ItemProperty -Path $verKey -Name "Class" -Value $className
Set-ItemProperty -Path $verKey -Name "Assembly" -Value $asmFullName
Set-ItemProperty -Path $verKey -Name "RuntimeVersion" -Value $runtimeVersion
Set-ItemProperty -Path $verKey -Name "CodeBase" -Value $codeBase

# 5. ProgId subkey under CLSID
New-Item -Path "$clsidKey\ProgId" -Force | Out-Null
Set-ItemProperty -Path "$clsidKey\ProgId" -Name "(default)" -Value $progId

# 6. Implemented Categories for managed COM
$catKey = "$clsidKey\Implemented Categories\{62C8FE65-4EBB-45E7-B440-6E39B2CDBF29}"
New-Item -Path $catKey -Force | Out-Null

# 7. PowerPoint Addin Registration
$pptKey = "HKCU:\Software\Microsoft\Office\PowerPoint\Addins\$progId"
New-Item -Path $pptKey -Force | Out-Null
Set-ItemProperty -Path $pptKey -Name "FriendlyName" -Value "PocketPresenter"
Set-ItemProperty -Path $pptKey -Name "Description" -Value "PocketPresenter Presentation Controller"
Set-ItemProperty -Path $pptKey -Name "LoadBehavior" -Value 3 -Type DWord
Set-ItemProperty -Path $pptKey -Name "CommandLineSafe" -Value 0 -Type DWord

Write-Host "COM registration completed successfully!"
