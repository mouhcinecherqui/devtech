# 🐳 Guide de Déploiement avec Docker Registry

Ce guide explique comment déployer votre application devtechly en utilisant un registre Docker (Docker Hub, GitHub Container Registry, ou un registre privé).

## 📋 Table des matières

1. [Vue d'ensemble](#vue-densemble)
2. [Configuration du registre](#configuration-du-registre)
3. [Build et Push de l'image](#build-et-push-de-limage)
4. [Déploiement depuis le registre](#déploiement-depuis-le-registre)
5. [Workflow complet](#workflow-complet)

---

## 🎯 Vue d'ensemble

Le déploiement avec un registre Docker permet de :

- ✅ Séparer la construction de l'image du déploiement
- ✅ Réutiliser la même image sur plusieurs serveurs
- ✅ Faciliter les mises à jour (pull de la nouvelle version)
- ✅ Améliorer la sécurité (images signées et vérifiées)
- ✅ Accélérer les déploiements (pas besoin de rebuild sur chaque serveur)

### Architecture

```
[CI/CD ou Machine de dev]
    ↓ (build & push)
[Registry Docker]
    ↓ (pull)
[Serveur de production]
```

---

## 🔧 Configuration du registre

### Option 1 : Docker Hub (Recommandé pour débuter)

Docker Hub est le registre public le plus populaire.

#### 1. Créer un compte

1. Allez sur [hub.docker.com](https://hub.docker.com)
2. Créez un compte gratuit
3. Notez votre nom d'utilisateur

#### 2. Se connecter

```powershell
# Windows PowerShell
docker login

# Linux/Mac
docker login
```

Entrez votre nom d'utilisateur et mot de passe Docker Hub.

#### 3. Utilisation

```powershell
# Build et push vers Docker Hub
.\build-and-push.ps1 -Username mocherqu

# Déployer depuis Docker Hub
.\deploy-from-registry.ps1 -Username mocherqu
```

---

### Option 2 : GitHub Container Registry (ghcr.io)

GitHub Container Registry est intégré à GitHub et gratuit pour les projets open source.

#### 1. Créer un Personal Access Token (PAT)

1. Allez sur GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Créez un nouveau token avec les permissions :
   - `write:packages` (pour push)
   - `read:packages` (pour pull)
3. Copiez le token (il ne sera affiché qu'une fois)

#### 2. Se connecter

```powershell
# Windows PowerShell
$env:GITHUB_TOKEN = "votre-token-github"
echo $env:GITHUB_TOKEN | docker login ghcr.io -u votre-username --password-stdin

# Linux/Mac
echo $GITHUB_TOKEN | docker login ghcr.io -u votre-username --password-stdin
```

#### 3. Utilisation

```powershell
# Build et push vers GitHub Container Registry
.\build-and-push.ps1 -Registry ghcr.io -Username votre-username

# Déployer depuis GitHub Container Registry
.\deploy-from-registry.ps1 -Registry ghcr.io -Username votre-username
```

---

### Option 3 : Registre privé

Pour un registre privé (ex: Harbor, GitLab Container Registry, AWS ECR, Azure Container Registry).

#### 1. Se connecter

```powershell
# Windows PowerShell
docker login registry.example.com

# Linux/Mac
docker login registry.example.com
```

#### 2. Utilisation

```powershell
# Build et push vers un registre privé
.\build-and-push.ps1 -Registry registry.example.com -Username votre-username

# Déployer depuis un registre privé
.\deploy-from-registry.ps1 -Registry registry.example.com -Username votre-username
```

---

## 🏗️ Build et Push de l'image

### Méthode 1 : Script PowerShell (Windows)

```powershell
# Build et push vers Docker Hub
.\build-and-push.ps1 -Username mocherqu

# Build et push vers GitHub Container Registry
.\build-and-push.ps1 -Registry ghcr.io -Username votre-username

# Build seulement (sans push)
.\build-and-push.ps1 -Username mocherqu -SkipPush

# Push seulement (si l'image existe déjà)
.\build-and-push.ps1 -Username mocherqu -SkipBuild
```

### Méthode 2 : Script Bash (Linux/Mac)

```bash
# Rendre le script exécutable
chmod +x build-and-push.sh

# Build et push vers Docker Hub
./build-and-push.sh --username votre-username

# Build et push vers GitHub Container Registry
./build-and-push.sh --registry ghcr.io --username votre-username

# Build seulement (sans push)
./build-and-push.sh --username votre-username --skip-push

# Push seulement (si l'image existe déjà)
./build-and-push.sh --username votre-username --skip-build
```

### Méthode 3 : Commandes Docker manuelles

```bash
# 1. Build de l'image
docker build -f Dockerfile.jhipster -t mocherqu/devtechly:latest .

# 2. Tag pour le registre (si nécessaire)
docker tag devtechly:latest mocherqu/devtechly:latest

# 3. Push vers le registre
docker push mocherqu/devtechly:latest
```

### Tags et versions

Il est recommandé d'utiliser des tags de version pour faciliter la gestion :

```powershell
# Build avec un tag de version
.\build-and-push.ps1 -Username mocherqu -Tag v1.0.0

# Build avec plusieurs tags
docker build -f Dockerfile.jhipster -t mocherqu/devtechly:v1.0.0 -t mocherqu/devtechly:latest .
docker push mocherqu/devtechly:v1.0.0
docker push mocherqu/devtechly:latest
```

---

## 🚀 Déploiement depuis le registre

### Méthode 1 : Script PowerShell (Windows)

```powershell
# Déployer depuis Docker Hub
.\deploy-from-registry.ps1 -Username mocherqu

# Déployer depuis GitHub Container Registry
.\deploy-from-registry.ps1 -Registry ghcr.io -Username votre-username

# Déployer une version spécifique
.\deploy-from-registry.ps1 -Username mocherqu -Tag v1.0.0

# Déployer avec le nom complet de l'image
.\deploy-from-registry.ps1 -ImageName mocherqu/devtechly:v1.0.0

# Pull seulement (sans démarrer les services)
.\deploy-from-registry.ps1 -Username mocherqu -PullOnly
```

### Méthode 2 : Script Bash (Linux/Mac)

```bash
# Rendre le script exécutable
chmod +x deploy-from-registry.sh

# Déployer depuis Docker Hub
./deploy-from-registry.sh --username votre-username

# Déployer depuis GitHub Container Registry
./deploy-from-registry.sh --registry ghcr.io --username votre-username

# Déployer une version spécifique
./deploy-from-registry.sh --username votre-username --tag v1.0.0

# Déployer avec le nom complet de l'image
./deploy-from-registry.sh --image-name votre-username/devtechly:v1.0.0

# Pull seulement (sans démarrer les services)
./deploy-from-registry.sh --username votre-username --pull-only
```

### Méthode 3 : Docker Compose manuel

Le script `deploy-from-registry.ps1` crée automatiquement un fichier `docker-compose.registry.yml`. Vous pouvez aussi le créer manuellement :

```yaml
name: devtechly-prod
services:
  devtechly-app:
    image: mocherqu/devtechly:latest # Image du registre
    ports:
      - '8080:8080'
    env_file:
      - .env.prod
    # ... reste de la configuration
```

Puis :

```bash
# Pull de l'image
docker pull mocherqu/devtechly:latest

# Démarrer les services
docker compose -f docker-compose.registry.yml --env-file .env.prod up -d
```

---

## 🔄 Workflow complet

### Scénario 1 : Développement local → Production

```powershell
# 1. Sur votre machine de développement
#    Build et push de l'image
.\build-and-push.ps1 -Username mocherqu -Tag v1.0.0

# 2. Sur le serveur de production
#    Pull et déploiement
.\deploy-from-registry.ps1 -Username mocherqu -Tag v1.0.0
```

### Scénario 2 : CI/CD avec GitHub Actions

Créez `.github/workflows/docker-publish.yml` :

```yaml
name: Build and Push Docker Image

on:
  push:
    tags:
      - 'v*'

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v2

      - name: Login to Docker Hub
        uses: docker/login-action@v2
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}

      - name: Build and push
        uses: docker/build-push-action@v4
        with:
          context: .
          file: ./Dockerfile.jhipster
          push: true
          tags: |
            ${{ secrets.DOCKER_USERNAME }}/devtechly:${{ github.ref_name }}
            ${{ secrets.DOCKER_USERNAME }}/devtechly:latest
```

### Scénario 3 : Mise à jour de l'application

```powershell
# 1. Build et push de la nouvelle version
.\build-and-push.ps1 -Username mocherqu -Tag v1.1.0

# 2. Sur le serveur de production
#    Arrêter l'ancienne version
docker compose -f docker-compose.registry.yml down

#    Pull de la nouvelle version
docker pull mocherqu/devtechly:v1.1.0

#    Mettre à jour le docker-compose.registry.yml avec le nouveau tag
#    (ou utiliser le script)
.\deploy-from-registry.ps1 -Username mocherqu -Tag v1.1.0
```

---

## 📝 Fichiers créés

Les scripts créent automatiquement :

- `docker-compose.registry.yml` : Configuration Docker Compose utilisant l'image du registre
- `.env.prod` : Variables d'environnement (si n'existe pas)

---

## 🔍 Vérification

### Vérifier que l'image est dans le registre

```bash
# Docker Hub
# Visitez: https://hub.docker.com/r/mocherqu/devtechly

# GitHub Container Registry
# Visitez: https://github.com/votre-username?tab=packages

# Ou via Docker
docker search mocherqu/devtechly  # Docker Hub seulement
```

### Vérifier le déploiement

```bash
# Statut des services
docker compose -f docker-compose.registry.yml ps

# Logs
docker compose -f docker-compose.registry.yml logs -f

# Santé de l'application
curl http://localhost:8080/management/health
```

---

## 🛠️ Dépannage

### Erreur : "unauthorized: authentication required"

**Solution** : Connectez-vous au registre

```bash
docker login  # Pour Docker Hub
# ou
docker login ghcr.io  # Pour GitHub Container Registry
```

### Erreur : "pull access denied"

**Solution** : Vérifiez que :

- L'image existe dans le registre
- Vous avez les permissions nécessaires
- Le nom de l'image est correct

### Erreur : "manifest unknown"

**Solution** : Vérifiez que le tag existe

```bash
# Lister les tags disponibles
docker images mocherqu/devtechly
```

### Image trop volumineuse

**Solution** : Optimisez le Dockerfile ou utilisez un registre avec plus d'espace.

---

## 🔐 Sécurité

### Bonnes pratiques

1. **Ne jamais commiter les credentials** : Utilisez des secrets/variables d'environnement
2. **Utiliser des tags de version** : Évitez `latest` en production
3. **Scanner les images** : Utilisez des outils comme Trivy ou Snyk
4. **Signer les images** : Utilisez Docker Content Trust
5. **Limiter les permissions** : Utilisez des tokens avec permissions minimales

### Exemple avec secrets

```powershell
# Windows PowerShell - Utiliser des variables d'environnement
$env:DOCKER_USERNAME = "mocherqu"
$env:DOCKER_PASSWORD = "votre-password"
docker login -u $env:DOCKER_USERNAME -p $env:DOCKER_PASSWORD

# Linux/Mac
export DOCKER_USERNAME="mocherqu"
export DOCKER_PASSWORD="votre-password"
echo $DOCKER_PASSWORD | docker login -u $DOCKER_USERNAME --password-stdin
```

---

## 📚 Ressources

- [Docker Hub Documentation](https://docs.docker.com/docker-hub/)
- [GitHub Container Registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)
- [Docker Registry](https://docs.docker.com/registry/)

---

## ✅ Checklist de déploiement

- [ ] Compte créé sur le registre (Docker Hub, GitHub, etc.)
- [ ] Connecté au registre (`docker login`)
- [ ] Image buildée et pushée (`build-and-push.ps1`)
- [ ] Image vérifiée dans le registre
- [ ] Fichier `.env.prod` configuré
- [ ] Image pullée sur le serveur (`deploy-from-registry.ps1`)
- [ ] Services démarrés et fonctionnels
- [ ] Application accessible et testée

---

**🎉 Félicitations ! Votre application est maintenant déployée avec un registre Docker !**
