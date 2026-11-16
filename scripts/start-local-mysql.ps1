param(
    [string]$MySqlDirectory = (Join-Path $env:USERPROFILE '.codex\tmp\skillswap-mysql')
)
$ErrorActionPreference = 'Stop'
$configuration = Join-Path $MySqlDirectory 'my.ini'
$binary = Get-ChildItem -LiteralPath $MySqlDirectory -Directory -Filter 'mysql-8.4*-winx64' |
    Select-Object -First 1 -ExpandProperty FullName
if (-not $binary -or -not (Test-Path -LiteralPath $configuration)) {
    throw 'No se encontró la instancia portable preparada. Usá docker compose up -d o indicá -MySqlDirectory.'
}
if (Get-NetTCPConnection -LocalPort 3307 -State Listen -ErrorAction SilentlyContinue) {
    Write-Host 'El puerto local 3307 ya está escuchando.'
    exit 0
}
$process = Start-Process -FilePath (Join-Path $binary 'bin\mysqld.exe') `
    -ArgumentList ('--defaults-file="' + $configuration + '"') -WindowStyle Hidden -PassThru
$process.Id | Set-Content -LiteralPath (Join-Path $MySqlDirectory 'process-id.txt')
for ($attempt = 0; $attempt -lt 30; $attempt++) {
    if (Get-NetTCPConnection -LocalPort 3307 -State Listen -ErrorAction SilentlyContinue) {
        Write-Host 'MySQL disponible en 127.0.0.1:3307.'
        exit 0
    }
    if ($process.HasExited) { throw 'MySQL terminó antes de abrir el puerto; revisá server.log.' }
    Start-Sleep -Milliseconds 500
}
throw 'MySQL no abrió el puerto en el tiempo esperado; revisá server.log.'
