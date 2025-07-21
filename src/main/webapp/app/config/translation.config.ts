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
  // Charge le fichier unique all-in-one.json pour chaque langue depuis assets
  return new TranslateHttpLoader(http, 'assets/i18n/', '/all-in-one.json');
}

export function missingTranslationHandler(): MissingTranslationHandler {
  return new MissingTranslationHandlerImpl();
}
