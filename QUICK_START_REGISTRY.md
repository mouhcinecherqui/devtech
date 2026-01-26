# 🚀 Quick Start - Déploiement avec Registry

## Démarrage rapide

### 1. Build et Push vers Docker Hub

```powershell
# Se connecter à Docker Hub (une seule fois)
docker login

# Build et push de l'image
.\build-and-push.ps1 -Username mocherqu
```

### 2. Déployer depuis le registre

```powershell
# Sur votre serveur de production
.\deploy-from-registry.ps1 -Username mocherqu
```

## Avec GitHub Container Registry

```powershell
# 1. Créer un token GitHub (Settings → Developer settings → Personal access tokens)
#    Permissions: write:packages, read:packages

# 2. Se connecter
$env:GITHUB_TOKEN = "votre-token"
echo $env:GITHUB_TOKEN | docker login ghcr.io -u votre-username-github --password-stdin

# 3. Build et push
.\build-and-push.ps1 -Registry ghcr.io -Username votre-username-github

# 4. Déployer
.\deploy-from-registry.ps1 -Registry ghcr.io -Username votre-username-github
```

## Fichiers créés

- ✅ `build-and-push.ps1` / `build-and-push.sh` - Build et push vers le registre
- ✅ `deploy-from-registry.ps1` / `deploy-from-registry.sh` - Déploiement depuis le registre
- ✅ `DEPLOIEMENT_REGISTRY.md` - Guide complet détaillé

## Documentation complète

Voir `DEPLOIEMENT_REGISTRY.md` pour le guide complet avec toutes les options.
