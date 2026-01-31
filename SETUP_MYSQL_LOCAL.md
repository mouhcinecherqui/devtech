# 🔧 Configuration MySQL pour lancement local en production

## Problème

L'erreur `Access denied for user 'devtechly'@'localhost'` indique que l'utilisateur MySQL `devtechly` n'existe pas encore sur votre base de données locale.

## Solution 1 : Utiliser Docker (Recommandé) 🐳

Si vous utilisez Docker, MySQL sera créé automatiquement avec l'utilisateur `devtechly`.

### Étapes :

1. **Démarrer MySQL avec Docker** :

   ```powershell
   docker compose -f docker-compose.prod.yml up -d mysql
   ```

2. **Attendre que MySQL soit prêt** (vérifier les logs) :

   ```powershell
   docker compose -f docker-compose.prod.yml logs mysql
   ```

3. **Vérifier que la base de données est créée** :

   ```powershell
   docker compose -f docker-compose.prod.yml exec mysql mysql -u devtechly -piNfc1gPa9HxSpYzbJ3m5sKAXZ8jTU7Fy -e "SHOW DATABASES;"
   ```

4. **Lancer l'application** :
   ```powershell
   docker compose -f docker-compose.prod.yml up -d
   ```

## Solution 2 : MySQL installé localement 💻

Si vous avez MySQL installé directement sur Windows (sans Docker), vous devez créer l'utilisateur et la base de données manuellement.

### Étapes :

1. **Ouvrir MySQL en tant qu'administrateur** :

   - Ouvrez PowerShell ou CMD en tant qu'administrateur
   - Naviguez vers le répertoire d'installation MySQL (généralement `C:\Program Files\MySQL\MySQL Server 8.0\bin`)
   - Ou ajoutez MySQL au PATH système

2. **Se connecter à MySQL en tant que root** :

   ```powershell
   mysql -u root -p
   ```

   Entrez votre mot de passe root MySQL.

3. **Exécuter le script SQL** :
   Une fois connecté à MySQL, copiez-collez le contenu de `setup-mysql-prod.sql` :

   ```sql
   CREATE USER IF NOT EXISTS 'devtechly'@'localhost' IDENTIFIED BY 'iNfc1gPa9HxSpYzbJ3m5sKAXZ8jTU7Fy';
   CREATE DATABASE IF NOT EXISTS devtechly CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   GRANT ALL PRIVILEGES ON devtechly.* TO 'devtechly'@'localhost';
   FLUSH PRIVILEGES;
   ```

4. **Vérifier la création** :

   ```sql
   SELECT User, Host FROM mysql.user WHERE User = 'devtechly';
   SHOW DATABASES LIKE 'devtechly';
   ```

5. **Quitter MySQL** :
   ```sql
   EXIT;
   ```

### Alternative : Exécuter le script directement

Si MySQL est dans votre PATH :

```powershell
mysql -u root -p < setup-mysql-prod.sql
```

Ou avec PowerShell :

```powershell
Get-Content setup-mysql-prod.sql | mysql -u root -p
```

## Configuration actuelle dans `.env.prod`

Les paramètres de connexion sont déjà configurés pour localhost :

```bash
DB_URL=jdbc:mysql://localhost:3306/devtechly?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USER=devtechly
DB_PASSWORD=iNfc1gPa9HxSpYzbJ3m5sKAXZ8jTU7Fy
```

## Vérification

Après avoir créé l'utilisateur et la base de données, testez la connexion :

```powershell
mysql -u devtechly -piNfc1gPa9HxSpYzbJ3m5sKAXZ8jTU7Fy -e "SHOW DATABASES;"
```

Vous devriez voir la base de données `devtechly` dans la liste.

## Lancer l'application

Une fois MySQL configuré, vous pouvez lancer l'application :

```powershell
# Avec Maven
mvnw spring-boot:run -Dspring-boot.run.profiles=prod

# Ou avec Docker
docker compose -f docker-compose.prod.yml up -d
```

## Dépannage

### Erreur : "mysql: command not found"

- Ajoutez MySQL au PATH système
- Ou utilisez le chemin complet : `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe`

### Erreur : "Access denied for user 'root'@'localhost'"

- Vérifiez que vous utilisez le bon mot de passe root
- Essayez de vous connecter sans mot de passe : `mysql -u root`

### Erreur persiste après création

- Vérifiez que MySQL est démarré : `net start MySQL80` (Windows)
- Vérifiez que le port 3306 est libre : `netstat -an | findstr 3306`
