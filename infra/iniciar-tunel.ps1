<#
  Publica o File Reader para a equipe: nginx (frontend + /api) exposto por um tunel rapido da Cloudflare.

  Pre-requisito: backend rodando em http://localhost:8080 (cd backend; .\mvnw.cmd spring-boot:run).

  Uso:
    powershell -ExecutionPolicy Bypass -File infra\iniciar-tunel.ps1
    powershell -ExecutionPolicy Bypass -File infra\iniciar-tunel.ps1 -PularBuild   # reaproveita o build atual do frontend

  O tunel rapido gera uma URL https://<aleatorio>.trycloudflare.com que muda a cada execucao.
  Para encerrar: infra\parar-tunel.ps1
#>
param([switch]$PularBuild)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$Infra = $PSScriptRoot
$Raiz = Split-Path -Parent $Infra
$NginxVersao = '1.30.0'
$NginxPrefixo = Join-Path $Infra 'nginx'
$NginxBin = Join-Path $NginxPrefixo 'bin'
$NginxExe = Join-Path $NginxBin 'nginx.exe'
$Estado = Join-Path $Infra '.estado'
$PortaNginx = 8088

function Testar-Url($url) {
    try {
        $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5
        return $r.StatusCode -eq 200
    } catch {
        return $false
    }
}

# 1. Backend
if (-not (Testar-Url 'http://127.0.0.1:8080/api/servicos')) {
    throw "Backend nao respondeu em http://localhost:8080. Suba o backend antes (cd backend; .\mvnw.cmd spring-boot:run)."
}
Write-Host "[ok] backend respondendo em :8080"

# 2. nginx portatil (baixado uma unica vez)
if (-not (Test-Path $NginxExe)) {
    Write-Host "Baixando nginx $NginxVersao..."
    $zip = Join-Path $env:TEMP "nginx-$NginxVersao.zip"
    $destinoTemp = Join-Path $env:TEMP "nginx-$NginxVersao-extraido"
    Invoke-WebRequest -Uri "https://nginx.org/download/nginx-$NginxVersao.zip" -OutFile $zip -UseBasicParsing
    if (Test-Path $destinoTemp) { Remove-Item $destinoTemp -Recurse -Force }
    Expand-Archive -Path $zip -DestinationPath $destinoTemp
    New-Item -ItemType Directory -Force $NginxBin | Out-Null
    Copy-Item -Path (Join-Path $destinoTemp "nginx-$NginxVersao\*") -Destination $NginxBin -Recurse -Force
    Remove-Item $zip, $destinoTemp -Recurse -Force
}
Copy-Item (Join-Path $NginxBin 'conf\mime.types') (Join-Path $NginxPrefixo 'conf\mime.types') -Force
foreach ($d in 'logs', 'temp') { New-Item -ItemType Directory -Force (Join-Path $NginxPrefixo $d) | Out-Null }
New-Item -ItemType Directory -Force $Estado | Out-Null
Write-Host "[ok] nginx $NginxVersao em $NginxBin"

# 3. Build de producao do frontend
if (-not $PularBuild) {
    Write-Host "Gerando build de producao do frontend..."
    Push-Location (Join-Path $Raiz 'frontend')
    try {
        npm run build
        if ($LASTEXITCODE -ne 0) { throw "Falha no build do frontend" }
    } finally {
        Pop-Location
    }
}
if (-not (Test-Path (Join-Path $Raiz 'frontend\dist\frontend\browser\index.html'))) {
    throw "Build do frontend nao encontrado em frontend\dist\frontend\browser. Rode sem -PularBuild."
}
Write-Host "[ok] build do frontend"

# 4. Encerra execucoes anteriores
& (Join-Path $Infra 'parar-tunel.ps1') -Silencioso

# 5. nginx
$prefixoArg = ($NginxPrefixo -replace '\\', '/') + '/'
# nginx -t escreve no stderr mesmo quando a configuracao esta correta
$ErrorActionPreference = 'Continue'
$saidaTeste = & $NginxExe -p $prefixoArg -c conf/nginx.conf -t 2>&1
$codigoTeste = $LASTEXITCODE
$ErrorActionPreference = 'Stop'
if ($codigoTeste -ne 0) {
    $saidaTeste | ForEach-Object { Write-Host $_ }
    throw "Configuracao do nginx invalida"
}
Start-Process -FilePath $NginxExe -ArgumentList '-p', $prefixoArg, '-c', 'conf/nginx.conf' -WorkingDirectory $NginxBin -WindowStyle Hidden
Start-Sleep -Seconds 2
if (-not (Testar-Url "http://127.0.0.1:$PortaNginx/") -or -not (Testar-Url "http://127.0.0.1:$PortaNginx/api/servicos")) {
    throw "nginx nao respondeu em http://127.0.0.1:$PortaNginx. Veja infra\nginx\logs\error.log"
}
Write-Host "[ok] nginx servindo frontend e /api em http://127.0.0.1:$PortaNginx"

# 6. Tunel rapido da Cloudflare
$logTunel = Join-Path $Estado 'cloudflared.log'
if (Test-Path $logTunel) { Remove-Item $logTunel -Force }
$cloudflared = (Get-Command cloudflared -ErrorAction Stop).Source
$processo = Start-Process -FilePath $cloudflared `
    -ArgumentList 'tunnel', '--no-autoupdate', '--url', "http://127.0.0.1:$PortaNginx", '--logfile', $logTunel `
    -WindowStyle Hidden -PassThru
Set-Content -Path (Join-Path $Estado 'cloudflared.pid') -Value $processo.Id

$url = $null
for ($i = 0; $i -lt 60 -and -not $url; $i++) {
    Start-Sleep -Seconds 1
    if (Test-Path $logTunel) {
        $m = Select-String -Path $logTunel -Pattern 'https://[a-z0-9-]+\.trycloudflare\.com' | Select-Object -First 1
        if ($m) { $url = $m.Matches[0].Value }
    }
    if ($processo.HasExited) { break }
}
if (-not $url) {
    throw "Nao foi possivel obter a URL do tunel. Veja $logTunel"
}
Set-Content -Path (Join-Path $Estado 'url.txt') -Value $url

Write-Host ""
Write-Host "==============================================================="
Write-Host " File Reader publicado em: $url"
Write-Host " (a URL pode levar alguns segundos para responder; muda a cada execucao)"
Write-Host " Para encerrar: infra\parar-tunel.ps1"
Write-Host "==============================================================="
