#!/usr/bin/env pwsh
# Script de build pour la production

Write-Host "[*] Verification de Docker..." -ForegroundColor Cyan

# Vérifier Docker
try {
    $dockerVersion = docker --version 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Docker non disponible"
    }
    Write-Host "[OK] Docker detecte: $dockerVersion" -ForegroundColor Green
} catch {
    Write-Host "[ERREUR] Docker Desktop n'est pas demarre!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Veuillez:" -ForegroundColor Yellow
    Write-Host "1. Demarrer Docker Desktop depuis le menu Demarrer" -ForegroundColor White
    Write-Host "2. Attendre que Docker soit pret (icone dans la barre des taches)" -ForegroundColor White
    Write-Host "3. Relancer ce script" -ForegroundColor White
    exit 1
}

Write-Host ""
Write-Host "[*] Arret des conteneurs existants..." -ForegroundColor Cyan
docker compose -f docker-compose.prod.yml down 2>$null

Write-Host ""
Write-Host "[*] Construction de l'image Docker de production..." -ForegroundColor Cyan
Write-Host "    Cela peut prendre 10-15 minutes..." -ForegroundColor Yellow

docker build -f Dockerfile.jhipster -t devtechly:latest .

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "[OK] Image construite avec succes!" -ForegroundColor Green
    Write-Host ""
    Write-Host "[*] Demarrage des services..." -ForegroundColor Cyan
    
    docker compose -f docker-compose.prod.yml up -d
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "[OK] Services demarres!" -ForegroundColor Green
        Write-Host ""
        Write-Host "[*] Statut des services:" -ForegroundColor Cyan
        docker compose -f docker-compose.prod.yml ps
        Write-Host ""
        Write-Host "[OK] Application accessible sur http://localhost:8080" -ForegroundColor Green
        Write-Host ""
        Write-Host "[*] Pour voir les logs:" -ForegroundColor Cyan
        Write-Host "   docker compose -f docker-compose.prod.yml logs -f" -ForegroundColor White
    } else {
        Write-Host "[ERREUR] Erreur lors du demarrage des services" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host ""
    Write-Host "[ERREUR] Erreur lors de la construction de l'image" -ForegroundColor Red
    Write-Host "   Verifiez les logs ci-dessus pour plus de details" -ForegroundColor Yellow
    exit 1
}










