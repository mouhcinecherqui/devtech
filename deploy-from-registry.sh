#!/bin/bash
# Script de déploiement depuis un registre Docker

set -e

# Valeurs par défaut
IMAGE_NAME="${IMAGE_NAME:-}"
REGISTRY="${REGISTRY:-docker.io}"
USERNAME="${USERNAME:-}"
APP_IMAGE_NAME="${APP_IMAGE_NAME:-devtechly}"
TAG="${TAG:-latest}"
PULL_ONLY="${PULL_ONLY:-false}"

# Parse des arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --image-name)
            IMAGE_NAME="$2"
            shift 2
            ;;
        --registry)
            REGISTRY="$2"
            shift 2
            ;;
        --username)
            USERNAME="$2"
            shift 2
            ;;
        --app-image-name)
            APP_IMAGE_NAME="$2"
            shift 2
            ;;
        --tag)
            TAG="$2"
            shift 2
            ;;
        --pull-only)
            PULL_ONLY="true"
            shift
            ;;
        *)
            echo "Usage: $0 [--image-name FULL_NAME] [--registry REGISTRY] [--username USERNAME] [--app-image-name NAME] [--tag TAG] [--pull-only]"
            exit 1
            ;;
    esac
done

echo "========================================"
echo "  Deploiement depuis Registry Docker"
echo "========================================"
echo ""

# Vérifier Docker
if ! command -v docker &> /dev/null; then
    echo "[ERREUR] Docker n'est pas installe!"
    exit 1
fi

echo "[OK] Docker detecte"
echo ""

# Déterminer le nom complet de l'image
if [ -z "$IMAGE_NAME" ]; then
    if [ "$REGISTRY" = "docker.io" ]; then
        if [ -z "$USERNAME" ]; then
            echo "[ERREUR] Username requis pour Docker Hub"
            echo "   Utilisation: $0 --username votre-username"
            exit 1
        fi
        FULL_IMAGE_NAME="${USERNAME}/${APP_IMAGE_NAME}:${TAG}"
    elif [ "$REGISTRY" = "ghcr.io" ]; then
        if [ -z "$USERNAME" ]; then
            echo "[ERREUR] Username requis pour GitHub Container Registry"
            echo "   Utilisation: $0 --registry ghcr.io --username votre-username"
            exit 1
        fi
        FULL_IMAGE_NAME="${REGISTRY}/${USERNAME}/${APP_IMAGE_NAME}:${TAG}"
    else
        # Registre privé
        if [ -z "$USERNAME" ]; then
            FULL_IMAGE_NAME="${REGISTRY}/${APP_IMAGE_NAME}:${TAG}"
        else
            FULL_IMAGE_NAME="${REGISTRY}/${USERNAME}/${APP_IMAGE_NAME}:${TAG}"
        fi
    fi
else
    FULL_IMAGE_NAME="$IMAGE_NAME"
fi

echo "[*] Configuration:"
echo "   Image: $FULL_IMAGE_NAME"
echo ""

# Vérifier si .env.prod existe
if [ ! -f ".env.prod" ]; then
    echo "[!] Fichier .env.prod non trouve"
    echo "   Creation du fichier .env.prod..."
    
    # Générer un secret JWT sécurisé
    JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n\r')
    
    # Générer des mots de passe
    DB_PASSWORD=$(openssl rand -base64 32 | tr -d '\n\r' | tr -d '+/=' | head -c 32)
    ROOT_PASSWORD=$(openssl rand -base64 32 | tr -d '\n\r' | tr -d '+/=' | head -c 32)
    
    DB_URL='jdbc:mysql://mysql:3306/devtechly?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
    
    cat > .env.prod << EOF
# Configuration de production devtechly
# Genere automatiquement le $(date '+%Y-%m-%d %H:%M:%S')

# Profil Spring
SPRING_PROFILES_ACTIVE=prod

# Base de donnees MySQL (conteneur)
MYSQL_ROOT_PASSWORD=$ROOT_PASSWORD
MYSQL_DATABASE=devtechly
MYSQL_USER=devtechly
MYSQL_PASSWORD=$DB_PASSWORD

# Application - Base de donnees
DB_URL=$DB_URL
DB_USER=devtechly
DB_PASSWORD=$DB_PASSWORD

# JWT Secret
JWT_SECRET=$JWT_SECRET

# Mail (Gmail SMTP) - a configurer
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=votre-email@gmail.com
SPRING_MAIL_PASSWORD=votre-mot-de-passe-app
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true

# OAuth2 Google - a configurer
GOOGLE_CLIENT_ID=votre_client_id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=votre_client_secret

# URL de base
JHIPSTER_MAIL_BASE_URL=https://devtechly.com

