<#
  Encerra o tunel da Cloudflare e o nginx iniciados por iniciar-tunel.ps1.
  Nao mexe no backend nem no ng serve.
#>
param([switch]$Silencioso)

$Infra = $PSScriptRoot
$NginxPrefixo = Join-Path $Infra 'nginx'
$NginxExe = Join-Path $NginxPrefixo 'bin\nginx.exe'
$Estado = Join-Path $Infra '.estado'

function Log($msg) { if (-not $Silencioso) { Write-Host $msg } }

# Tunel
$pidFile = Join-Path $Estado 'cloudflared.pid'
if (Test-Path $pidFile) {
    $tunelPid = Get-Content $pidFile | Select-Object -First 1
    $p = Get-Process -Id $tunelPid -ErrorAction SilentlyContinue
    if ($p -and $p.ProcessName -eq 'cloudflared') {
        Stop-Process -Id $tunelPid -Force -Confirm:$false
        Log "[ok] tunel encerrado"
    }
    Remove-Item $pidFile -Force
}
Remove-Item (Join-Path $Estado 'url.txt') -Force -ErrorAction SilentlyContinue

# nginx: somente processos do executavel deste projeto
$nginx = Get-Process -Name nginx -ErrorAction SilentlyContinue | Where-Object { $_.Path -eq $NginxExe }
if ($nginx) {
    $nginx | Stop-Process -Force -Confirm:$false
    Log "[ok] nginx encerrado"
}
Remove-Item (Join-Path $NginxPrefixo 'logs\nginx.pid') -Force -ErrorAction SilentlyContinue

if (-not $nginx -and -not $tunelPid) { Log "Nada em execucao." }
