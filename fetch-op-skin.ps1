Start-Sleep -Seconds 1
$uri = 'https://sessionserver.mojang.com/session/minecraft/profile/35a509031bd541a1822683d44fe4c51d?unsigned=false'
$r = Invoke-WebRequest -Uri $uri -UseBasicParsing -TimeoutSec 25
Set-Content -Path 'D:\FrostSMP\op-skin.json' -Value $r.Content -Encoding UTF8
Write-Output 'saved'
