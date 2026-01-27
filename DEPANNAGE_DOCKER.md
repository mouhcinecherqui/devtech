# 🔧 Dépannage Docker Desktop

## ❌ Erreur : "The system cannot find the file specified"

Cette erreur signifie que Docker Desktop n'est pas complètement démarré ou n'est pas accessible.

### ✅ Solutions

#### 1. Vérifier que Docker Desktop est démarré

1. **Ouvrir Docker Desktop** depuis le menu Démarrer
2. **Attendre** que l'icône Docker dans la barre des tâches soit verte (pas orange/rouge)
3. **Vérifier** qu'il n'y a pas de message d'erreur dans Docker Desktop

#### 2. Redémarrer Docker Desktop

1. **Fermer** Docker Desktop complètement (clic droit sur l'icône → Quit)
2. **Attendre** 10-15 secondes
3. **Rouvrir** Docker Desktop depuis le menu Démarrer
4. **Attendre** que Docker soit complètement démarré (icône verte)

#### 3. Vérifier que Docker fonctionne

Ouvrez PowerShell et testez :

```powershell
# Test 1 : Vérifier la version
docker --version

# Test 2 : Vérifier que le daemon répond
docker ps

# Test 3 : Vérifier les informations Docker
docker info
```

Si toutes ces commandes fonctionnent **sans erreur**, Docker est prêt !

#### 4. Redémarrer le service Docker (si nécessaire)

Si Docker Desktop ne démarre pas :

1. Ouvrir **Services** (Win + R → `services.msc`)
2. Chercher **Docker Desktop Service**
3. Clic droit → **Redémarrer**

#### 5. Vérifier les ressources système

Docker Desktop nécessite :

- ✅ Au moins 4 GB de RAM disponible
- ✅ Virtualisation activée dans le BIOS
- ✅ WSL 2 installé (pour Windows)

### 🔍 Vérification rapide

Exécutez cette commande pour vérifier que Docker est prêt :

```powershell
docker run hello-world
```

Si cette commande fonctionne, Docker est opérationnel !

### ⚠️ Si Docker Desktop ne démarre toujours pas

1. **Vérifier les logs** : Docker Desktop → Settings → Troubleshoot → View logs
2. **Redémarrer Windows** (parfois nécessaire)
3. **Réinstaller Docker Desktop** (dernier recours)

### ✅ Une fois Docker prêt

Relancez le build :

```powershell
.\build-and-push.ps1 -Username mocherqu
```
