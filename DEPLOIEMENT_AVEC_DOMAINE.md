# 🌐 Déploiement avec Nom de Domaine

Guide pour déployer votre application devtechly avec votre nom de domaine (localhost) au lieu de localhost.

## 📊 Situation actuelle vs avec domaine

### ❌ Actuellement (localhost)

- **URL :** `http://localhost:8080`
- **Accès :** Seulement depuis la machine locale
- **Configuration :** `docker-compose.prod.yml`

### ✅ Avec nom de domaine

- **URL :** `http://localhost`
- **Accès :** Depuis Internet
- **Configuration :** `docker-compose.prod-with-domain.yml` + Nginx

## 🏗️ Architecture avec domaine

```
Internet
    ↓
[Votre Serveur]
    ↓
[Nginx - Port 80/443] ← Reverse Proxy
    ↓
[Spring Boot - Port 8080] ← Application (conteneur Docker)
    ↓
[MySQL - Port 3306] ← Base de données (conteneur Docker)
```

## 📋 Prérequis

1. ✅ **Serveur avec IP publique** (VPS, Cloud, etc.)
2. ✅ **Nom de domaine** configuré (localhost)
3. ✅ **DNS configuré** : A record pointant vers l'IP du serveur
4. ✅ **Docker installé** sur le serveur
5. ✅ **Ports 80 et 443 ouverts** dans le firewall

## 🚀 Déploiement en 5 étapes

### Étape 1 : Configuration DNS

Configurez votre DNS pour pointer vers votre serveur :

```
Type    Name    Value           TTL
A       @       VOTRE_IP        Auto
A       www     VOTRE_IP        Auto
```

**Exemple :**

- `localhost` → `123.45.67.89`
- `localhost` → `123.45.67.89`

### Étape 2 : Préparer les fichiers sur le serveur

Sur votre serveur Linux, copiez ces fichiers :

```bash
# Structure de fichiers nécessaire
devtech/
├── docker-compose.prod-with-domain.yml
├── .env.prod
├── nginx/
│   ├── nginx.conf
│   └── conf.d/
│       └── devtechly.conf
└── src/main/docker/config/mysql/  # Si vous avez des configs MySQL
```

### Étape 3 : Build et Push de l'image (depuis votre machine)

```powershell
# Sur votre machine de développement
.\build-and-push.ps1 -Username mocherqu
```

### Étape 4 : Déployer sur le serveur

Sur votre serveur Linux :

```bash
# 1. Se connecter à Docker Hub
docker login

# 2. Créer le répertoire pour les certificats SSL (si HTTPS)
mkdir -p nginx/ssl

# 3. Démarrer les services
docker compose -f docker-compose.prod-with-domain.yml --env-file .env.prod up -d

# 4. Vérifier les logs
docker compose -f docker-compose.prod-with-domain.yml logs -f
```

### Étape 5 : Vérifier le déploiement

```bash
# Option 1 — Depuis le serveur (après redéploiement avec nginx à jour)
curl http://localhost/management/health

# Option 2 — Test fiable sans passer par le port 80 (si vous avez encore une 301)
docker compose -f docker-compose.prod-with-domain.yml exec nginx wget -qO- http://devtechly-app:8080/management/health

# Depuis Internet (remplacer par votre domaine)
curl http://devtechly.com/management/health
```

> **Si vous obtenez une 301** : un autre Nginx (ou Apache) sur l'hôte peut écouter sur le port 80. l’Utilisez l'option 2 (exec dans le conteneur) pour vérifier que l'application répond. Le bloc Nginx inclut localhost dans server_name ; redéployez après modification de nginx/conf.d/devtechly.conf.

## 🔒 Configuration HTTPS (Recommandé)

Pour activer HTTPS avec Let's Encrypt :

### Option 1 : Certbot avec Nginx (Recommandé)

```bash
# 1. Installer Certbot
sudo apt update
sudo apt install certbot python3-certbot-nginx

# 2. Arrêter temporairement Nginx dans Docker
docker compose -f docker-compose.prod-with-domain.yml stop nginx

# 3. Obtenir les certificats
sudo certbot certonly --standalone -d localhost

# 4. Copier les certificats dans le répertoire nginx/ssl
sudo cp /etc/letsencrypt/live/localhost/fullchain.pem nginx/ssl/
sudo cp /etc/letsencrypt/live/localhost/privkey.pem nginx/ssl/
sudo chmod 644 nginx/ssl/*.pem

# 5. Décommenter la section HTTPS dans nginx/conf.d/devtechly.conf
# 6. Décommenter la redirection HTTP vers HTTPS

# 7. Redémarrer Nginx
docker compose -f docker-compose.prod-with-domain.yml up -d nginx
```

