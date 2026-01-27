# Instructions pour Régénérer les Fichiers de Traduction

## Problème

Les fichiers fusionnés `target/classes/static/i18n/{lang}.json` contiennent encore les anciennes traductions incorrectes (strings simples au lieu d'objets pour `why1` à `why5`).

## Solution

Il faut régénérer les fichiers fusionnés après avoir supprimé les clés incorrectes des fichiers source.

## Étapes

### 1. Nettoyer les fichiers de build précédents

```bash
# Supprimer les anciens fichiers fusionnés
Remove-Item -Path "target\classes\static\i18n\*.json" -Force

# Ou nettoyer complètement le dossier target
npm run clean-www
```

### 2. Reconstruire l'application en mode production

```bash
# Reconstruire avec webpack qui va fusionner les fichiers JSON
npm run webapp:build:prod
```

### 3. Vérifier que les fichiers sont correctement générés

```bash
# Vérifier que les fichiers fusionnés contiennent la bonne structure
Get-Content "target\classes\static\i18n\fr.json" | Select-String -Pattern '"why1".*\{' -Context 2,5
```

Les fichiers devraient maintenant contenir :

```json
"why1": {
  "title": "Équipe expérimentée",
  "desc": "Équipe expérimentée de développeurs et designers web",
  "detail": "Équipe expérimentée de développeurs et designers web"
}
```

Au lieu de :

```json
"why1": "Équipe expérimentée de développeurs et designers web"
```

### 4. Redémarrer l'application Spring Boot

```bash
# Si l'application tourne déjà, la redémarrer pour charger les nouveaux fichiers
# Ou simplement reconstruire le JAR/WAR
mvn clean package -DskipTests
```

### 5. Vider le cache du navigateur

- Ouvrir les outils de développement (F12)
- Vider le cache du navigateur
- Ou tester en navigation privée
- Ou ajouter `?v=timestamp` aux requêtes

## Vérification

1. **Vérifier les fichiers source** : Les fichiers dans `src/main/webapp/i18n/fr/` et `src/main/webapp/assets/i18n/fr/` ne doivent plus contenir les clés `why1` à `why5` comme strings simples.

2. **Vérifier les fichiers fusionnés** : Après le rebuild, les fichiers dans `target/classes/static/i18n/` doivent contenir la structure correcte avec `global.home.why1.title`, etc.

3. **Tester l'accès HTTP** : Accéder à `http://localhost:8080/i18n/fr.json` et vérifier que le JSON contient la bonne structure.

4. **Vérifier la console du navigateur** : Ouvrir F12 > Network et vérifier que `/i18n/fr.json` retourne un 200 OK avec le bon contenu.

## Notes Importantes

- Les fichiers fusionnés sont générés par le plugin `MergeJsonWebpackPlugin` lors du build webpack
- Les fichiers doivent être dans `target/classes/static/i18n/` pour être inclus dans le JAR/WAR
- Le pattern Spring Boot `/i18n/**` a été corrigé pour servir tous les fichiers JSON
- Le cache a été réduit à 1 jour pour les fichiers i18n pour faciliter les mises à jour
