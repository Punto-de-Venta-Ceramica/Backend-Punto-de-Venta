# Escanea con SonarQube solo los módulos indicados.
# Uso:   .\scripts\sonar.ps1 -Token <TOKEN> -Modulos catalogo,inventario,autenticacion
# El token se genera en http://localhost:9000 (Mi cuenta > Seguridad). También puede
# ir en la variable de entorno SONAR_TOKEN para no escribirlo.
param(
    [string]$Token = $env:SONAR_TOKEN,
    [string[]]$Modulos = @("catalogo", "inventario", "autenticacion"),
    [string]$ProjectKey = "barrocontrol-backend",
    [string]$HostUrl = "http://localhost:9000"
)

if (-not $Token) { Write-Error "Falta el token (-Token o variable SONAR_TOKEN)."; exit 1 }

$inclusiones = ($Modulos | ForEach-Object { "**/$_/**" }) -join ","
Set-Location (Join-Path $PSScriptRoot "..")

& .\mvnw.cmd -Pcalidad clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:5.1.0.4751:sonar `
    "-Dsonar.projectKey=$ProjectKey" `
    "-Dsonar.projectName=BarroControl Backend" `
    "-Dsonar.host.url=$HostUrl" `
    "-Dsonar.token=$Token" `
    "-Dsonar.inclusions=$inclusiones" `
    "-Dsonar.coverage.inclusions=$inclusiones"
