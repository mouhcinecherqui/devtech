# 🎯 Scénarios de Déploiement

Guide pour comprendre les différents scénarios de déploiement selon votre situation.

## 📊 Vue d'ensemble

| Scénario           | Machine               | URL              | Script                     | Utilisation        |
| ------------------ | --------------------- | ---------------- | -------------------------- | ------------------ |
| **1. Local**       | Votre PC Windows      | `localhost:8080` | `deploy-from-registry.ps1` | Développement/Test |
| **2. VPS/Serveur** | Serveur distant Linux | `devtechly.com`  | `deploy-with-domain.ps1`   | Production         |

---

## 🖥️ Scénario 1 : Déploiement Local (Votre PC Windows)

### Quand utiliser ?

- ✅ Test et développement
- ✅ Démonstration locale
- ✅ Pas besoin d'accès Internet

### Configuration

- **Machine :** Votre PC Windows
- **URL :** `http://localhost:8080`
- **Script :** `deploy-from-registry.ps1`
- **Fichier Docker Compose :** `docker-compose.prod.yml` ou `docker-compose.registry.yml`

### Commandes

```powershell
# 1. Build et push (sur votre PC)
.\build-and-push.ps1 -Username mocherqu

# 2. Déployer localement (sur votre PC)
.\deploy-from-registry.ps1 -Username mocherqu
```

### Résultat

- Application accessible sur `http://localhost:8080`
- Accessible uniquement depuis votre machine
- Pas de nom de domaine nécessaire

---

## 🌐 Scénario 2 : Déploiement sur VPS/Serveur (Production)

### Quand utiliser ?

- ✅ Production réelle
- ✅ Accès depuis Internet
- ✅ Utilisation d'un nom de domaine

### Configuration

- **Machine :** VPS, Cloud (AWS, Azure, OVH, etc.), ou serveur dédié
- **OS :** Linux (Ubuntu, Debian, CentOS, etc.)
- **URL :** `http://devtechly.com` ou `https://devtechly.com`
- **Script :** `deploy-with-domain.ps1` (ou équivalent bash)
- **Fichier Docker Compose :** `docker-compose.prod-with-domain.yml`

### Architecture

```
[Votre PC Windows]
    ↓ (build & push)
[Docker Hub]
    ↓ (pull)
[VPS/Serveur Linux]
    ↓ (docker compose)
[Application sur devtechly.com]
```

### Étapes détaillées

#### Étape 1 : Sur votre PC Windows (Développement)

```powershell
# Build et push de l'image vers Docker Hub
.\build-and-push.ps1 -Username mocherqu
```

#### Étape 2 : Sur votre VPS/Serveur Linux (Production)

**Cette étape est pour VPS/serveur distant !**

```bash
# 1. Se connecter au VPS via SSH
ssh utilisateur@votre-vps-ip

# 2. Créer le répertoire du projet
mkdir -p ~/devtech
cd ~/devtech

# 3. Copier les fichiers nécessaires depuis votre PC
# Option A : Via SCP (depuis votre PC Windows)
scp docker-compose.prod-with-domain.yml utilisateur@votre-vps-ip:~/devtech/
scp -r nginx/ utilisateur@votre-vps-ip:~/devtech/
scp .env.prod utilisateur@votre-vps-ip:~/devtech/

# Option B : Via Git (si le projet est sur Git)
git clone votre-repo
cd devtech

# Option C : Via FTP/SFTP (FileZilla, WinSCP, etc.)
```

**Fichiers nécessaires sur le VPS :**

```
~/devtech/
├── docker-compose.prod-with-domain.yml
├── .env.prod
└── nginx/
    ├── nginx.conf
    └── conf.d/
        └── devtechly.conf
```

#### Étape 3 : Installer Docker sur le VPS (si pas déjà installé)

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install -y docker.io docker-compose
sudo systemctl start docker
sudo systemctl enable docker

# Ajouter votre utilisateur au groupe docker
sudo usermod -aG docker $USER
# Déconnexion/reconnexion nécessaire
```

#### Étape 4 : Configurer DNS

Sur votre fournisseur de domaine (OVH, GoDaddy, etc.) :

```
Type    Name    Value           TTL
A       @       IP_DU_VPS       Auto
A       www     IP_DU_VPS       Auto
```

#### Étape 5 : Déployer sur le VPS

```bash
# Sur le VPS
cd ~/devtech

# Se connecter à Docker Hub
docker login

