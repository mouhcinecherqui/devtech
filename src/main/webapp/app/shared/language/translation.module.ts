import { NgModule, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MissingTranslationHandler, TranslateLoader, TranslateModule, TranslateService } from '@ngx-translate/core';
import { missingTranslationHandler, translatePartialLoader } from 'app/config/translation.config';
import { StateStorageService } from 'app/core/auth/state-storage.service';

import frCustomDashboard from 'i18n/fr/custom-dashboard.json';
import enCustomDashboard from 'i18n/en/custom-dashboard.json';
import esCustomDashboard from 'i18n/es/custom-dashboard.json';
import arCustomDashboard from 'i18n/ar/custom-dashboard.json';

@NgModule({
  imports: [
    TranslateModule.forRoot({
      loader: {
        provide: TranslateLoader,
        useFactory: translatePartialLoader,
        deps: [HttpClient],
      },
      missingTranslationHandler: {
        provide: MissingTranslationHandler,
        useFactory: missingTranslationHandler,
      },
    }),
  ],
})
export class TranslationModule {
  private readonly translateService = inject(TranslateService);
  private readonly stateStorageService = inject(StateStorageService);

  constructor() {
    this.translateService.setDefaultLang('fr');
    // if user have changed language and navigates away from the application and back to the application then use previously chosen language
    const langKey = this.stateStorageService.getLocale() ?? 'fr';
    this.registerCustomTranslations();
    this.translateService.use(langKey);
  }

  private registerCustomTranslations(): void {
    this.translateService.setTranslation('fr', frCustomDashboard as Record<string, unknown>, true);
    this.translateService.setTranslation('en', enCustomDashboard as Record<string, unknown>, true);
    this.translateService.setTranslation('es', esCustomDashboard as Record<string, unknown>, true);
    this.translateService.setTranslation('ar', arCustomDashboard as Record<string, unknown>, true);
  }
}
