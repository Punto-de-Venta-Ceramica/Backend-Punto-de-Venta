$ErrorActionPreference = 'Stop'
$backendRoot = Split-Path -Parent $PSScriptRoot
$jdkDirectory = Join-Path (Split-Path -Parent $backendRoot) '.tools\jdk-download'
$localJdk = Get-ChildItem -LiteralPath $jdkDirectory -Directory -Filter 'jdk-*' -ErrorAction SilentlyContinue | Select-Object -First 1
if ($localJdk) {
    $env:JAVA_HOME = $localJdk.FullName
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
}
Push-Location $backendRoot
try {
    & .\mvnw.cmd spring-boot:run
    if ($LASTEXITCODE -ne 0) { throw "El backend terminó con código $LASTEXITCODE" }
} finally {
    Pop-Location
}
