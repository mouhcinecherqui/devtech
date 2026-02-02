#!/usr/bin/env node
/**
 * Convert dt-logo.png to WebP for better image delivery (Lighthouse optimization).
 * Run: npm install sharp --save-dev && node scripts/optimize-images.js
 * Or convert manually at https://squoosh.app and save as dt-logo.webp
 */
const fs = require('fs');
const path = require('path');

const imagesDir = path.join(__dirname, '../src/main/webapp/content/images');
const pngPath = path.join(imagesDir, 'dt-logo.png');
const webpPath = path.join(imagesDir, 'dt-logo.webp');

if (!fs.existsSync(pngPath)) {
  console.log('dt-logo.png not found.');
  process.exit(0);
}

let sharp;
try {
  sharp = require('sharp');
} catch {
  console.log('Run: npm install sharp --save-dev\nOr convert at https://squoosh.app');
  process.exit(0);
}

// Logo displayed at 40px (navbar) and 120px (loading) - output 150px for retina
const logoSize = 150;

sharp(pngPath)
  .resize(logoSize, logoSize)
  .webp({ quality: 85 })
  .toFile(webpPath)
  .then(() => console.log('Created dt-logo.webp'))
  .catch(err => {
    console.error(err.message);
    process.exit(1);
  });
