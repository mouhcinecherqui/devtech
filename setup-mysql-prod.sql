-- Script SQL pour créer l'utilisateur et la base de données pour le mode production
-- À exécuter en tant qu'administrateur MySQL (root)

-- Créer l'utilisateur s'il n'existe pas déjà
CREATE USER IF NOT EXISTS 'devtechly'@'localhost' IDENTIFIED BY 'iNfc1gPa9HxSpYzbJ3m5sKAXZ8jTU7Fy';

-- Créer la base de données si elle n'existe pas
CREATE DATABASE IF NOT EXISTS devtechly CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Accorder tous les privilèges sur la base de données
GRANT ALL PRIVILEGES ON devtechly.* TO 'devtechly'@'localhost';

-- Appliquer les changements
FLUSH PRIVILEGES;

-- Vérifier que l'utilisateur existe
SELECT User, Host FROM mysql.user WHERE User = 'devtechly';

-- Vérifier que la base de données existe
SHOW DATABASES LIKE 'devtechly';
