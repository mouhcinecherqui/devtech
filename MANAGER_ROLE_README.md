# Rôle MANAGER - Guide d'utilisation

## Vue d'ensemble

Le rôle `ROLE_MANAGER` a été ajouté au système avec les mêmes fonctionnalités que le rôle `ROLE_ADMIN`, **sauf pour les paiements**.

## Fonctionnalités disponibles pour les MANAGERS

### ✅ Fonctionnalités incluses

- **Gestion des utilisateurs** : Créer, modifier, supprimer des utilisateurs
- **Gestion des tickets** : Voir et gérer les tickets de support
- **Gestion des clients** : Accéder aux informations des clients
- **Paramètres système** : Configurer les paramètres de l'application
- **Documentation API** : Accéder à la documentation Swagger
- **Configuration** : Voir la configuration de l'application
- **Santé système** : Vérifier l'état de santé de l'application
- **Logs** : Consulter les logs de l'application
- **Métriques** : Voir les métriques de performance
- **Dashboard Manager** : Interface dédiée pour les managers

### ❌ Fonctionnalités exclues

- **Paiements** : Les managers n'ont pas accès à la gestion des paiements

## Comment ajouter le rôle MANAGER à un utilisateur

### Méthode 1 : Via l'interface d'administration

1. Connectez-vous en tant qu'administrateur
2. Allez dans "Administration" > "Gestion des utilisateurs"
3. Sélectionnez l'utilisateur à modifier
4. Dans la section "Droits", ajoutez `ROLE_MANAGER`
5. Sauvegardez les modifications

### Méthode 2 : Via la base de données

Exécutez le script SQL `add_manager_role.sql` en remplaçant 'username' par le nom d'utilisateur :

```sql
-- Ajouter le rôle MANAGER à un utilisateur
INSERT INTO jhi_user_authority (user_id, authority_name)
SELECT u.id, 'ROLE_MANAGER'
FROM jhi_user u
WHERE u.login = 'nom_utilisateur'
AND NOT EXISTS (
    SELECT 1 FROM jhi_user_authority ua
    WHERE ua.user_id = u.id AND ua.authority_name = 'ROLE_MANAGER'
);
```

## Navigation pour les MANAGERS

### Dashboard Manager

- URL : `/manager-dashboard`
- Interface dédiée avec statistiques et vue d'ensemble

### Menu de navigation

Les managers verront dans la sidebar :

- 🏠 Accueil
- 🎫 Tickets
- 👤 Clients
- ⚙️ Paramètres

**Note** : L'option 💳 Paiements n'apparaît pas pour les managers.

## Sécurité

### Backend

- Les endpoints `/api/admin/**` sont accessibles aux managers
- Les endpoints de paiement restent réservés aux admins
- La configuration de sécurité a été mise à jour pour supporter les deux rôles

### Frontend

- Les routes manager sont protégées par `UserRouteAccessService`
- Les composants vérifient les autorisations avant d'afficher le contenu
- La navbar affiche automatiquement le bon dashboard selon le rôle

## Migration depuis un rôle existant

Si vous voulez transformer un utilisateur USER en MANAGER :

1. Connectez-vous en tant qu'admin
2. Allez dans la gestion des utilisateurs
3. Sélectionnez l'utilisateur
4. Remplacez `ROLE_USER` par `ROLE_MANAGER`
5. Sauvegardez

L'utilisateur aura maintenant accès à toutes les fonctionnalités manager.

## Dépannage

### L'utilisateur ne voit pas le dashboard manager

- Vérifiez que le rôle `ROLE_MANAGER` est bien assigné
- Vérifiez dans la base de données : `SELECT * FROM jhi_user_authority WHERE authority_name = 'ROLE_MANAGER'`

### Erreur d'accès refusé

- Vérifiez que l'utilisateur a bien le rôle `ROLE_MANAGER`
- Vérifiez les logs de l'application pour plus de détails

### Le menu ne s'affiche pas correctement

- Videz le cache du navigateur
- Vérifiez que l'application a bien redémarré après les modifications
