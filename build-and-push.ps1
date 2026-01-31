#!/usr/bin/env pwsh
# Script pour construire et pousser l'image Docker vers un registre

param(
    [string]$Registry = "docker.io",  # docker.io, ghcr.io, ou votre registre privé
    [string]$Username = "",           # Votre nom d'utilisateur du registre
    [string]$ImageName = "devtechly", # Nom de l'image
    [string]$Tag = "v1.0.4",          # Tag de l'image
    [switch]$SkipBuild = $false,      # Passer la construction si l'image existe déjà
    [switch]$SkipPush = $false        # Ne pas pousser vers le registre
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Build & Push Docker Image to Registry" -ForegroundColor Cyan
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

# Déterminer le nom complet de l'image
if ($Registry -eq "docker.io") {
    if ([string]::IsNullOrEmpty($Username)) {
        Write-Host "[ERREUR] Username requis pour Docker Hub" -ForegroundColor Red
        Write-Host "   Utilisation: .\build-and-push.ps1 -Username votre-username" -ForegroundColor Yellow
        exit 1
    }
    $FullImageName = "${Username}/${ImageName}:${Tag}"
} elseif ($Registry -eq "ghcr.io") {
    if ([string]::IsNullOrEmpty($Username)) {
        Write-Host "[ERREUR] Username requis pour GitHub Container Registry" -ForegroundColor Red
        Write-Host "   Utilisation: .\build-and-push.ps1 -Registry ghcr.io -Username votre-username" -ForegroundColor Yellow
        exit 1
    }
    $FullImageName = "${Registry}/${Username}/${ImageName}:${Tag}"
} else {
    # Registre privé (ex: registry.example.com)
    if ([string]::IsNullOrEmpty($Username)) {
        $FullImageName = "${Registry}/${ImageName}:${Tag}"
    } else {
        $FullImageName = "${Registry}/${Username}/${ImageName}:${Tag}"
    }
}

Write-Host "[*] Configuration:" -ForegroundColor Cyan
Write-Host "   Registry: $Registry" -ForegroundColor White
Write-Host "   Image: $FullImageName" -ForegroundColor White
Write-Host ""

# Étape 1: Build de l'image
if (-not $SkipBuild) {
    Write-Host "[*] Construction de l'image Docker..." -ForegroundColor Cyan
    Write-Host "   Cela peut prendre 10-15 minutes..." -ForegroundColor Yellow
    
    docker build -f Dockerfile.jhipster -t $FullImageName -t "${ImageName}:${Tag}" .
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERREUR] Echec de la construction de l'image" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "[OK] Image construite avec succes: $FullImageName" -ForegroundColor Green
    Write-Host ""
} else {
    Write-Host "[*] Construction ignoree (SkipBuild)" -ForegroundColor Yellow
    Write-Host ""
}

# Étape 2: Push vers le registre
if (-not $SkipPush) {
    Write-Host "[*] Connexion au registre..." -ForegroundColor Cyan
    
    # Vérifier si l'utilisateur est déjà connecté
    if ($Registry -eq "docker.io") {
        Write-Host "   Pour Docker Hub, assurez-vous d'etre connecte:" -ForegroundColor Yellow
        Write-Host "   docker login" -ForegroundColor White
    } elseif ($Registry -eq "ghcr.io") {
        Write-Host "   Pour GitHub Container Registry, connectez-vous avec:" -ForegroundColor Yellow
        Write-Host "   echo `$env:GITHUB_TOKEN | docker login ghcr.io -u $Username --password-stdin" -ForegroundColor White
        Write-Host "   (GITHUB_TOKEN doit etre defini dans votre environnement)" -ForegroundColor Yellow
    } else {
        Write-Host "   Pour un registre prive, connectez-vous avec:" -ForegroundColor Yellow
        Write-Host "   docker login $Registry" -ForegroundColor White
    }
    
    Write-Host ""
    Write-Host "[*] Envoi de l'image vers le registre..." -ForegroundColor Cyan
    
    docker push $FullImageName
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERREUR] Echec de l'envoi de l'image" -ForegroundColor Red
        Write-Host "   Verifiez que vous etes connecte au registre" -ForegroundColor Yellow
        exit 1
    }
    
    Write-Host "[OK] Image envoyee avec succes vers $FullImageName" -ForegroundColor Green
    Write-Host ""
} else {
    Write-Host "[*] Envoi ignore (SkipPush)" -ForegroundColor Yellow
    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "[OK] Operation terminee avec succes!" -ForegroundColor Green
Write-Host ""
Write-Host "Image disponible sur: $FullImageName" -ForegroundColor Cyan
Write-Host ""
Write-Host "Pour deployer cette image:" -ForegroundColor Yellow
Write-Host "   .\deploy-from-registry.ps1 -ImageName $FullImageName" -ForegroundColor White
Write-Host ""
