import { MissingTranslationHandler, MissingTranslationHandlerParams, TranslateLoader } from '@ngx-translate/core';
import { HttpClient } from '@angular/common/http';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';

export const translationNotFoundMessage = 'translation-not-found';

export class MissingTranslationHandlerImpl implements MissingTranslationHandler {
  handle(params: MissingTranslationHandlerParams): string {
    const { key } = params;
    return `${translationNotFoundMessage}[${key}]`;
  }
}

export function translatePartialLoader(http: HttpClient): TranslateLoader {
  // Charge le fichier de traduction fusionné depuis i18n/ (généré par webpack MergeJsonWebpackPlugin)
  // En production, les fichiers sont servis depuis /i18n/ (configuré dans StaticResourcesWebConfiguration)
  // Les fichiers sont fusionnés par webpack dans i18n/{lang}.json
  return new TranslateHttpLoader(http, 'i18n/', '.json');
}

export function missingTranslationHandler(): MissingTranslationHandler {
  return new MissingTranslationHandlerImpl();
}

// Langues supportées incluant l'arabe
export const SUPPORTED_LANGUAGES = [
  { code: 'fr', name: 'Français', flag: '🇫🇷' },
  { code: 'en', name: 'English', flag: '🇺🇸' },
  { code: 'es', name: 'Español', flag: '🇪🇸' },
  { code: 'ar', name: 'العربية', flag: '🇸🇦' },
];

export const DEFAULT_LANGUAGE = 'fr';
