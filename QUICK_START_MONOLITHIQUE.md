# ⚡ Quick Start - Déploiement Monolithique

Guide ultra-rapide pour déployer devtechly avec Docker Hub (utilisateur: **mocherqu**)

## 🚀 En 3 commandes

### 1. Se connecter à Docker Hub

```powershell
docker login
# Username: mocherqu
# Password: [votre mot de passe]
```

### 2. Build et Push

```powershell
.\build-and-push.ps1 -Username mocherqu
```

⏱️ **Temps :** 10-15 minutes

### 3. Déployer

```powershell
.\deploy-from-registry.ps1 -Username mocherqu
```

✅ **Application accessible sur :** http://localhost:8080

---

## 📦 Fichiers utilisés

- `Dockerfile.jhipster` - Build monolithique (Backend + Frontend)
- `docker-compose.prod.yml` - Configuration Docker Compose
- `build-and-push.ps1` - Script build/push
- `deploy-from-registry.ps1` - Script déploiement

---

## 🔍 Vérification rapide

```powershell
# Vérifier les services
docker compose -f docker-compose.registry.yml ps

# Voir les logs
docker compose -f docker-compose.registry.yml logs -f

# Tester l'application
curl http://localhost:8080/management/health
```

---

## 📚 Documentation complète

- `GUIDE_DEPLOIEMENT_MONOLITHIQUE.md` - Guide détaillé
- `COMMANDES_DOCKER_HUB.md` - Toutes les commandes
- `DEPLOIEMENT_REGISTRY.md` - Guide complet du registre

---

**Image Docker Hub :** `mocherqu/devtechly:latest` 🐳
