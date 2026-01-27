# 🚀 Guide de Déploiement - Architecture Monolithique

Guide simplifié pour déployer votre application devtechly avec **une seule image Docker** (Backend + Frontend intégrés).

## ✅ Configuration actuelle

Votre application utilise déjà l'architecture monolithique :

- ✅ `Dockerfile.jhipster` - Construit l'image complète (Backend + Frontend)
- ✅ `docker-compose.prod.yml` - Configuration de déploiement
- ✅ Scripts de build et déploiement prêts

## 📋 Prérequis

1. **Docker installé et démarré**
2. **Compte Docker Hub** (utilisateur: `mocherqu`)
3. **Fichier `.env.prod`** configuré (créé automatiquement par les scripts)

## 🎯 Workflow en 3 étapes

### Étape 1 : Se connecter à Docker Hub (une seule fois)

```powershell
docker login
# Entrez votre nom d'utilisateur: mocherqu
# Entrez votre mot de passe Docker Hub
```

### Étape 2 : Build et Push de l'image

```powershell
# Build et push vers Docker Hub
.\build-and-push.ps1 -Username mocherqu
```

Cette commande va :

- ✅ Construire l'image Docker complète (Backend + Frontend)
- ✅ La tagger comme `mocherqu/devtechly:latest`
- ✅ La pousser vers Docker Hub

**Temps estimé :** 10-15 minutes (première fois)

### Étape 3 : Déployer l'application

#### Sur votre machine locale :

```powershell
# Déployer depuis Docker Hub
.\deploy-from-registry.ps1 -Username mocherqu
```

#### Sur un serveur de production :

```powershell
# 1. Copier les fichiers nécessaires sur le serveur :
#    - deploy-from-registry.ps1 (ou deploy-from-registry.sh pour Linux)
#    - docker-compose.prod.yml
#    - .env.prod

# 2. Se connecter à Docker Hub
docker login

# 3. Déployer
.\deploy-from-registry.ps1 -Username mocherqu
```

## 🌐 Accès à l'application

Une fois déployée, l'application est accessible sur :

- **URL :** `http://localhost:8080`
- **Backend API :** `http://localhost:8080/api`
- **Health Check :** `http://localhost:8080/management/health`

## 📝 Commandes utiles

### Vérifier le statut

```powershell
# Voir les conteneurs en cours d'exécution
docker compose -f docker-compose.registry.yml ps

# Voir les logs en temps réel
docker compose -f docker-compose.registry.yml logs -f

# Voir les logs de l'application uniquement
docker compose -f docker-compose.registry.yml logs -f devtechly-app
```

### Arrêter l'application

```powershell
docker compose -f docker-compose.registry.yml down
```

### Redémarrer l'application

```powershell
docker compose -f docker-compose.registry.yml restart
```

### Mettre à jour l'application

```powershell
# 1. Build et push de la nouvelle version
.\build-and-push.ps1 -Username mocherqu -Tag v1.1.0

# 2. Sur le serveur, arrêter l'ancienne version
docker compose -f docker-compose.registry.yml down

# 3. Déployer la nouvelle version
.\deploy-from-registry.ps1 -Username mocherqu -Tag v1.1.0
```

## 🔍 Vérification

### Vérifier que l'image est dans Docker Hub

Visitez : https://hub.docker.com/r/mocherqu/devtechly

### Vérifier que l'application fonctionne

```powershell
# Test de santé
curl http://localhost:8080/management/health

# Devrait retourner : {"status":"UP",...}
```

## ⚙️ Options avancées

### Build avec un tag de version

```powershell
.\build-and-push.ps1 -Username mocherqu -Tag v1.0.0
```

### Build seulement (sans push)

```powershell
.\build-and-push.ps1 -Username mocherqu -SkipPush
```

### Déployer une version spécifique

```powershell
.\deploy-from-registry.ps1 -Username mocherqu -Tag v1.0.0
```

## 🐛 Dépannage

### L'application ne démarre pas

```powershell
# Vérifier les logs
docker compose -f docker-compose.registry.yml logs devtechly-app

# Vérifier que MySQL est démarré
docker compose -f docker-compose.registry.yml ps mysql
```

### Erreur de connexion à la base de données

- Vérifiez que MySQL est démarré et healthy
- Vérifiez les variables `DB_URL`, `DB_USER`, `DB_PASSWORD` dans `.env.prod`
- Attendez quelques secondes (MySQL peut prendre du temps à démarrer)

### Erreur "unauthorized: authentication required"

```powershell
# Reconnectez-vous à Docker Hub
docker login
```

### Port 8080 déjà utilisé

Modifiez le port dans `docker-compose.registry.yml` :

```yaml
ports:
  - '8081:8080' # Utiliser le port 8081 au lieu de 8080
```

## 📊 Structure de l'image monolithique

```
mocherqu/devtechly:latest
├── Backend Spring Boot (API REST)
│   ├── Port 8080
│   └── Endpoints: /api/*
└── Frontend Angular (fichiers statiques)
    └── Servi par Spring Boot sur /
```

## ✅ Checklist de déploiement

- [ ] Docker installé et démarré
- [ ] Connecté à Docker Hub (`docker login`)
- [ ] Fichier `.env.prod` configuré (créé automatiquement)
- [ ] Image buildée et pushée (`build-and-push.ps1`)
- [ ] Image vérifiée dans Docker Hub
- [ ] Application déployée (`deploy-from-registry.ps1`)
- [ ] Services démarrés et fonctionnels
- [ ] Application accessible sur http://localhost:8080
- [ ] Health check OK (`/management/health`)

## 🎉 C'est tout !

Votre application devtechly est maintenant déployée avec une seule image Docker contenant le backend et le frontend.

**Image Docker Hub :** `mocherqu/devtechly:latest`

Pour toute question, consultez :

- `COMMANDES_DOCKER_HUB.md` - Toutes les commandes
- `DEPLOIEMENT_REGISTRY.md` - Guide complet du registre
- `ARCHITECTURE_DOCKER.md` - Comparaison des architectures
