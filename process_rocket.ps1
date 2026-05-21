Add-Type -AssemblyName System.Drawing

$inputFile = "C:\Users\User\.gemini\antigravity\brain\1ac34790-fb5d-40df-b92a-46ddebe1970f\rocket_spritesheet_1779364477124.png"
$outputFile = "d:\Projects\AstroDrill\Game\assets\textures\rocket_anim.png"

$bmp = New-Object System.Drawing.Bitmap $inputFile
$bmp.MakeTransparent([System.Drawing.Color]::White)
$bmp.Save($outputFile, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Host "Done"
