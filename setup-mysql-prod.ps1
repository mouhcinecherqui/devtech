# Script PowerShell pour configurer MySQL pour le mode production
# Ce script cree l'utilisateur 'devtechly' et la base de donnees si necessaire
# Usage: .\setup-mysql-prod.ps1 [-MysqlPath <chemin>] [-RootPassword <motdepasse>]

param(
    [string]$MysqlPath = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
    [string]$RootPassword = ""
)

Write-Host "=== Configuration MySQL pour devtechly (mode production) ===" -ForegroundColor Cyan
Write-Host ""

# Si le mot de passe n'est pas fourni en parametre, essayer de le lire depuis une variable d'environnement
if ([string]::IsNullOrWhiteSpace($RootPassword)) {
    $RootPassword = $env:MYSQL_ROOT_PASSWORD
}

# Si toujours vide, le demander
if ([string]::IsNullOrWhiteSpace($RootPassword)) {
    Write-Host "Veuillez entrer les identifiants MySQL root pour creer l'utilisateur et la base de donnees:" -ForegroundColor Yellow
    try {
        $rootPasswordSecure = Read-Host "Mot de passe root MySQL" -AsSecureString
        $RootPassword = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($rootPasswordSecure))
    } catch {
        Write-Host "Erreur: Impossible de lire le mot de passe en mode interactif." -ForegroundColor Red
        Write-Host "Utilisez: `$env:MYSQL_ROOT_PASSWORD='votremotdepasse'; .\setup-mysql-prod.ps1" -ForegroundColor Yellow
        exit 1
    }
}

Write-Host ""
Write-Host "Creation de l'utilisateur 'devtechly' et de la base de donnees..." -ForegroundColor Green

# Executer le script SQL
$scriptPath = Join-Path $PSScriptRoot "setup-mysql-prod.sql"
$sqlScript = Get-Content -Path $scriptPath -Raw -Encoding UTF8

try {
    $env:MYSQL_PWD = $RootPassword
    $sqlScript | & $MysqlPath -u root -p"$RootPassword" 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "Configuration MySQL reussie!" -ForegroundColor Green
        Write-Host "  - Utilisateur: devtechly" -ForegroundColor Gray
        Write-Host "  - Base de donnees: devtechly" -ForegroundColor Gray
        Write-Host ""
        Write-Host "Vous pouvez maintenant lancer l'application avec:" -ForegroundColor Cyan
        Write-Host "  npm run backend:start:prod" -ForegroundColor White
    } else {
        Write-Host ""
        Write-Host "Erreur lors de la configuration MySQL" -ForegroundColor Red
        Write-Host "Verifiez que MySQL est en cours d'execution et que les identifiants root sont corrects." -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Alternative: Executez manuellement le script SQL:" -ForegroundColor Yellow
        Write-Host "  mysql -u root -p < setup-mysql-prod.sql" -ForegroundColor White
    }
} catch {
    Write-Host ""
    Write-Host "Erreur: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "Alternative: Executez manuellement le script SQL:" -ForegroundColor Yellow
    Write-Host "  mysql -u root -p < setup-mysql-prod.sql" -ForegroundColor White
} finally {
    Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
}
