# Script pour tester le chargement des variables d'environnement depuis .env.prod

Write-Host "=== Test des variables d'environnement depuis .env.prod ===" -ForegroundColor Cyan
Write-Host ""

# Charger les variables depuis .env.prod avec dotenv-cli
$envVars = dotenv -e .env.prod -- powershell -Command "Get-ChildItem Env: | Where-Object { `$_.Name -like 'DB_*' -or `$_.Name -like 'SPRING_DATASOURCE_*' } | Format-Table Name, Value -AutoSize"

Write-Host "Variables chargées:" -ForegroundColor Green
Write-Host $envVars

Write-Host ""
Write-Host "Pour lancer l'application avec ces variables:" -ForegroundColor Yellow
Write-Host "  npm run backend:start:prod" -ForegroundColor White
Write-Host ""
Write-Host "Ou directement:" -ForegroundColor Yellow
Write-Host "  dotenv -e .env.prod -- ./mvnw -Pprod spring-boot:run" -ForegroundColor White
