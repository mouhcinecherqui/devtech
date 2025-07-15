-- Script pour ajouter le rôle MANAGER à un utilisateur existant
-- Remplacez 'username' par le nom d'utilisateur auquel vous voulez ajouter le rôle MANAGER

-- 1. Insérer le rôle MANAGER dans la table des autorités (si pas déjà fait)
INSERT INTO jhi_authority (name) VALUES ('ROLE_MANAGER') ON DUPLICATE KEY UPDATE name = name;

-- 2. Ajouter le rôle MANAGER à un utilisateur spécifique
-- Remplacez 'username' par le nom d'utilisateur réel
INSERT INTO jhi_user_authority (user_id, authority_name)
SELECT u.id, 'ROLE_MANAGER'
FROM jhi_user u
WHERE u.login = 'username'
AND NOT EXISTS (
    SELECT 1 FROM jhi_user_authority ua 
    WHERE ua.user_id = u.id AND ua.authority_name = 'ROLE_MANAGER'
);

-- 3. Vérifier que le rôle a été ajouté
SELECT u.login, ua.authority_name
FROM jhi_user u
JOIN jhi_user_authority ua ON u.id = ua.user_id
WHERE u.login = 'username'; 