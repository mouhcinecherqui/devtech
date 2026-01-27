import { NgModule, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MissingTranslationHandler, TranslateLoader, TranslateModule, TranslateService } from '@ngx-translate/core';
import { missingTranslationHandler, translatePartialLoader } from 'app/config/translation.config';
import { StateStorageService } from 'app/core/auth/state-storage.service';

import frGlobal from 'i18n/fr/global.json';
import enGlobal from 'i18n/en/global.json';
import esGlobal from 'i18n/es/global.json';
import arGlobal from 'i18n/ar/global.json';
import frCustomDashboard from 'i18n/fr/custom-dashboard.json';
import enCustomDashboard from 'i18n/en/custom-dashboard.json';
import esCustomDashboard from 'i18n/es/custom-dashboard.json';
import arCustomDashboard from 'i18n/ar/custom-dashboard.json';
import frClientContact from 'i18n/fr/client-contact.json';
import enClientContact from 'i18n/en/client-contact.json';
import esClientContact from 'i18n/es/client-contact.json';
import arClientContact from 'i18n/ar/client-contact.json';
import frClientPayment from 'i18n/fr/client-payment.json';
import enClientPayment from 'i18n/en/client-payment.json';
import esClientPayment from 'i18n/es/client-payment.json';
import arClientPayment from 'i18n/ar/client-payment.json';
import frClientTickets from 'i18n/fr/client-tickets.json';
import enClientTickets from 'i18n/en/client-tickets.json';
import esClientTickets from 'i18n/es/client-tickets.json';
import arClientTickets from 'i18n/ar/client-tickets.json';
import frClientDocumentation from 'i18n/fr/client-documentation.json';
import enClientDocumentation from 'i18n/en/client-documentation.json';
import esClientDocumentation from 'i18n/es/client-documentation.json';
import arClientDocumentation from 'i18n/ar/client-documentation.json';

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

    // Charger d'abord les traductions via HTTP (fichiers fusionnés contenant toutes les traductions)
    // Le TranslateHttpLoader chargera automatiquement les fichiers fusionnés depuis /i18n/{lang}.json
    this.translateService.use(langKey).subscribe({
      next: () => {
        // Les traductions HTTP sont chargées, maintenant ajouter les traductions personnalisées par-dessus
        this.registerCustomTranslations();
        console.log('Traductions chargées pour la langue:', langKey);
      },
      error: error => {
        console.error('Erreur lors du chargement des traductions HTTP:', error);
        // En cas d'erreur, charger les traductions statiques comme fallback
        this.registerCustomTranslations();
      },
    });
  }

  private registerCustomTranslations(): void {
    // Charger les traductions globales pour toutes les langues
    this.translateService.setTranslation('fr', frGlobal as Record<string, unknown>, true);
    this.translateService.setTranslation('en', enGlobal as Record<string, unknown>, true);
    this.translateService.setTranslation('es', esGlobal as Record<string, unknown>, true);
    this.translateService.setTranslation('ar', arGlobal as Record<string, unknown>, true);

    // Charger les traductions personnalisées
    this.translateService.setTranslation('fr', frCustomDashboard as Record<string, unknown>, true);
    this.translateService.setTranslation('fr', frClientContact as Record<string, unknown>, true);
    this.translateService.setTranslation('fr', frClientPayment as Record<string, unknown>, true);
    this.translateService.setTranslation('fr', frClientTickets as Record<string, unknown>, true);
    this.translateService.setTranslation('fr', frClientDocumentation as Record<string, unknown>, true);
    this.translateService.setTranslation('en', enCustomDashboard as Record<string, unknown>, true);
    this.translateService.setTranslation('en', enClientContact as Record<string, unknown>, true);
    this.translateService.setTranslation('en', enClientPayment as Record<string, unknown>, true);
    this.translateService.setTranslation('en', enClientTickets as Record<string, unknown>, true);
    this.translateService.setTranslation('en', enClientDocumentation as Record<string, unknown>, true);
    this.translateService.setTranslation('es', esCustomDashboard as Record<string, unknown>, true);
    this.translateService.setTranslation('es', esClientContact as Record<string, unknown>, true);
    this.translateService.setTranslation('es', esClientPayment as Record<string, unknown>, true);
    this.translateService.setTranslation('es', esClientTickets as Record<string, unknown>, true);
    this.translateService.setTranslation('es', esClientDocumentation as Record<string, unknown>, true);
    this.translateService.setTranslation('ar', arCustomDashboard as Record<string, unknown>, true);
    this.translateService.setTranslation('ar', arClientContact as Record<string, unknown>, true);
    this.translateService.setTranslation('ar', arClientPayment as Record<string, unknown>, true);
    this.translateService.setTranslation('ar', arClientTickets as Record<string, unknown>, true);
    this.translateService.setTranslation('ar', arClientDocumentation as Record<string, unknown>, true);
  }
}
