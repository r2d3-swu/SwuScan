$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$shortcutPath = Join-Path ([Environment]::GetFolderPath("Desktop")) "SwuScan.lnk"
$targetPath = Join-Path $projectDir "run.bat"

$shell = New-Object -ComObject WScript.Shell
$shortcut = $shell.CreateShortcut($shortcutPath)
$shortcut.TargetPath = $targetPath
$shortcut.WorkingDirectory = $projectDir
$shortcut.Description = "Start SwuScanDesktop"
$shortcut.IconLocation = "$env:SystemRoot\System32\shell32.dll,167"
$shortcut.Save()

Write-Host "Created shortcut: $shortcutPath"
