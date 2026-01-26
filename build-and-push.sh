#!/bin/bash
# Script pour construire et pousser l'image Docker vers un registre

set -e

# Valeurs par défaut
REGISTRY="${REGISTRY:-docker.io}"
USERNAME="${USERNAME:-}"
IMAGE_NAME="${IMAGE_NAME:-devtechly}"
TAG="${TAG:-latest}"
SKIP_BUILD="${SKIP_BUILD:-false}"
SKIP_PUSH="${SKIP_PUSH:-false}"

# Parse des arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --registry)
            REGISTRY="$2"
            shift 2
            ;;
        --username)
            USERNAME="$2"
            shift 2
            ;;
        --image-name)
            IMAGE_NAME="$2"
            shift 2
            ;;
        --tag)
            TAG="$2"
            shift 2
            ;;
        --skip-build)
            SKIP_BUILD="true"
            shift
            ;;
        --skip-push)
            SKIP_PUSH="true"
            shift
            ;;
        *)
            echo "Usage: $0 [--registry REGISTRY] [--username USERNAME] [--image-name NAME] [--tag TAG] [--skip-build] [--skip-push]"
            exit 1
            ;;
    esac
done

echo "========================================"
echo "  Build & Push Docker Image to Registry"
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
if [ "$REGISTRY" = "docker.io" ]; then
    if [ -z "$USERNAME" ]; then
        echo "[ERREUR] Username requis pour Docker Hub"
        echo "   Utilisation: $0 --username votre-username"
        exit 1
    fi
    FULL_IMAGE_NAME="${USERNAME}/${IMAGE_NAME}:${TAG}"
elif [ "$REGISTRY" = "ghcr.io" ]; then
    if [ -z "$USERNAME" ]; then
        echo "[ERREUR] Username requis pour GitHub Container Registry"
        echo "   Utilisation: $0 --registry ghcr.io --username votre-username"
        exit 1
    fi
    FULL_IMAGE_NAME="${REGISTRY}/${USERNAME}/${IMAGE_NAME}:${TAG}"
else
    # Registre privé
    if [ -z "$USERNAME" ]; then
        FULL_IMAGE_NAME="${REGISTRY}/${IMAGE_NAME}:${TAG}"
    else
        FULL_IMAGE_NAME="${REGISTRY}/${USERNAME}/${IMAGE_NAME}:${TAG}"
    fi
fi

echo "[*] Configuration:"
echo "   Registry: $REGISTRY"
echo "   Image: $FULL_IMAGE_NAME"
echo ""

# Étape 1: Build de l'image
if [ "$SKIP_BUILD" != "true" ]; then
    echo "[*] Construction de l'image Docker..."
    echo "   Cela peut prendre 10-15 minutes..."
    
    docker build -f Dockerfile.jhipster -t "$FULL_IMAGE_NAME" -t "${IMAGE_NAME}:${TAG}" .
    
    if [ $? -ne 0 ]; then
        echo "[ERREUR] Echec de la construction de l'image"
        exit 1
    fi
    
    echo "[OK] Image construite avec succes: $FULL_IMAGE_NAME"
    echo ""
else
    echo "[*] Construction ignoree (--skip-build)"
    echo ""
fi

# Étape 2: Push vers le registre
if [ "$SKIP_PUSH" != "true" ]; then
    echo "[*] Connexion au registre..."
    
    # Vérifier si l'utilisateur est déjà connecté
    if [ "$REGISTRY" = "docker.io" ]; then
        echo "   Pour Docker Hub, assurez-vous d'etre connecte:"
        echo "   docker login"
    elif [ "$REGISTRY" = "ghcr.io" ]; then
        echo "   Pour GitHub Container Registry, connectez-vous avec:"
        echo "   echo \$GITHUB_TOKEN | docker login ghcr.io -u $USERNAME --password-stdin"
        echo "   (GITHUB_TOKEN doit etre defini dans votre environnement)"
    else
        echo "   Pour un registre prive, connectez-vous avec:"
        echo "   docker login $REGISTRY"
    fi
    
    echo ""
    echo "[*] Envoi de l'image vers le registre..."
    
    docker push "$FULL_IMAGE_NAME"
    
    if [ $? -ne 0 ]; then
        echo "[ERREUR] Echec de l'envoi de l'image"
        echo "   Verifiez que vous etes connecte au registre"
        exit 1
    fi
    
    echo "[OK] Image envoyee avec succes vers $FULL_IMAGE_NAME"
    echo ""
else
    echo "[*] Envoi ignore (--skip-push)"
    echo ""
fi

echo "========================================"
echo "[OK] Operation terminee avec succes!"
echo ""
echo "Image disponible sur: $FULL_IMAGE_NAME"
echo ""
echo "Pour deployer cette image:"
echo "   ./deploy-from-registry.sh --image-name $FULL_IMAGE_NAME"
echo ""
