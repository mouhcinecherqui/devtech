-- Script pour vérifier et corriger la structure de la base de données
-- À exécuter dans MySQL

-- 1. Vérifier si la table ticket existe
SELECT 'Vérification de la table ticket' as info;
SHOW TABLES LIKE 'ticket';

-- 2. Vérifier la structure de la table ticket
SELECT 'Structure de la table ticket' as info;
DESCRIBE ticket;

-- 3. Vérifier si la colonne messages existe dans ticket
SELECT 'Vérification de la colonne messages dans ticket' as info;
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'ticket' AND COLUMN_NAME = 'messages';

-- 4. Vérifier si la table ticket_message existe
SELECT 'Vérification de la table ticket_message' as info;
SHOW TABLES LIKE 'ticket_message';

-- 5. Si la table ticket_message n'existe pas, la créer
CREATE TABLE IF NOT EXISTS ticket_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    author_type VARCHAR(20) NOT NULL,
    author_login VARCHAR(100) NOT NULL,
    created_date DATETIME(6) NOT NULL,
    is_internal BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (ticket_id) REFERENCES ticket(id)
);

-- 6. Créer les index pour les performances
CREATE INDEX IF NOT EXISTS idx_ticket_message_ticket_id ON ticket_message(ticket_id);
CREATE INDEX IF NOT EXISTS idx_ticket_message_created_date ON ticket_message(created_date);
CREATE INDEX IF NOT EXISTS idx_ticket_message_author ON ticket_message(author_login);

-- 7. Vérifier la structure de la table ticket_message
SELECT 'Structure de la table ticket_message' as info;
DESCRIBE ticket_message;

-- 8. Vérifier les données existantes
SELECT 'Nombre de tickets' as info, COUNT(*) as count FROM ticket;
SELECT 'Nombre de messages' as info, COUNT(*) as count FROM ticket_message;

-- 9. Si la colonne messages existe dans ticket, la supprimer
-- (Décommentez les lignes suivantes si nécessaire)
-- ALTER TABLE ticket DROP COLUMN IF EXISTS messages;

-- 10. Vérifier les contraintes de clé étrangère
SELECT 'Vérification des contraintes de clé étrangère' as info;
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
WHERE TABLE_NAME = 'ticket_message' AND REFERENCED_TABLE_NAME IS NOT NULL; 