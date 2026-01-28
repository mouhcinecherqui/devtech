# Script PowerShell simple pour configurer MySQL pour le mode production
# Usage: .\setup-mysql-simple.ps1 -RootPassword "votre_mot_de_passe_root"

param(
    [Parameter(Mandatory=$false)]
    [string]$RootPassword = "",
    
    [Parameter(Mandatory=$false)]
    [string]$MySQLPath = "mysql"
)

Write-Host "=== Configuration MySQL pour devtechly (mode production) ===" -ForegroundColor Cyan
Write-Host ""

# Si le mot de passe n'est pas fourni en paramètre, le demander
if ([string]::IsNullOrWhiteSpace($RootPassword)) {
    $securePassword = Read-Host "Mot de passe root MySQL" -AsSecureString
    $RootPassword = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword))
}

# Commandes SQL à exécuter
$sqlCommands = @"
CREATE USER IF NOT EXISTS 'devtechly'@'localhost' IDENTIFIED BY 'iNfc1gPa9HxSpYzbJ3m5sKAXZ8jTU7Fy';
CREATE DATABASE IF NOT EXISTS devtechly CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON devtechly.* TO 'devtechly'@'localhost';
FLUSH PRIVILEGES;
SELECT User, Host FROM mysql.user WHERE User = 'devtechly';
SHOW DATABASES LIKE 'devtechly';
"@

Write-Host "Création de l'utilisateur 'devtechly' et de la base de données..." -ForegroundColor Green
Write-Host ""

try {
    # Exécuter les commandes SQL
    $sqlCommands | & $MySQLPath -u root -p"$RootPassword" 2>&1
    
    if ($LASTEXITCODE -eq 0 -or $?) {
        Write-Host ""
        Write-Host "✓ Configuration MySQL réussie!" -ForegroundColor Green
        Write-Host "  - Utilisateur: devtechly" -ForegroundColor Gray
        Write-Host "  - Base de données: devtechly" -ForegroundColor Gray
        Write-Host ""
        Write-Host "Vous pouvez maintenant lancer l'application avec:" -ForegroundColor Cyan
        Write-Host "  npm run backend:start:prod" -ForegroundColor White
    } else {
        Write-Host ""
        Write-Host "✗ Erreur lors de la configuration MySQL" -ForegroundColor Red
        Write-Host "Vérifiez que MySQL est en cours d'exécution et que les identifiants root sont corrects." -ForegroundColor Yellow
    }
} catch {
    Write-Host ""
    Write-Host "✗ Erreur: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "Vérifiez que MySQL est installé et accessible." -ForegroundColor Yellow
    Write-Host "Vous pouvez aussi exécuter manuellement les commandes SQL dans MySQL." -ForegroundColor Yellow
}
