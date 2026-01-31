# 🔍 Diagnostic : Tickets ne se créent pas en production

## Problème

Les tickets se créent correctement en local (`localhost:8080`) mais retournent une erreur `500 Internal Server Error` en production (`devtechly.com`).

## Modifications effectuées

### 1. Configuration d'upload améliorée

- Ajout de la configuration `application.upload.path` dans `application-prod.yml`
- Support de la variable d'environnement `UPLOAD_PATH`
- Amélioration des logs pour identifier les problèmes de permissions

### 2. Gestion d'erreurs améliorée

- Logs plus détaillés dans `TicketResource.java`
- Retour d'erreurs JSON détaillées pour le débogage
- Gestion séparée des erreurs IO, URI et autres exceptions

## Causes probables

### 1. Problème de permissions sur le dossier uploads ⚠️

**Symptôme** : Erreur lors de la création du dossier ou de l'écriture des fichiers

**Solution** :

```bash
# Sur le serveur de production
# Créer le dossier uploads avec les bonnes permissions
mkdir -p /app/uploads
chmod 755 /app/uploads
chown <user>:<group> /app/uploads

# Ou si vous utilisez Docker
docker compose exec devtechly-app mkdir -p /app/uploads
docker compose exec devtechly-app chmod 755 /app/uploads
```

**Configuration Docker** :
Ajoutez un volume dans `docker-compose.prod.yml` :

```yaml
devtechly-app:
  volumes:
    - ./uploads:/app/uploads
```

### 2. Problème avec ActivityIntegrationService

**Symptôme** : L'erreur se produit lors de l'appel à `activityIntegrationService.createTicketCreatedActivity()`

**Solution** :

- Vérifier les logs du serveur pour voir si l'erreur vient de ce service
- Si le service n'est pas critique, il est déjà dans un try-catch et ne devrait pas bloquer

### 3. Problème de base de données

**Symptôme** : Erreur lors de la sauvegarde du ticket (`ticketRepository.save()`)

**Vérification** :

```bash
# Vérifier les logs MySQL
docker compose logs mysql

# Vérifier les contraintes de la table tickets
docker compose exec mysql mysql -u devtechly -p devtechly -e "SHOW CREATE TABLE ticket;"
```

### 4. Problème CORS (moins probable)

**Symptôme** : L'erreur se produit avant d'atteindre le serveur

**Vérification** :

- Vérifier que `application-prod.yml` a la bonne configuration CORS pour `devtechly.com`
- Actuellement configuré pour `localhost`, à mettre à jour pour la production

## Étapes de diagnostic

### 1. Vérifier les logs du serveur

```bash
# Logs Docker
docker compose logs devtechly-app --tail=100

# Ou logs directs si application déployée directement
tail -f /var/log/devtechly/application.log
```

**Chercher** :

- `Erreur lors de la création du ticket avec image`
- `Impossible de créer le dossier d'upload`
- `Le dossier d'upload n'est pas accessible en écriture`
- `Stack trace complet`

### 2. Vérifier la réponse HTTP détaillée

Avec les modifications, l'erreur 500 devrait maintenant retourner un JSON avec :

```json
{
  "error": "Erreur lors de la création du ticket",
  "message": "Message d'erreur détaillé",
  "type": "TypeException"
}
```

**Dans Chrome DevTools** :

1. Onglet Network
2. Cliquer sur la requête `tickets` (POST)
3. Onglet "Response" ou "Preview"
4. Voir le message d'erreur détaillé

### 3. Tester la création sans image

Créer un ticket sans image pour isoler le problème :

- Si ça fonctionne sans image → problème avec `saveImage()`
- Si ça ne fonctionne toujours pas → problème ailleurs (DB, email, etc.)

### 4. Vérifier les variables d'environnement

```bash
# Sur le serveur
docker compose exec devtechly-app env | grep -E "(UPLOAD|DB_|SPRING_)"
```

## Solutions selon le diagnostic

### Si l'erreur vient du dossier uploads

**Option 1 : Utiliser un chemin absolu**

```yaml
# Dans application-prod.yml ou .env.prod
UPLOAD_PATH=/app/uploads
```

**Option 2 : Utiliser un volume Docker**

```yaml
# Dans docker-compose.prod.yml
volumes:
  - ./uploads:/app/uploads
```

**Option 3 : Désactiver temporairement l'upload d'image**
Modifier le frontend pour ne pas envoyer d'image et voir si le ticket se crée.

### Si l'erreur vient de la base de données

1. Vérifier que la table `ticket` existe
2. Vérifier les contraintes (NOT NULL, FOREIGN KEY, etc.)
3. Vérifier que l'utilisateur `devtechly` a les permissions nécessaires

### Si l'erreur vient d'un service externe

Les services suivants sont dans des try-catch et ne devraient pas bloquer :

- `clientEmailService.sendTicketCreatedEmail()` ✅
- `mailService.sendTicketCreatedEmail()` ✅
- `notificationService.notifyClient()` ✅
- `notificationService.notifyAdmins()` ✅
- `activityIntegrationService.createTicketCreatedActivity()` ✅

Si l'un de ces services lance une exception non gérée, vérifier les logs.

## Configuration recommandée pour la production

### 1. Mettre à jour CORS dans application-prod.yml

```yaml
jhipster:
  cors:
    allowed-origins: 'https://devtechly.com,https://www.devtechly.com'
```

### 2. Configurer le chemin d'upload

```yaml
application:
  upload:
    path: ${UPLOAD_PATH:/app/uploads}
    max-file-size: 5MB
```

### 3. Dans .env.prod (production réelle)

```bash
UPLOAD_PATH=/app/uploads
```

### 4. Dans docker-compose.prod.yml

```yaml
devtechly-app:
  volumes:
    - ./uploads:/app/uploads
  environment:
    - UPLOAD_PATH=/app/uploads
```

## Test après correction

1. **Créer un ticket avec image** → Vérifier que ça fonctionne
2. **Créer un ticket sans image** → Vérifier que ça fonctionne
3. **Vérifier les logs** → S'assurer qu'il n'y a pas d'erreurs
4. **Vérifier les emails** → S'assurer que les emails sont envoyés
5. **Vérifier les notifications** → S'assurer que les notifications sont créées

## Commandes utiles

```bash
# Vérifier les permissions du dossier uploads
ls -la /app/uploads

# Créer le dossier avec les bonnes permissions
mkdir -p /app/uploads && chmod 755 /app/uploads

# Vérifier les logs en temps réel
docker compose logs -f devtechly-app

# Redémarrer l'application après modification
docker compose restart devtechly-app

# Vérifier la configuration
docker compose exec devtechly-app env | grep UPLOAD
```

## Prochaines étapes

1. ✅ Vérifier les logs du serveur de production
2. ✅ Identifier le message d'erreur exact dans la réponse HTTP
3. ✅ Appliquer la solution appropriée selon le diagnostic
4. ✅ Tester la création de tickets après correction
5. ✅ Vérifier que tout fonctionne correctement
