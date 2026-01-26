# 🐳 Commandes Docker Hub - mocherqu

Commandes prêtes à l'emploi pour déployer avec Docker Hub (utilisateur: **mocherqu**)

## 🔐 Connexion (une seule fois)

```powershell
docker login
# Entrez votre nom d'utilisateur: mocherqu
# Entrez votre mot de passe Docker Hub
```

## 🏗️ Build et Push

### Build et push vers Docker Hub

```powershell
.\build-and-push.ps1 -Username mocherqu
```

### Build et push avec un tag de version

```powershell
.\build-and-push.ps1 -Username mocherqu -Tag v1.0.0
```

### Build seulement (sans push)

```powershell
.\build-and-push.ps1 -Username mocherqu -SkipPush
```

### Push seulement (si l'image existe déjà)

```powershell
.\build-and-push.ps1 -Username mocherqu -SkipBuild
```

## 🚀 Déploiement

### Déployer depuis Docker Hub

```powershell
.\deploy-from-registry.ps1 -Username mocherqu
```

### Déployer une version spécifique

```powershell
.\deploy-from-registry.ps1 -Username mocherqu -Tag v1.0.0
```

### Déployer avec le nom complet de l'image

```powershell
.\deploy-from-registry.ps1 -ImageName mocherqu/devtechly:latest
```

### Pull seulement (sans démarrer les services)

```powershell
.\deploy-from-registry.ps1 -Username mocherqu -PullOnly
```

## 📦 Commandes Docker manuelles

### Build de l'image

```bash
docker build -f Dockerfile.jhipster -t mocherqu/devtechly:latest .
```

### Push vers Docker Hub

```bash
docker push mocherqu/devtechly:latest
```

### Pull depuis Docker Hub

```bash
docker pull mocherqu/devtechly:latest
```

### Vérifier l'image dans Docker Hub

Visitez: https://hub.docker.com/r/mocherqu/devtechly

## 🔄 Workflow complet

### 1. Sur votre machine de développement

```powershell
# Build et push
.\build-and-push.ps1 -Username mocherqu -Tag v1.0.0
```

### 2. Sur le serveur de production

```powershell
# Déployer
.\deploy-from-registry.ps1 -Username mocherqu -Tag v1.0.0
```

## 📝 Notes

- L'image sera disponible sur: `mocherqu/devtechly:latest`
- Pour voir toutes les versions: https://hub.docker.com/r/mocherqu/devtechly/tags
- Le script crée automatiquement `docker-compose.registry.yml` avec l'image du registre
