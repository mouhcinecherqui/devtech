#!/usr/bin/env pwsh
# Script de déploiement depuis un registre Docker

param(
    [string]$ImageName = "",          # Nom complet de l'image (ex: username/devtechly:latest)
    [string]$Registry = "docker.io",  # docker.io, ghcr.io, ou votre registre privé
    [string]$Username = "",           # Votre nom d'utilisateur du registre
    [string]$AppImageName = "devtechly", # Nom de l'image dans le registre
    [string]$Tag = "latest",          # Tag de l'image
    [switch]$PullOnly = $false        # Seulement pull l'image, ne pas démarrer les services
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Deploiement depuis Registry Docker" -ForegroundColor Cyan
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
if ([string]::IsNullOrEmpty($ImageName)) {
    if ($Registry -eq "docker.io") {
        if ([string]::IsNullOrEmpty($Username)) {
            Write-Host "[ERREUR] Username requis pour Docker Hub" -ForegroundColor Red
            Write-Host "   Utilisation: .\deploy-from-registry.ps1 -Username votre-username" -ForegroundColor Yellow
            exit 1
        }
        $FullImageName = "${Username}/${AppImageName}:${Tag}"
    } elseif ($Registry -eq "ghcr.io") {
        if ([string]::IsNullOrEmpty($Username)) {
            Write-Host "[ERREUR] Username requis pour GitHub Container Registry" -ForegroundColor Red
            Write-Host "   Utilisation: .\deploy-from-registry.ps1 -Registry ghcr.io -Username votre-username" -ForegroundColor Yellow
            exit 1
        }
        $FullImageName = "${Registry}/${Username}/${AppImageName}:${Tag}"
    } else {
        # Registre privé
        if ([string]::IsNullOrEmpty($Username)) {
            $FullImageName = "${Registry}/${AppImageName}:${Tag}"
        } else {
            $FullImageName = "${Registry}/${Username}/${AppImageName}:${Tag}"
        }
    }
} else {
    $FullImageName = $ImageName
}

Write-Host "[*] Configuration:" -ForegroundColor Cyan
Write-Host "   Image: $FullImageName" -ForegroundColor White
Write-Host ""

# Vérifier si .env.prod existe
$envFile = ".env.prod"
if (-not (Test-Path $envFile)) {
    Write-Host "[!] Fichier .env.prod non trouve" -ForegroundColor Yellow
    Write-Host "   Creation du fichier .env.prod..." -ForegroundColor Cyan
    
    # Générer un secret JWT sécurisé
    $jwtSecret = ""
    try {
        $jwtSecret = (openssl rand -base64 64) -replace "`n|`r", ""
    } catch {
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
    $envContent += "# Genere automatiquement le " + (Get-Date -Format "yyyy-MM-dd HH:mm:ss") + "`n"
    $envContent += "`n# Profil Spring`n"
    $envContent += "SPRING_PROFILES_ACTIVE=prod`n"
    $envContent += "`n# Base de donnees MySQL (conteneur)`n"
    $envContent += "MYSQL_ROOT_PASSWORD=$rootPassword`n"
    $envContent += "MYSQL_DATABASE=devtechly`n"
    $envContent += "MYSQL_USER=devtechly`n"
    $envContent += "MYSQL_PASSWORD=$dbPassword`n"
    $envContent += "`n# Application - Base de donnees`n"
    $envContent += "DB_URL=$dbUrl`n"
    $envContent += "DB_USER=devtechly`n"
    $envContent += "DB_PASSWORD=$dbPassword`n"
    $envContent += "`n# JWT Secret`n"
    $envContent += "JWT_SECRET=$jwtSecret`n"
    $envContent += "`n# Mail (Gmail SMTP) - a configurer`n"
    $envContent += "SPRING_MAIL_HOST=smtp.gmail.com`n"
    $envContent += "SPRING_MAIL_PORT=587`n"
    $envContent += "SPRING_MAIL_USERNAME=votre-email@gmail.com`n"
    $envContent += "SPRING_MAIL_PASSWORD=votre-mot-de-passe-app`n"
    $envContent += "SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true`n"
    $envContent += "SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true`n"
    $envContent += "`n# OAuth2 Google - a configurer`n"
    $envContent += "GOOGLE_CLIENT_ID=votre_client_id.apps.googleusercontent.com`n"
    $envContent += "GOOGLE_CLIENT_SECRET=votre_client_secret`n"
    $envContent += "`n# URL de base`n"
    $envContent += "JHIPSTER_MAIL_BASE_URL=https://devtechly.com`n"
    $envContent += "`n# Configuration Java`n"
    $envContent += "JAVA_OPTS=-Xmx1g -Xms512m -XX:+UseG1GC -XX:+UseContainerSupport`n"
    
    $envContent | Out-File -FilePath $envFile -Encoding UTF8
    Write-Host "[OK] Fichier .env.prod cree" -ForegroundColor Green
    Write-Host "   [!] IMPORTANT: Modifiez EMAIL et OAuth2 Google dans .env.prod!" -ForegroundColor Yellow
    Write-Host ""
}

# Créer un fichier docker-compose temporaire avec l'image du registre
$composeFile = "docker-compose.registry.yml"
Write-Host "[*] Creation du fichier docker-compose avec l'image du registre..." -ForegroundColor Cyan

$composeContent = @"
name: devtechly-prod
services:
  devtechly-app:
    image: $FullImageName
    ports:
      - '8080:8080'
    env_file:
      - .env.prod
    environment:
      - SPRING_PROFILES_ACTIVE=`${SPRING_PROFILES_ACTIVE:-prod}
      - DB_URL=`${DB_URL}
      - DB_USER=`${DB_USER}
      - DB_PASSWORD=`${DB_PASSWORD}
      - JWT_SECRET=`${JWT_SECRET}
      - GOOGLE_CLIENT_ID=`${GOOGLE_CLIENT_ID}
      - GOOGLE_CLIENT_SECRET=`${GOOGLE_CLIENT_SECRET}
      - SPRING_MAIL_USERNAME=`${SPRING_MAIL_USERNAME}
      - SPRING_MAIL_PASSWORD=`${SPRING_MAIL_PASSWORD}
      - JHIPSTER_MAIL_BASE_URL=`${JHIPSTER_MAIL_BASE_URL:-https://devtechly.com}
    depends_on:
      mysql:
        condition: service_started
    restart: unless-stopped
  mysql:
    image: mysql:8.0.32
    volumes:
      - ./src/main/docker/config/mysql:/etc/mysql/conf.d
      - prod-db-data:/var/lib/mysql
    env_file:
      - .env.prod
    environment:
      - MYSQL_ROOT_PASSWORD=`${MYSQL_ROOT_PASSWORD}
      - MYSQL_USER=`${MYSQL_USER:-devtechly}
      - MYSQL_PASSWORD=`${MYSQL_PASSWORD}
      - MYSQL_DATABASE=`${MYSQL_DATABASE:-devtechly}
    command: mysqld --lower_case_table_names=1 --skip-mysqlx --character_set_server=utf8mb4 --explicit_defaults_for_timestamp
    healthcheck:
      test: ['CMD-SHELL', 'mysql -e "SHOW DATABASES;" && sleep 5']
      interval: 5s
      timeout: 10s
      retries: 10
volumes:
  prod-db-data:
"@

$composeContent | Out-File -FilePath $composeFile -Encoding UTF8
Write-Host "[OK] Fichier $composeFile cree" -ForegroundColor Green
Write-Host ""

# Pull de l'image depuis le registre
Write-Host "[*] Telechargement de l'image depuis le registre..." -ForegroundColor Cyan
Write-Host "   Image: $FullImageName" -ForegroundColor White

# Vérifier si l'utilisateur doit se connecter
if ($Registry -ne "docker.io" -or $Registry -ne "ghcr.io") {
    Write-Host "   Assurez-vous d'etre connecte au registre:" -ForegroundColor Yellow
    if ($Registry -eq "ghcr.io") {
        Write-Host "   echo `$env:GITHUB_TOKEN | docker login ghcr.io -u $Username --password-stdin" -ForegroundColor White
    } else {
        Write-Host "   docker login $Registry" -ForegroundColor White
    }
    Write-Host ""
}

try {
    docker pull $FullImageName
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERREUR] Echec du telechargement de l'image" -ForegroundColor Red
        Write-Host "   Verifiez que vous etes connecte au registre et que l'image existe" -ForegroundColor Yellow
        exit 1
    }
    Write-Host "[OK] Image telechargee avec succes" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "[ERREUR] Erreur lors du telechargement: $_" -ForegroundColor Red
    exit 1
}

if ($PullOnly) {
    Write-Host "[OK] Image telechargee. Utilisez docker-compose pour demarrer les services." -ForegroundColor Green
    Write-Host "   docker compose -f $composeFile --env-file .env.prod up -d" -ForegroundColor White
    exit 0
}

# Arrêter les conteneurs existants
Write-Host "[*] Arret des conteneurs existants..." -ForegroundColor Cyan
docker compose -f $composeFile down 2>$null

# Démarrer les services
Write-Host "[*] Demarrage des services..." -ForegroundColor Cyan

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
        Write-Host "[OK] L'application devrait etre accessible sur http://localhost:8080" -ForegroundColor Green
        Write-Host ""
        Write-Host "[*] Pour voir les logs:" -ForegroundColor Cyan
        Write-Host "   docker compose -f $composeFile logs -f" -ForegroundColor White
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
