import { Component, OnInit, Renderer2, RendererFactory2, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterOutlet } from '@angular/router';
import { LangChangeEvent, TranslateService } from '@ngx-translate/core';
import dayjs from 'dayjs/esm';

import { AccountService } from 'app/core/auth/account.service';
import { AppPageTitleStrategy } from 'app/app-page-title-strategy';
import FooterComponent from '../footer/footer.component';
import PageRibbonComponent from '../profiles/page-ribbon.component';
import { SidebarComponent } from './sidebar.component';

@Component({
  selector: 'jhi-main',
  templateUrl: './main.component.html',
  styleUrls: ['./main.component.scss'],
  providers: [AppPageTitleStrategy],
  imports: [CommonModule, RouterOutlet, FooterComponent, PageRibbonComponent, SidebarComponent],
})
export default class MainComponent implements OnInit {
  private readonly renderer: Renderer2;

  private readonly router = inject(Router);
  private readonly appPageTitleStrategy = inject(AppPageTitleStrategy);
  readonly accountService = inject(AccountService);
  private readonly translateService = inject(TranslateService);
  private readonly rootRenderer = inject(RendererFactory2);

  constructor() {
    this.renderer = this.rootRenderer.createRenderer(document.querySelector('html'), null);
  }

  ngOnInit(): void {
    // try to log in automatically
    this.accountService.identity().subscribe();

    this.translateService.onLangChange.subscribe((langChangeEvent: LangChangeEvent) => {
      this.appPageTitleStrategy.updateTitle(this.router.routerState.snapshot);
      dayjs.locale(langChangeEvent.lang);
      this.renderer.setAttribute(document.querySelector('html'), 'lang', langChangeEvent.lang);
    });
  }

  toggleTheme(): void {
    const html = document.querySelector('html');
    if (html?.classList.contains('dark-theme')) {
      html.classList.remove('dark-theme');
      html.classList.add('light-theme');
    } else {
      html?.classList.remove('light-theme');
      html?.classList.add('dark-theme');
    }
  }
}
