[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$mavenImage = 'maven:3.9.12-eclipse-temurin-21-alpine@sha256:8b2f036477a5bc9fbeb16cfb7301c484d7fff727b1c4907301ac665526bd7a8e'

docker run --rm `
    --volume "${repositoryRoot}:/workspace" `
    --workdir /workspace `
    --env OPENAPI_CONTRACT_UPDATE_PATH=/workspace/openapi/openapi-v1.json `
    $mavenImage `
    mvn --batch-mode --no-transfer-progress `
        '-Dtest=FinanceControlFinanceServiceApplicationTests#runtimeOpenApiMatchesVersionedContract' `
        test

if ($LASTEXITCODE -ne 0) {
    throw "OpenAPI contract generation failed with exit code $LASTEXITCODE."
}

Write-Host "OpenAPI contract updated at $(Join-Path $repositoryRoot 'openapi\openapi-v1.json')"
