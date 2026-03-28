param(
    [string]$ImageTag = (Get-Date -Format 'yyyyMMdd-HHmmss'),
    [string]$Namespace = 'novartc',
    [string]$DeploymentName = 'novartc-server'
)

$ErrorActionPreference = 'Stop'

function Assert-LastExitCode {
    param(
        [string]$StepName
    )

    if ($LASTEXITCODE -ne 0) {
        throw "$StepName failed with exit code $LASTEXITCODE"
    }
}

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$serverDir = Join-Path $repoRoot 'novartc-server'
$deploymentYaml = Join-Path $PSScriptRoot 'base\deployment.yaml'
$imageName = "novartc-server:$ImageTag"

if (-not (Test-Path (Join-Path $repoRoot 'pom.xml'))) {
    throw "Repository root not found: $repoRoot"
}

if (-not (Test-Path (Join-Path $serverDir 'Dockerfile'))) {
    throw "Dockerfile not found: $(Join-Path $serverDir 'Dockerfile')"
}

Write-Host "[1/5] Maven package novartc-server..." -ForegroundColor Cyan
Push-Location $repoRoot
try {
    mvn -pl novartc-server -am -DskipTests package
    Assert-LastExitCode 'Maven package'
}
finally {
    Pop-Location
}

Write-Host "[2/5] Build image $imageName ..." -ForegroundColor Cyan
Push-Location $serverDir
try {
    docker build -t $imageName .
    Assert-LastExitCode 'Docker build'
}
finally {
    Pop-Location
}

Write-Host "[3/5] Update deployment image tag..." -ForegroundColor Cyan
$content = Get-Content -Path $deploymentYaml -Raw
$newContent = [regex]::Replace($content, 'image:\s*novartc-server:[^\s]+', "image: $imageName")
Set-Content -Path $deploymentYaml -Value $newContent

Write-Host "[4/5] Apply Kubernetes manifests..." -ForegroundColor Cyan
kubectl apply -k (Join-Path $PSScriptRoot 'base')
Assert-LastExitCode 'kubectl apply'

Write-Host "[5/5] Wait for rollout..." -ForegroundColor Cyan
kubectl -n $Namespace rollout status deployment/$DeploymentName --timeout=180s
Assert-LastExitCode 'kubectl rollout status'

Write-Host ""
Write-Host "Done." -ForegroundColor Green
Write-Host "Image: $imageName"
Write-Host "Verify:" -ForegroundColor Yellow
Write-Host "  curl.exe -i http://127.0.0.1:30070/actuator/health/readiness"
Write-Host "  Test-NetConnection -ComputerName 127.0.0.1 -Port 30688"
