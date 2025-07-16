import { Component, inject } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import dayjs from 'dayjs/esm';
import { FaIconLibrary } from '@fortawesome/angular-fontawesome';
import { NgbDatepickerConfig } from '@ng-bootstrap/ng-bootstrap';
import locale from '@angular/common/locales/fr';
// jhipster-needle-angular-add-module-import JHipster will add new module here

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { fontAwesomeIcons } from './config/font-awesome-icons';
import MainComponent from './layouts/main/main.component';
import { Title, Meta } from '@angular/platform-browser';

@Component({
  selector: 'jhi-app',
  template: '<jhi-main />',
  imports: [
    MainComponent,
    // jhipster-needle-angular-add-module JHipster will add new module here
  ],
})
export default class AppComponent {
  private readonly applicationConfigService = inject(ApplicationConfigService);
  private readonly iconLibrary = inject(FaIconLibrary);
  private readonly dpConfig = inject(NgbDatepickerConfig);
  private readonly titleService = inject(Title);
  private readonly metaService = inject(Meta);

  constructor() {
    this.applicationConfigService.setEndpointPrefix(SERVER_API_URL);
    registerLocaleData(locale);
    this.iconLibrary.addIcons(...fontAwesomeIcons);
    this.dpConfig.minDate = { year: dayjs().subtract(100, 'year').year(), month: 1, day: 1 };
    // SEO: titre et meta par défaut
    this.titleService.setTitle('DevTech - Support & Gestion');
    this.metaService.updateTag({
      name: 'description',
      content: 'Plateforme DevTech : gestion des tickets, support client, administration, notifications et plus.',
    });
    this.metaService.updateTag({ name: 'keywords', content: 'support, tickets, admin, gestion, DevTech, client, SaaS' });
  }
}
