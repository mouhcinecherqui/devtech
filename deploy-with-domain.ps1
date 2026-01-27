#!/usr/bin/env pwsh
# Script de déploiement avec nom de domaine (utilise Nginx comme reverse proxy)

param(
    [string]$ImageName = "mocherqu/devtechly:latest",
    [string]$Domain = "devtechly.com",
    [switch]$UseHttps = $false
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Deploiement avec Nom de Domaine" -ForegroundColor Cyan
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

Write-Host "[*] Configuration:" -ForegroundColor Cyan
Write-Host "   Image: $ImageName" -ForegroundColor White
Write-Host "   Domaine: $Domain" -ForegroundColor White
Write-Host "   HTTPS: $UseHttps" -ForegroundColor White
Write-Host ""

# Vérifier si .env.prod existe
$envFile = ".env.prod"
if (-not (Test-Path $envFile)) {
    Write-Host "[!] Fichier .env.prod non trouve" -ForegroundColor Yellow
    Write-Host "   Le script deploy-from-registry.ps1 va le creer automatiquement" -ForegroundColor Yellow
    Write-Host ""
}

# Vérifier que les fichiers Nginx existent
if (-not (Test-Path "nginx\nginx.conf")) {
    Write-Host "[ERREUR] Fichier nginx\nginx.conf non trouve!" -ForegroundColor Red
    Write-Host "   Assurez-vous que les fichiers Nginx sont presents" -ForegroundColor Yellow
    exit 1
}

if (-not (Test-Path "nginx\conf.d\devtechly.conf")) {
    Write-Host "[ERREUR] Fichier nginx\conf.d\devtechly.conf non trouve!" -ForegroundColor Red
    Write-Host "   Assurez-vous que les fichiers Nginx sont presents" -ForegroundColor Yellow
    exit 1
}

# Mettre à jour le fichier docker-compose avec l'image spécifiée
$composeFile = "docker-compose.prod-with-domain.yml"
Write-Host "[*] Verification du fichier docker-compose..." -ForegroundColor Cyan

# Vérifier que le fichier existe
if (-not (Test-Path $composeFile)) {
    Write-Host "[ERREUR] Fichier $composeFile non trouve!" -ForegroundColor Red
    exit 1
}

# Mettre à jour l'image dans le fichier docker-compose si nécessaire
$composeContent = Get-Content $composeFile -Raw
if ($composeContent -notmatch "image:\s*$([regex]::Escape($ImageName))") {
    Write-Host "[*] Mise a jour de l'image dans docker-compose..." -ForegroundColor Cyan
    $composeContent = $composeContent -replace 'image:\s*mocherqu/devtechly:latest', "image: $ImageName"
    $composeContent | Set-Content $composeFile -NoNewline
    Write-Host "[OK] Image mise a jour: $ImageName" -ForegroundColor Green
}

Write-Host ""

# Pull de l'image depuis le registre
Write-Host "[*] Telechargement de l'image depuis le registre..." -ForegroundColor Cyan
Write-Host "   Image: $ImageName" -ForegroundColor White

try {
    docker pull $ImageName
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERREUR] Echec du telechargement de l'image" -ForegroundColor Red
        Write-Host "   Verifiez que vous etes connecte au registre" -ForegroundColor Yellow
        exit 1
    }
    Write-Host "[OK] Image telechargee avec succes" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "[ERREUR] Erreur lors du telechargement: $_" -ForegroundColor Red
    exit 1
}

# Arrêter les conteneurs existants
Write-Host "[*] Arret des conteneurs existants..." -ForegroundColor Cyan
docker compose -f $composeFile down 2>$null

# Démarrer les services
Write-Host "[*] Demarrage des services avec domaine..." -ForegroundColor Cyan

# Charger les variables d'environnement depuis .env.prod
if (Test-Path ".env.prod") {
    Get-Content ".env.prod" | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]*)\s*=\s*(.*)$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim()
            [Environment]::SetEnvironmentVariable($key, $value, "Process")
        }
    }
}

try {
    docker compose -f $composeFile --env-file .env.prod up -d
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Services demarres avec succes" -ForegroundColor Green
        Write-Host ""
        Write-Host "[*] Statut des services:" -ForegroundColor Cyan
        docker compose -f $composeFile ps
        Write-Host ""
        
        if ($UseHttps) {
            Write-Host "[OK] L'application devrait etre accessible sur:" -ForegroundColor Green
            Write-Host "   https://$Domain" -ForegroundColor White
        } else {
            Write-Host "[OK] L'application devrait etre accessible sur:" -ForegroundColor Green
            Write-Host "   http://$Domain" -ForegroundColor White
        }
        
        Write-Host ""
        Write-Host "[*] Pour voir les logs:" -ForegroundColor Cyan
        Write-Host "   docker compose -f $composeFile logs -f" -ForegroundColor White
        Write-Host ""
        Write-Host "[!] IMPORTANT:" -ForegroundColor Yellow
        Write-Host "   - Verifiez que votre DNS pointe vers ce serveur" -ForegroundColor Yellow
        Write-Host "   - Verifiez que les ports 80 et 443 sont ouverts" -ForegroundColor Yellow
        if (-not $UseHttps) {
            Write-Host "   - Pour activer HTTPS, configurez les certificats SSL" -ForegroundColor Yellow
        }
    } else {
        Write-Host "[ERREUR] Erreur lors du demarrage des services" -ForegroundColor Red
        Write-Host "   Verifiez les logs avec: docker compose -f $composeFile logs" -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host "[ERREUR] Erreur lors du demarrage: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "[OK] Deploiement termine avec succes!" -ForegroundColor Green
Write-Host ""
