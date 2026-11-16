param(
    [string]$DatabaseUrl = 'jdbc:mysql://127.0.0.1:3307/skillswap?connectionTimeZone=UTC',
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$UserId = 'user-mateo'
)
$ErrorActionPreference = 'Stop'
$jar = Join-Path $PSScriptRoot '..\app\target\app-0.1.0-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw 'Primero ejecutá mvn clean verify desde la raíz.' }
if (-not $JavaHome) {
    $prepared = Join-Path $env:USERPROFILE '.codex\tmp\skillswap-review-tools'
    if (Test-Path -LiteralPath $prepared) {
        $JavaHome = Get-ChildItem -LiteralPath $prepared -Directory -Filter 'jdk-17*' |
            Select-Object -First 1 -ExpandProperty FullName
    }
}
if ($JavaHome) { $java = Join-Path $JavaHome 'bin\java.exe' }
else { $java = (Get-Command java -ErrorAction Stop).Source }
$env:SKILLSWAP_DB_URL = $DatabaseUrl
$env:SKILLSWAP_DB_USER = 'skillswap'
$env:SKILLSWAP_DB_PASSWORD = 'skillswap'
& $java -jar $jar --demo "--user=$UserId"
if ($LASTEXITCODE -ne 0) { throw "SkillSwap terminó con código $LASTEXITCODE" }