# Démarrer les services
docker compose -f docker-compose.prod-with-domain.yml --env-file .env.prod up -d

# Vérifier les logs
docker compose -f docker-compose.prod-with-domain.yml logs -f
```

### Résultat

- Application accessible sur `http://devtechly.com` (ou `https://devtechly.com`)
- Accessible depuis Internet
- Nom de domaine configuré

---

## 🔄 Workflow complet VPS

### Sur votre PC Windows (Machine de développement)

```powershell
# 1. Modifier le code
# 2. Build et push
.\build-and-push.ps1 -Username mocherqu -Tag v1.0.0
```

### Sur votre VPS (Serveur de production)

```bash
# 1. Se connecter au VPS
ssh utilisateur@votre-vps-ip

# 2. Aller dans le répertoire
cd ~/devtech

# 3. Pull de la nouvelle image
docker pull mocherqu/devtechly:v1.0.0

# 4. Mettre à jour docker-compose.prod-with-domain.yml avec le nouveau tag
# (ou utiliser latest)

# 5. Redémarrer les services
docker compose -f docker-compose.prod-with-domain.yml down
docker compose -f docker-compose.prod-with-domain.yml --env-file .env.prod up -d

# 6. Vérifier
docker compose -f docker-compose.prod-with-domain.yml ps
```

---

## 📋 Comparaison

| Aspect             | Local (PC Windows)         | VPS/Serveur                           |
| ------------------ | -------------------------- | ------------------------------------- |
| **Machine**        | Votre PC                   | Serveur distant                       |
| **OS**             | Windows                    | Linux                                 |
| **URL**            | `localhost:8080`           | `devtechly.com`                       |
| **Accès**          | Local uniquement           | Internet                              |
| **Script**         | `deploy-from-registry.ps1` | `deploy-with-domain.ps1`              |
| **Docker Compose** | `docker-compose.prod.yml`  | `docker-compose.prod-with-domain.yml` |
| **Nginx**          | Non nécessaire             | Oui (reverse proxy)                   |
| **DNS**            | Non nécessaire             | Oui                                   |
| **SSL/HTTPS**      | Non nécessaire             | Recommandé                            |

---

## ❓ Questions fréquentes

### Q : Puis-je utiliser mon PC Windows comme serveur de production ?

**R :** Techniquement oui, mais **non recommandé** :

- ❌ Votre PC doit être allumé 24/7
- ❌ Pas d'IP publique fixe (sauf configuration spéciale)
- ❌ Sécurité moins bonne
- ❌ Performance limitée

**Mieux :** Utiliser un VPS (OVH, DigitalOcean, AWS, etc.)

### Q : Dois-je copier tous les fichiers du projet sur le VPS ?

**R :** Non, seulement les fichiers nécessaires :

- ✅ `docker-compose.prod-with-domain.yml`
- ✅ `.env.prod`
- ✅ `nginx/` (dossier complet)
- ❌ Pas besoin du code source (l'image Docker contient tout)

### Q : Comment transférer les fichiers vers le VPS ?

**R :** Plusieurs méthodes :

1. **SCP** (depuis PowerShell ou Git Bash) : `scp fichier utilisateur@vps:~/devtech/`
2. **SFTP** (FileZilla, WinSCP)
3. **Git** (si le projet est versionné)
4. **Créer les fichiers directement sur le VPS** (copier-coller le contenu)

### Q : Puis-je utiliser le même script sur Windows et Linux ?

**R :**

- **Windows :** Utilisez `deploy-with-domain.ps1` (PowerShell)
- **Linux :** Créez un équivalent bash ou utilisez les commandes Docker directement

---

## ✅ Checklist VPS

Avant de déployer sur un VPS :

- [ ] VPS avec IP publique
- [ ] Docker installé sur le VPS
- [ ] DNS configuré (A record vers l'IP du VPS)
- [ ] Ports 80 et 443 ouverts dans le firewall
- [ ] Fichiers copiés sur le VPS
- [ ] `.env.prod` configuré avec les bonnes valeurs
- [ ] Connecté à Docker Hub sur le VPS (`docker login`)
- [ ] Image buildée et pushée depuis votre PC

---

## 🎯 Résumé

**Étape 2 = VPS/Serveur distant** ✅

L'étape 2 du guide `DEPLOIEMENT_AVEC_DOMAINE.md` concerne le déploiement sur un **VPS ou serveur distant** (pas votre PC local).

Pour localhost, utilisez simplement `deploy-from-registry.ps1` sur votre PC.
