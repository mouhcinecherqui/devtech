#!/usr/bin/env pwsh
# Script de déploiement en production pour devtechly

Write-Host "[*] Deploiement devtechly en production" -ForegroundColor Green
Write-Host ""

# Vérifier si Docker est disponible
$dockerAvailable = $false
try {
    docker --version | Out-Null
    $dockerAvailable = $true
    Write-Host "[OK] Docker detecte" -ForegroundColor Green
} catch {
    Write-Host "[!] Docker n'est pas disponible localement" -ForegroundColor Yellow
    Write-Host "   Le script preparera les fichiers pour un deploiement distant" -ForegroundColor Yellow
}

# Vérifier si .env.prod existe
$envFile = ".env.prod"
if (-not (Test-Path $envFile)) {
    Write-Host "[*] Creation du fichier .env.prod..." -ForegroundColor Cyan
    
    # Générer un secret JWT sécurisé
    $jwtSecret = ""
    try {
        # Utiliser OpenSSL si disponible (Linux/WSL)
        $jwtSecret = (openssl rand -base64 64) -replace "`n|`r", ""
    } catch {
        # Sinon générer avec PowerShell (méthode compatible)
        $bytes = New-Object byte[] 64
        $rng = New-Object System.Security.Cryptography.RNGCryptoServiceProvider
        $rng.GetBytes($bytes)
        $jwtSecret = [Convert]::ToBase64String($bytes)
        $rng.Dispose()
    }
    
    # Générer des mots de passe
    $dbPassword = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 32 | ForEach-Object {[char]$_})
    $rootPassword = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 32 | ForEach-Object {[char]$_})
    
    $dbUrl = 'jdbc:mysql://mysql:3306/devtechly?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
    
    $envContent = "# Configuration de production devtechly`n"
    $envContent += "# Généré automatiquement le " + (Get-Date -Format "yyyy-MM-dd HH:mm:ss") + "`n"
    $envContent += "`n# Profil Spring`n"
    $envContent += "SPRING_PROFILES_ACTIVE=prod`n"
    $envContent += "`n# Base de données MySQL (conteneur)`n"
    $envContent += "MYSQL_ROOT_PASSWORD=$rootPassword`n"
    $envContent += "MYSQL_DATABASE=devtechly`n"
    $envContent += "MYSQL_USER=devtechly`n"
    $envContent += "MYSQL_PASSWORD=$dbPassword`n"
    $envContent += "`n# Application - Base de données (DB_* référencés dans application-prod.yml)`n"
    $envContent += "DB_URL=$dbUrl`n"
    $envContent += "DB_USER=devtechly`n"
    $envContent += "DB_PASSWORD=$dbPassword`n"
    $envContent += "`n# JWT Secret (IMPORTANT: Changez-le en production)`n"
    $envContent += "JWT_SECRET=$jwtSecret`n"
    $envContent += "`n# Mail (Gmail SMTP) - à configurer`n"
    $envContent += "SPRING_MAIL_HOST=smtp.gmail.com`n"
    $envContent += "SPRING_MAIL_PORT=587`n"
    $envContent += "SPRING_MAIL_USERNAME=votre-email@gmail.com`n"
    $envContent += "SPRING_MAIL_PASSWORD=votre-mot-de-passe-app`n"
    $envContent += "SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true`n"
    $envContent += "SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true`n"
    $envContent += "`n# OAuth2 Google - à configurer`n"
    $envContent += "GOOGLE_CLIENT_ID=votre_client_id.apps.googleusercontent.com`n"
    $envContent += "GOOGLE_CLIENT_SECRET=votre_client_secret`n"
    $envContent += "`n# URL de base`n"
    $envContent += "JHIPSTER_MAIL_BASE_URL=https://devtechly.com`n"
    $envContent += "`n# Configuration Java`n"
    $envContent += "JAVA_OPTS=-Xmx1g -Xms512m -XX:+UseG1GC -XX:+UseContainerSupport`n"
    
    $envContent | Out-File -FilePath $envFile -Encoding UTF8
    Write-Host "[OK] Fichier .env.prod cree" -ForegroundColor Green
    Write-Host "   [!] IMPORTANT: Modifiez EMAIL et OAuth2 Google dans .env.prod avant deploiement!" -ForegroundColor Yellow
    Write-Host ""
}

Write-Host "[*] Configuration Docker Compose (env_file .env.prod, DB_*, JWT_SECRET, GOOGLE_CLIENT_*)" -ForegroundColor Cyan

