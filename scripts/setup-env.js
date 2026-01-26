#!/usr/bin/env node
/**
 * Crée .env à partir de env.example si .env n'existe pas.
 * Usage: node scripts/setup-env.js
 */

const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const envPath = path.join(root, '.env');
const examplePath = path.join(root, 'env.example');

if (!fs.existsSync(examplePath)) {
  console.error('[setup-env] env.example introuvable.');
  process.exit(1);
}

if (fs.existsSync(envPath)) {
  console.log('[setup-env] .env existe déjà. Aucune action.');
  process.exit(0);
}

fs.copyFileSync(examplePath, envPath);
console.log('[setup-env] .env créé depuis env.example.');
console.log('[setup-env] Modifiez .env avec vos valeurs (DB_*, JWT_SECRET, GOOGLE_*, SPRING_MAIL_*).');