### Option 2 : Certbot dans un conteneur Docker

```yaml
# Ajouter dans docker-compose.prod-with-domain.yml
certbot:
  image: certbot/certbot
  volumes:
    - ./nginx/ssl:/etc/letsencrypt
  command: certonly --standalone -d localhost --email votre-email@example.com --agree-tos
```

## ⚙️ Configuration de l'application

### Variables d'environnement (.env.prod)

Assurez-vous que `.env.prod` contient :

```bash
# URL de base de l'application
JHIPSTER_MAIL_BASE_URL=http://localhost

# Autres variables...
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://mysql:3306/devtechly?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
# ...
```

### Configuration Spring Boot

L'application est déjà configurée pour `localhost` dans :

- `src/main/resources/config/application-prod.yml`
- `src/main/webapp/environments/environment.prod.ts`

## 🔍 Vérification

### Vérifier que Nginx fonctionne

```bash
# Logs Nginx
docker compose -f docker-compose.prod-with-domain.yml logs nginx

# Tester la configuration Nginx
docker compose -f docker-compose.prod-with-domain.yml exec nginx nginx -t
```

### Vérifier que l'application répond

```bash
# Depuis le serveur
curl http://localhost/management/health

# Depuis Internet
curl http://localhost/management/health
```

### Vérifier les DNS

```bash
# Vérifier que le DNS pointe vers votre serveur
nslookup localhost
dig localhost
```

## 🔧 Dépannage

### L'application n'est pas accessible depuis Internet

1. **Vérifier le firewall :**

   ```bash
   # Ouvrir les ports 80 et 443
   sudo ufw allow 80/tcp
   sudo ufw allow 443/tcp
   sudo ufw reload
   ```

2. **Vérifier que Nginx écoute :**

   ```bash
   sudo netstat -tlnp | grep :80
   sudo netstat -tlnp | grep :443
   ```

3. **Vérifier les logs :**
   ```bash
   docker compose -f docker-compose.prod-with-domain.yml logs nginx
   docker compose -f docker-compose.prod-with-domain.yml logs devtechly-app
   ```

### Erreur 502 Bad Gateway

- Vérifier que l'application Spring Boot est démarrée
- Vérifier que le nom du service dans docker-compose est `devtechly-app`
- Vérifier les logs : `docker compose logs devtechly-app`

### Erreur de certificat SSL

- Vérifier que les certificats sont dans `nginx/ssl/`
- Vérifier les permissions : `chmod 644 nginx/ssl/*.pem`
- Vérifier la configuration Nginx : `nginx -t`

## 📝 Script de déploiement automatique

Créez `deploy-with-domain.sh` sur votre serveur :

```bash
#!/bin/bash
set -e

echo "[*] Déploiement devtechly avec domaine..."

# Pull de l'image
docker pull mocherqu/devtechly:latest

# Arrêter les services existants
docker compose -f docker-compose.prod-with-domain.yml down

# Démarrer les services
docker compose -f docker-compose.prod-with-domain.yml --env-file .env.prod up -d

# Vérifier le statut
docker compose -f docker-compose.prod-with-domain.yml ps

echo "[OK] Déploiement terminé!"
echo "[*] Application accessible sur: http://localhost"
```

## 🔄 Mise à jour de l'application

```bash
# 1. Build et push de la nouvelle version (depuis votre machine)
.\build-and-push.ps1 -Username mocherqu -Tag v1.1.0

# 2. Sur le serveur, mettre à jour
docker pull mocherqu/devtechly:v1.1.0

# 3. Mettre à jour docker-compose.prod-with-domain.yml avec le nouveau tag
# 4. Redémarrer
docker compose -f docker-compose.prod-with-domain.yml up -d
```

## ✅ Checklist de déploiement

- [ ] DNS configuré (A record vers l'IP du serveur)
- [ ] Ports 80 et 443 ouverts dans le firewall
- [ ] Fichiers copiés sur le serveur
- [ ] Image buildée et pushée vers Docker Hub
- [ ] `.env.prod` configuré avec les bonnes valeurs
- [ ] Services démarrés avec `docker-compose.prod-with-domain.yml`
- [ ] Nginx fonctionne et répond
- [ ] Application accessible depuis Internet
- [ ] HTTPS configuré (optionnel mais recommandé)
- [ ] Certificats SSL valides (si HTTPS)

## 🎉 Résultat

Une fois déployé, votre application sera accessible sur :

- **HTTP :** `http://localhost`
- **HTTP :** `http://localhost` (après configuration SSL)

Au lieu de `http://localhost:8080` !

---

**Note :** Pour un déploiement local (localhost), continuez à utiliser `docker-compose.prod.yml` et `deploy-from-registry.ps1`.
