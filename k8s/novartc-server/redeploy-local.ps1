param(
    [string]$ImageTag = (Get-Date -Format 'yyyyMMdd-HHmmss'),
    [string]$Namespace = 'novartc',
    [string]$WorkloadName = 'novartc-server'
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
$workloadKind = 'deployment'

if (-not (Test-Path (Join-Path $repoRoot 'pom.xml'))) {
    throw "Repository root not found: $repoRoot"
}

if (-not (Test-Path (Join-Path $serverDir 'Dockerfile'))) {
    throw "Dockerfile not found: $(Join-Path $serverDir 'Dockerfile')"
}

Write-Host "[1/6] Maven package novartc-server..." -ForegroundColor Cyan
Push-Location $repoRoot
try {
    mvn -pl novartc-server -am -DskipTests package
    Assert-LastExitCode 'Maven package'
}
finally {
    Pop-Location
}

Write-Host "[2/6] Build image $imageName ..." -ForegroundColor Cyan
Push-Location $serverDir
try {
    docker build -t $imageName .
    Assert-LastExitCode 'Docker build'
}
finally {
    Pop-Location
}

Write-Host "[3/6] Update deployment image tag..." -ForegroundColor Cyan
$content = Get-Content -Path $deploymentYaml -Raw
$newContent = [regex]::Replace($content, 'image:\s*novartc-server:[^\s]+', "image: $imageName")
Set-Content -Path $deploymentYaml -Value $newContent

if ($newContent -match 'kind:\s*StatefulSet') {
    $workloadKind = 'statefulset'
}

if ($workloadKind -eq 'statefulset') {
    Write-Host "[4/6] Remove legacy deployment with same name if it exists..." -ForegroundColor Cyan
    kubectl -n $Namespace delete deployment $WorkloadName --ignore-not-found=true
    Assert-LastExitCode 'kubectl delete legacy deployment'
} else {
    Write-Host "[4/6] No legacy deployment cleanup needed..." -ForegroundColor DarkGray
}

Write-Host "[5/6] Apply Kubernetes manifests..." -ForegroundColor Cyan
kubectl apply -k (Join-Path $PSScriptRoot 'base')
Assert-LastExitCode 'kubectl apply'

Write-Host "[6/6] Wait for rollout..." -ForegroundColor Cyan
kubectl -n $Namespace rollout status "$workloadKind/$WorkloadName" --timeout=180s
Assert-LastExitCode 'kubectl rollout status'

Write-Host ""
Write-Host "Done." -ForegroundColor Green
Write-Host "Image: $imageName"
Write-Host "Verify:" -ForegroundColor Yellow
Write-Host "  kubectl -n $Namespace get pods -l app=novartc-server -o wide"
Write-Host "  curl.exe -i http://127.0.0.1:30070/actuator/health/readiness"
Write-Host "  Test-NetConnection -ComputerName 127.0.0.1 -Port 30688"
