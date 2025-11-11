import { Component, OnInit, OnDestroy, Renderer2, RendererFactory2, inject, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterOutlet } from '@angular/router';
import { LangChangeEvent, TranslateService } from '@ngx-translate/core';
import { Subject, takeUntil } from 'rxjs';
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
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class MainComponent implements OnInit, OnDestroy {
  private readonly renderer: Renderer2;

  private readonly router = inject(Router);
  private readonly appPageTitleStrategy = inject(AppPageTitleStrategy);
  readonly accountService = inject(AccountService);
  private readonly translateService = inject(TranslateService);
  private readonly rootRenderer = inject(RendererFactory2);
  private readonly destroy$ = new Subject<void>();

  constructor() {
    this.renderer = this.rootRenderer.createRenderer(document.querySelector('html'), null);
  }

  ngOnInit(): void {
    // try to log in automatically, but don't redirect if not authenticated
    this.accountService
      .identity()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next() {
          // User is authenticated, continue normally
        },
        error() {
          // User is not authenticated, but don't redirect - let them access public pages
          console.warn('User not authenticated, allowing access to public pages');
        },
      });

    this.translateService.onLangChange.pipe(takeUntil(this.destroy$)).subscribe((langChangeEvent: LangChangeEvent) => {
      this.appPageTitleStrategy.updateTitle(this.router.routerState.snapshot);
      dayjs.locale(langChangeEvent.lang);
      this.renderer.setAttribute(document.querySelector('html'), 'lang', langChangeEvent.lang);
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
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
