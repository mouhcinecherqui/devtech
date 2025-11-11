# 🚀 Guide de Déploiement en Production - DevTech

## ✅ État actuel

Le déploiement en production a été préparé avec les fichiers suivants :

1. ✅ **deploy-prod.ps1** - Script de déploiement PowerShell
2. ✅ **docker-compose.prod.yml** - Configuration Docker Compose pour la production (mis à jour)
3. ✅ **.env.prod** - Variables d'environnement de production (à configurer)
4. ✅ **Dockerfile.jhipster** - Dockerfile pour la production

## ⚠️ Configuration requise AVANT le déploiement

### 1. Modifier le fichier `.env.prod`

Ouvrez le fichier `.env.prod` et modifiez les valeurs suivantes :

```bash
# Email SMTP - REMPLACER par vos vraies données
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=votre-email@gmail.com
SPRING_MAIL_PASSWORD=votre-mot-de-passe-app

# OAuth2 Google - REMPLACER par vos vraies clés
GOOGLE_CLIENT_ID=votre_client_id_google
GOOGLE_CLIENT_SECRET=votre_client_secret_google
```

### 2. Vérifier les mots de passe générés

Les mots de passe pour MySQL ont été générés automatiquement. Assurez-vous qu'ils sont suffisamment sécurisés pour la production.

## 📦 Options de déploiement

### Option A : Déploiement local avec Docker Desktop

Si Docker Desktop est installé et démarré sur votre machine :

```powershell
# Exécuter le script de déploiement
.\deploy-prod.ps1
```

Le script va :

1. Construire l'image Docker `devtech:latest`
2. Démarrer MySQL et l'application avec Docker Compose
3. Exposer l'application sur `http://localhost:8080`

### Option B : Déploiement sur serveur distant (Linux)

#### Sur votre machine Windows :

1. Copiez les fichiers suivants sur le serveur :
   - `Dockerfile.jhipster`
   - `docker-compose.prod.yml`
   - `.env.prod`
   - Tout le code source du projet

#### Sur le serveur Linux :

```bash
# Installer Docker et Docker Compose (si pas déjà installé)
sudo apt update
sudo apt install -y docker.io docker-compose

# Naviguer vers le répertoire du projet
cd /path/to/devtech

# Construire l'image Docker
docker build -f Dockerfile.jhipster -t devtech:latest .

# Démarrer les services
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d

# Vérifier le statut
docker compose -f docker-compose.prod.yml ps

# Voir les logs
docker compose -f docker-compose.prod.yml logs -f
```

## 🔍 Vérification du déploiement

### Vérifier que les services sont en cours d'exécution :

```bash
docker compose -f docker-compose.prod.yml ps
```

Vous devriez voir :

- `devtech-prod-mysql-1` - En cours d'exécution (healthy)
- `devtech-prod-devtech-app-1` - En cours d'exécution

### Tester l'application :

```bash
# Vérifier la santé de l'application
curl http://localhost:8080/management/health

# Ou ouvrir dans un navigateur
http://localhost:8080
```

## 📝 Commandes utiles

### Voir les logs en temps réel :

```bash
docker compose -f docker-compose.prod.yml logs -f
```

### Voir les logs d'un service spécifique :

```bash
docker compose -f docker-compose.prod.yml logs -f devtech-app
docker compose -f docker-compose.prod.yml logs -f mysql
```

### Arrêter les services :

```bash
docker compose -f docker-compose.prod.yml down
```

### Redémarrer les services :

```bash
docker compose -f docker-compose.prod.yml restart
```

### Mettre à jour l'application (après modifications du code) :

```bash
# Reconstruire l'image
docker build -f Dockerfile.jhipster -t devtech:latest .

# Redémarrer les services
docker compose -f docker-compose.prod.yml up -d --build
```

## 🔐 Sécurité

⚠️ **IMPORTANT pour la production** :

1. **Mots de passe** : Les mots de passe générés dans `.env.prod` sont suffisants pour le développement, mais pour la production réelle, générez des mots de passe plus complexes.

2. **JWT Secret** : Le secret JWT a été généré automatiquement, mais vous pouvez en générer un nouveau avec :

   ```bash
   openssl rand -base64 64
   ```

3. **Variables sensibles** : Ne commitez **JAMAIS** le fichier `.env.prod` dans Git ! Il contient des secrets.

4. **Base de données** : Pour la production, considérez :
   - Utiliser une base de données MySQL externe (RDS, Cloud SQL, etc.)
   - Configurer des sauvegardes automatiques
   - Utiliser SSL/TLS pour les connexions à la base de données

## 🌐 Configuration HTTPS (production réelle)

Pour une production réelle, vous devrez :

1. Configurer un reverse proxy (Nginx ou Traefik)
2. Obtenir un certificat SSL (Let's Encrypt recommandé)
3. Configurer les redirections HTTP → HTTPS
4. Mettre à jour les URLs CORS dans `application-prod.yml`

Voir les guides détaillés :

- `README_DEPLOIEMENT_OVH.md`
- `README_DEPLOIEMENT_DOCKER_OVH.md`

## 🆘 Dépannage

### L'application ne démarre pas :

```bash
# Vérifier les logs
docker compose -f docker-compose.prod.yml logs devtech-app

# Vérifier la connexion à MySQL
docker compose -f docker-compose.prod.yml logs mysql
```

### Erreur de connexion à la base de données :

- Vérifiez que MySQL est démarré : `docker compose -f docker-compose.prod.yml ps`
- Vérifiez les variables `SPRING_DATASOURCE_*` dans `.env.prod`
- Attendez quelques secondes (MySQL peut prendre du temps à démarrer)

### Erreur de port déjà utilisé :

Modifiez le port dans `docker-compose.prod.yml` :

```yaml
ports:
  - '8081:8080' # Utiliser le port 8081 au lieu de 8080
```

## ✅ Checklist de déploiement

- [ ] Fichier `.env.prod` créé et configuré
- [ ] Variables EMAIL configurées
- [ ] Variables OAUTH2 Google configurées
- [ ] Docker installé et démarré
- [ ] Image Docker construite
- [ ] Services démarrés avec succès
- [ ] Application accessible sur http://localhost:8080
- [ ] Santé de l'application vérifiée (`/management/health`)
- [ ] Logs vérifiés (pas d'erreurs critiques)

---

**🎉 Félicitations ! Votre application DevTech est prête pour la production !**

Pour toute question, consultez les guides détaillés ou les logs Docker.
