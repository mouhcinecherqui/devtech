# 🏗️ Architecture Docker - Monolithique vs Séparée

## 📊 Vue d'ensemble

Votre application JHipster peut être déployée de deux façons :

### Option 1 : Architecture Monolithique (Actuelle) ✅ Recommandée pour débuter

**Une seule image Docker** contenant :

- Backend Spring Boot (API REST)
- Frontend Angular compilé (fichiers statiques servis par Spring Boot)

**Fichiers utilisés :**

- `Dockerfile.jhipster` - Build monolithique
- `docker-compose.prod.yml` - Déploiement monolithique

**Avantages :**

- ✅ Simple à déployer et maintenir
- ✅ Pas de problèmes CORS
- ✅ Un seul conteneur à gérer
- ✅ Déploiement rapide
- ✅ Idéal pour les petits/moyens projets

**Inconvénients :**

- ❌ Rebuild complet même pour un petit changement frontend
- ❌ Moins flexible pour la scalabilité
- ❌ Image plus volumineuse

---

### Option 2 : Architecture Séparée (Moderne) 🚀 Pour la production avancée

**Deux images Docker séparées :**

- `devtechly-backend` - Backend Spring Boot uniquement (API REST)
- `devtechly-frontend` - Frontend Angular avec Nginx (fichiers statiques)

**Fichiers utilisés :**

- `Dockerfile.backend` - Build backend seul
- `Dockerfile.frontend` - Build frontend avec Nginx
- `docker-compose.separated.yml` - Déploiement séparé
- `build-and-push-separated.ps1` - Script pour build/push les deux images

**Avantages :**

- ✅ Déploiements indépendants (mettre à jour le frontend sans toucher au backend)
- ✅ Scalabilité indépendante (plusieurs instances frontend/backend)
- ✅ Images plus petites et optimisées
- ✅ Architecture moderne et flexible
- ✅ Meilleure séparation des responsabilités

**Inconvénients :**

- ❌ Plus complexe à configurer
- ❌ Nécessite un reverse proxy ou configuration CORS
- ❌ Plus de conteneurs à gérer

---

## 🎯 Quelle architecture choisir ?

### Choisissez l'architecture Monolithique si :

- Vous débutez avec Docker
- Votre projet est petit/moyen
- Vous voulez une solution simple et rapide
- Vous n'avez pas besoin de scalabilité avancée

### Choisissez l'architecture Séparée si :

- Vous avez une équipe frontend/backend séparée
- Vous voulez déployer le frontend et backend indépendamment
- Vous avez besoin de scalabilité (plusieurs instances)
- Vous voulez une architecture moderne et flexible

---

## 📝 Guide d'utilisation

### Architecture Monolithique (Recommandée)

#### 1. Build et Push

```powershell
# Build et push de l'image monolithique
.\build-and-push.ps1 -Username mocherqu
```

#### 2. Déployer

```powershell
# Déployer l'application monolithique
.\deploy-from-registry.ps1 -Username mocherqu
```

L'application sera accessible sur : `http://localhost:8080`

---

### Architecture Séparée

#### 1. Build et Push les deux images

```powershell
# Build et push backend + frontend
.\build-and-push-separated.ps1 -Username mocherqu

# Ou seulement le backend
.\build-and-push-separated.ps1 -Username mocherqu -BackendOnly

# Ou seulement le frontend
.\build-and-push-separated.ps1 -Username mocherqu -FrontendOnly
```

#### 2. Déployer

```powershell
# Déployer avec docker-compose
docker compose -f docker-compose.separated.yml --env-file .env.prod up -d
```

L'application sera accessible sur :

- **Frontend** : `http://localhost` (port 80)
- **Backend API** : `http://localhost:8080`

---

## 🔧 Configuration CORS (Architecture Séparée)

Si vous séparez le frontend et le backend, vous devez configurer CORS dans Spring Boot.

### Modifier `SecurityConfiguration.java`

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
  CorsConfiguration configuration = new CorsConfiguration();
  configuration.setAllowedOrigins(Arrays.asList("http://localhost", "https://votre-domaine.com"));
  configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
  configuration.setAllowedHeaders(Arrays.asList("*"));
  configuration.setAllowCredentials(true);

  UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
  source.registerCorsConfiguration("/api/**", configuration);
  return source;
}

```

### Ou utiliser Nginx comme reverse proxy

Modifiez `Dockerfile.frontend` pour décommenter la section proxy dans la configuration Nginx.

---

## 📊 Comparaison des images

| Critère                 | Monolithique | Séparée                                 |
| ----------------------- | ------------ | --------------------------------------- |
| Nombre d'images         | 1            | 2                                       |
| Taille totale           | ~800MB       | ~600MB (backend) + ~50MB (frontend)     |
| Temps de build          | 10-15 min    | 5-10 min (backend) + 3-5 min (frontend) |
| Complexité              | Faible       | Moyenne                                 |
| Scalabilité             | Limitée      | Excellente                              |
| Déploiement indépendant | Non          | Oui                                     |

---

## 🚀 Migration de Monolithique vers Séparée

Si vous voulez migrer de l'architecture monolithique vers l'architecture séparée :

1. **Build les nouvelles images :**

   ```powershell
   .\build-and-push-separated.ps1 -Username mocherqu
   ```

2. **Arrêter l'ancienne version :**

   ```powershell
   docker compose -f docker-compose.prod.yml down
   ```

3. **Démarrer la nouvelle version :**

   ```powershell
   docker compose -f docker-compose.separated.yml --env-file .env.prod up -d
   ```

4. **Configurer CORS** (voir section ci-dessus)

---

## ✅ Recommandation

**Pour commencer :** Utilisez l'architecture **Monolithique** avec `Dockerfile.jhipster`

**Pour la production avancée :** Migrez vers l'architecture **Séparée** quand vous avez besoin de plus de flexibilité

---

## 📚 Fichiers de référence

- **Monolithique :**

  - `Dockerfile.jhipster`
  - `docker-compose.prod.yml`
  - `build-and-push.ps1`
  - `deploy-from-registry.ps1`

- **Séparée :**
  - `Dockerfile.backend`
  - `Dockerfile.frontend`
  - `docker-compose.separated.yml`
  - `build-and-push-separated.ps1`
