#!/usr/bin/env pwsh
# Script pour construire et pousser les images séparées (Backend + Frontend)

param(
    [string]$Registry = "docker.io",
    [string]$Username = "mocherqu",
    [string]$BackendImageName = "devtechly-backend",
    [string]$FrontendImageName = "devtechly-frontend",
    [string]$Tag = "latest",
    [switch]$SkipBuild = $false,
    [switch]$SkipPush = $false,
    [switch]$BackendOnly = $false,
    [switch]$FrontendOnly = $false
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Build & Push Images Separated" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Vérifier Docker
try {
    docker --version | Out-Null
    Write-Host "[OK] Docker detecte" -ForegroundColor Green
} catch {
    Write-Host "[ERREUR] Docker n'est pas disponible!" -ForegroundColor Red
    exit 1
}

# Déterminer les noms complets des images
if ($Registry -eq "docker.io") {
    $BackendFullName = "${Username}/${BackendImageName}:${Tag}"
    $FrontendFullName = "${Username}/${FrontendImageName}:${Tag}"
} elseif ($Registry -eq "ghcr.io") {
    $BackendFullName = "${Registry}/${Username}/${BackendImageName}:${Tag}"
    $FrontendFullName = "${Registry}/${Username}/${FrontendImageName}:${Tag}"
} else {
    $BackendFullName = "${Registry}/${Username}/${BackendImageName}:${Tag}"
    $FrontendFullName = "${Registry}/${Username}/${FrontendImageName}:${Tag}"
}

Write-Host "[*] Configuration:" -ForegroundColor Cyan
Write-Host "   Registry: $Registry" -ForegroundColor White
Write-Host "   Backend: $BackendFullName" -ForegroundColor White
Write-Host "   Frontend: $FrontendFullName" -ForegroundColor White
Write-Host ""

# Build Backend
if (-not $SkipBuild -and -not $FrontendOnly) {
    Write-Host "[*] Construction de l'image Backend..." -ForegroundColor Cyan
    Write-Host "   Cela peut prendre 5-10 minutes..." -ForegroundColor Yellow
    
    docker build -f Dockerfile.backend -t $BackendFullName -t "${BackendImageName}:${Tag}" .
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERREUR] Echec de la construction du backend" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "[OK] Backend construit avec succes" -ForegroundColor Green
    Write-Host ""
}

# Build Frontend
if (-not $SkipBuild -and -not $BackendOnly) {
    Write-Host "[*] Construction de l'image Frontend..." -ForegroundColor Cyan
    Write-Host "   Cela peut prendre 3-5 minutes..." -ForegroundColor Yellow
    
    docker build -f Dockerfile.frontend -t $FrontendFullName -t "${FrontendImageName}:${Tag}" .
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERREUR] Echec de la construction du frontend" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "[OK] Frontend construit avec succes" -ForegroundColor Green
    Write-Host ""
}

# Push Backend
if (-not $SkipPush -and -not $FrontendOnly) {
    Write-Host "[*] Envoi du Backend vers le registre..." -ForegroundColor Cyan
    docker push $BackendFullName
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERREUR] Echec de l'envoi du backend" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "[OK] Backend envoye avec succes" -ForegroundColor Green
    Write-Host ""
}

# Push Frontend
if (-not $SkipPush -and -not $BackendOnly) {
    Write-Host "[*] Envoi du Frontend vers le registre..." -ForegroundColor Cyan
    docker push $FrontendFullName
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERREUR] Echec de l'envoi du frontend" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "[OK] Frontend envoye avec succes" -ForegroundColor Green
    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "[OK] Operation terminee avec succes!" -ForegroundColor Green
Write-Host ""
Write-Host "Images disponibles:" -ForegroundColor Cyan
Write-Host "   Backend: $BackendFullName" -ForegroundColor White
Write-Host "   Frontend: $FrontendFullName" -ForegroundColor White
Write-Host ""
Write-Host "Pour deployer:" -ForegroundColor Yellow
Write-Host "   docker compose -f docker-compose.separated.yml up -d" -ForegroundColor White
Write-Host ""