# Configuration Java
JAVA_OPTS=-Xmx1g -Xms512m -XX:+UseG1GC -XX:+UseContainerSupport
EOF
    
    echo "[OK] Fichier .env.prod cree"
    echo "   [!] IMPORTANT: Modifiez EMAIL et OAuth2 Google dans .env.prod!"
    echo ""
fi

# Créer un fichier docker-compose temporaire avec l'image du registre
COMPOSE_FILE="docker-compose.registry.yml"
echo "[*] Creation du fichier docker-compose avec l'image du registre..."

cat > "$COMPOSE_FILE" << EOF
name: devtechly-prod
services:
  devtechly-app:
    image: $FULL_IMAGE_NAME
    ports:
      - '8080:8080'
    env_file:
      - .env.prod
    environment:
      - SPRING_PROFILES_ACTIVE=\${SPRING_PROFILES_ACTIVE:-prod}
      - DB_URL=\${DB_URL}
      - DB_USER=\${DB_USER}
      - DB_PASSWORD=\${DB_PASSWORD}
      - JWT_SECRET=\${JWT_SECRET}
      - GOOGLE_CLIENT_ID=\${GOOGLE_CLIENT_ID}
      - GOOGLE_CLIENT_SECRET=\${GOOGLE_CLIENT_SECRET}
      - SPRING_MAIL_USERNAME=\${SPRING_MAIL_USERNAME}
      - SPRING_MAIL_PASSWORD=\${SPRING_MAIL_PASSWORD}
      - JHIPSTER_MAIL_BASE_URL=\${JHIPSTER_MAIL_BASE_URL:-https://devtechly.com}
    depends_on:
      mysql:
        condition: service_started
    restart: unless-stopped
  mysql:
    image: mysql:8.0.32
    volumes:
      - ./src/main/docker/config/mysql:/etc/mysql/conf.d
      - prod-db-data:/var/lib/mysql
    env_file:
      - .env.prod
    environment:
      - MYSQL_ROOT_PASSWORD=\${MYSQL_ROOT_PASSWORD}
      - MYSQL_USER=\${MYSQL_USER:-devtechly}
      - MYSQL_PASSWORD=\${MYSQL_PASSWORD}
      - MYSQL_DATABASE=\${MYSQL_DATABASE:-devtechly}
    command: mysqld --lower_case_table_names=1 --skip-mysqlx --character_set_server=utf8mb4 --explicit_defaults_for_timestamp
    healthcheck:
      test: ['CMD-SHELL', 'mysql -e "SHOW DATABASES;" && sleep 5']
      interval: 5s
      timeout: 10s
      retries: 10
volumes:
  prod-db-data:
EOF

echo "[OK] Fichier $COMPOSE_FILE cree"
echo ""

# Pull de l'image depuis le registre
echo "[*] Telechargement de l'image depuis le registre..."
echo "   Image: $FULL_IMAGE_NAME"

# Vérifier si l'utilisateur doit se connecter
if [ "$REGISTRY" != "docker.io" ] && [ "$REGISTRY" != "ghcr.io" ]; then
    echo "   Assurez-vous d'etre connecte au registre:"
    if [ "$REGISTRY" = "ghcr.io" ]; then
        echo "   echo \$GITHUB_TOKEN | docker login ghcr.io -u $USERNAME --password-stdin"
    else
        echo "   docker login $REGISTRY"
    fi
    echo ""
fi

docker pull "$FULL_IMAGE_NAME"

if [ $? -ne 0 ]; then
    echo "[ERREUR] Echec du telechargement de l'image"
    echo "   Verifiez que vous etes connecte au registre et que l'image existe"
    exit 1
fi

echo "[OK] Image telechargee avec succes"
echo ""

if [ "$PULL_ONLY" = "true" ]; then
    echo "[OK] Image telechargee. Utilisez docker-compose pour demarrer les services."
    echo "   docker compose -f $COMPOSE_FILE --env-file .env.prod up -d"
    exit 0
fi

# Arrêter les conteneurs existants
echo "[*] Arret des conteneurs existants..."
docker compose -f "$COMPOSE_FILE" down 2>/dev/null || true

# Démarrer les services
echo "[*] Demarrage des services..."

docker compose -f "$COMPOSE_FILE" --env-file .env.prod up -d

if [ $? -eq 0 ]; then
    echo "[OK] Services demarres avec succes"
    echo ""
    echo "[*] Statut des services:"
    docker compose -f "$COMPOSE_FILE" ps
    echo ""
    echo "[OK] L'application devrait etre accessible sur http://localhost:8080"
    echo ""
    echo "[*] Pour voir les logs:"
    echo "   docker compose -f $COMPOSE_FILE logs -f"
else
    echo "[ERREUR] Erreur lors du demarrage des services"
    echo "   Verifiez les logs avec: docker compose -f $COMPOSE_FILE logs"
    exit 1
fi

echo ""
echo "========================================"
echo "[OK] Deploiement termine avec succes!"
echo ""