if ($dockerAvailable) {
    Write-Host ""
    Write-Host "[*] Construction de l'image Docker..." -ForegroundColor Cyan
    
    try {
        docker build -f Dockerfile.jhipster -t devtechly:latest .
        if ($LASTEXITCODE -eq 0) {
            Write-Host "[OK] Image Docker construite avec succes" -ForegroundColor Green
        } else {
            Write-Host "[ERREUR] Erreur lors de la construction de l'image" -ForegroundColor Red
            exit 1
        }
    } catch {
        Write-Host "[ERREUR] Erreur lors de la construction: $_" -ForegroundColor Red
        exit 1
    }
    
    Write-Host ""
    Write-Host "[*] Demarrage des services en production..." -ForegroundColor Cyan
    
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
    
    # Arrêter les conteneurs existants s'ils existent
    docker compose -f docker-compose.prod.yml down 2>$null
    
    # Démarrer les services
    try {
        docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
        if ($LASTEXITCODE -eq 0) {
            Write-Host "[OK] Services demarres avec succes" -ForegroundColor Green
            Write-Host ""
            Write-Host "[*] Statut des services:" -ForegroundColor Cyan
            docker compose -f docker-compose.prod.yml ps
            Write-Host ""
            Write-Host "[*] Pour voir les logs:" -ForegroundColor Cyan
            Write-Host "   docker compose -f docker-compose.prod.yml logs -f" -ForegroundColor White
            Write-Host ""
            Write-Host "[OK] L'application devrait etre accessible sur http://localhost:8080" -ForegroundColor Green
        } else {
            Write-Host "[ERREUR] Erreur lors du demarrage des services" -ForegroundColor Red
            Write-Host "   Verifiez les logs avec: docker compose -f docker-compose.prod.yml logs" -ForegroundColor Yellow
            exit 1
        }
    } catch {
        Write-Host "[ERREUR] Erreur lors du demarrage: $_" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host ""
    Write-Host "[*] Preparation des fichiers pour deploiement distant..." -ForegroundColor Cyan
    
    # Créer un script de déploiement pour serveur Linux
    $deployScript = '#!/bin/bash' + "`n"
    $deployScript += '# Script de déploiement pour serveur Linux' + "`n"
    $deployScript += "`n"
    $deployScript += 'echo "Deploiement devtechly en production"' + "`n"
    $deployScript += "`n"
    $deployScript += '# Vérifier Docker' + "`n"
    $deployScript += 'if ! command -v docker 2> /dev/null; then' + "`n"
    $deployScript += '    echo "Docker nest pas installe"' + "`n"
    $deployScript += '    exit 1' + "`n"
    $deployScript += 'fi' + "`n"
    $deployScript += "`n"
    $deployScript += '# Construire l''image' + "`n"
    $deployScript += 'echo "Construction de l''image Docker..."' + "`n"
    $deployScript += 'docker build -f Dockerfile.jhipster -t devtechly:latest .' + "`n"
    $deployScript += "`n"
    $deployScript += 'if [ $? -ne 0 ]; then' + "`n"
    $deployScript += '    echo "Erreur lors de la construction"' + "`n"
    $deployScript += '    exit 1' + "`n"
    $deployScript += 'fi' + "`n"
    $deployScript += "`n"
    $deployScript += '# Arrêter les services existants' + "`n"
    $deployScript += 'docker compose -f docker-compose.prod.yml down' + "`n"
    $deployScript += "`n"
    $deployScript += '# Démarrer les services' + "`n"
    $deployScript += 'echo "Demarrage des services..."' + "`n"
    $deployScript += 'docker compose -f docker-compose.prod.yml --env-file .env.prod up -d' + "`n"
    $deployScript += "`n"
    $deployScript += 'if [ $? -eq 0 ]; then' + "`n"
    $deployScript += '    echo "Deploiement reussi!"' + "`n"
    $deployScript += '    echo "Statut:"' + "`n"
    $deployScript += '    docker compose -f docker-compose.prod.yml ps' + "`n"
    $deployScript += '    echo ""' + "`n"
    $deployScript += '    echo "Logs: docker compose -f docker-compose.prod.yml logs -f"' + "`n"
    $deployScript += 'else' + "`n"
    $deployScript += '    echo "Erreur lors du deploiement"' + "`n"
    $deployScript += '    exit 1' + "`n"
    $deployScript += 'fi' + "`n"
    
    $deployScript | Out-File -FilePath "deploy-prod.sh" -Encoding UTF8
    Write-Host "[OK] Script deploy-prod.sh cree" -ForegroundColor Green
    
    Write-Host ""
    Write-Host "[*] Instructions pour le deploiement sur serveur distant:" -ForegroundColor Cyan
    Write-Host "   1. Copiez les fichiers suivants sur le serveur:" -ForegroundColor White
    Write-Host "      - Dockerfile.jhipster" -ForegroundColor White
    Write-Host "      - docker-compose.prod.yml" -ForegroundColor White
    Write-Host "      - .env.prod" -ForegroundColor White
    Write-Host "      - deploy-prod.sh" -ForegroundColor White
    Write-Host "      - Tout le code source du projet" -ForegroundColor White
    Write-Host ""
    Write-Host "   2. Sur le serveur, exécutez:" -ForegroundColor White
    Write-Host "      chmod +x deploy-prod.sh" -ForegroundColor White
    Write-Host "      ./deploy-prod.sh" -ForegroundColor White
}

Write-Host ""
Write-Host "[OK] Deploiement prepare!" -ForegroundColor Green
Write-Host ""

