import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';
import { DOCUMENT } from '@angular/common';
import { Title, Meta, DomSanitizer, SafeHtml } from '@angular/platform-browser';

import SharedModule from 'app/shared/shared.module';
import { AccountService } from 'app/core/auth/account.service';
import { Account } from 'app/core/auth/account.model';
import { PaiementsService } from 'app/admin/paiements/paiements.service';
import { ClientsService } from 'app/admin/clients/clients.service';
import { ClientReviewsDisplayComponent } from '../client-reviews-display/client-reviews-display.component';
import { OAuth2UserService, OAuth2UserInfo } from '../core/auth/oauth2-user.service';
import { OAuth2Service } from '../core/auth/oauth2.service';

@Component({
  selector: 'jhi-home',
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
  imports: [SharedModule, RouterModule, ClientReviewsDisplayComponent],
})
export default class HomeComponent implements OnInit, OnDestroy {
  account = signal<Account | null>(null);
  oauth2UserInfo = signal<OAuth2UserInfo | null>(null);
  structuredData: SafeHtml | null = null;

  // Compteurs et listes pour dashboard admin
  ticketsCount = 0;
  ticketsToday = 0;
  ticketsPending = 0;
  clientsCount = 0;
  clientsToday = 0;
  paiementsCount = 0;
  paiementsToday = 0;

  recentActivities: { text: string; time: string }[] = [
    { text: 'Ticket #204 fermé par admin', time: 'il y a 1 h' },
    { text: 'Nouveau paiement en attente', time: 'il y a 30 min' },
    { text: 'Nouveau client ajouté : SARL WebExpress', time: 'il y a 3 h' },
  ];

  private readonly destroy$ = new Subject<void>();
  private readonly accountService = inject(AccountService);
  private readonly router = inject(Router);
  private readonly http = inject(HttpClient);
  private readonly paiementsService = inject(PaiementsService);
  private readonly clientsService = inject(ClientsService);
  private readonly oauth2Service: OAuth2Service = inject(OAuth2Service);
  private readonly oauth2UserService = inject(OAuth2UserService);
  private readonly titleService = inject(Title);
  private readonly metaService = inject(Meta);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly document = inject(DOCUMENT);

  ngOnInit(): void {
    this.updateSeoMetadata();
    // Vérifier les paramètres OAuth2
    this.oauth2Service.checkOAuth2Success();
    this.accountService
      .getAuthenticationState()
      .pipe(takeUntil(this.destroy$))
      .subscribe(account => {
        this.account.set(account);
        if (account) {
          // Rediriger automatiquement les clients vers leur dashboard
          const authorities: string[] = (account as any).authorities ?? [];
          if (authorities.includes('ROLE_CLIENT')) {
            this.router.navigate(['/client-dashboard']);
            return;
          }
          this.loadTickets();
          this.loadClients();
          this.loadPaiements();
          this.loadOAuth2UserInfo();
        }
      });
  }

  loadTickets(): void {
    this.http.get<any[]>('/api/tickets').subscribe(tickets => {
      this.ticketsCount = tickets.length;
      const today = new Date().toISOString().slice(0, 10);
      this.ticketsToday = tickets.filter(t => t.createdDate?.startsWith(today)).length;
      this.ticketsPending = tickets.filter(t => t.status === 'Pending' || t.status === 'En attente').length;
    });
  }

  loadClients(): void {
    this.clientsService.getAll().subscribe(response => {
      const clients = response.body || [];
      this.clientsCount = clients.length;
      const today = new Date().toISOString().slice(0, 10);
      this.clientsToday = clients.filter(c => c.createdDate?.startsWith(today)).length;
    });
  }

  loadPaiements(): void {
    this.paiementsService.query().subscribe({
      next: (response: any) => {
        const paiements: any[] = response.body ?? [];
        this.paiementsCount = paiements.length;
        const today = new Date().toISOString().slice(0, 10);
        this.paiementsToday = paiements.filter((p: any) => p.date?.startsWith(today)).length;
      },
      error(error: any) {
        console.error('Erreur lors du chargement des paiements:', error);
      },
    });
  }

  loadOAuth2UserInfo(): void {
    this.oauth2UserService.getOAuth2UserInfo().subscribe({
      next: userInfo => {
        this.oauth2UserInfo.set(userInfo);
        if (userInfo.success && userInfo.oauth2) {
          console.warn('Utilisateur OAuth2 connecté:', userInfo);
          this.navigateAfterOAuth2(userInfo);
        }
      },
      error(error) {
        console.warn('Utilisateur non connecté via OAuth2 ou erreur:', error);
      },
    });
  }

  login(): void {
    this.router.navigate(['/login']);
  }

  // Après OAuth2, si l'utilisateur est connecté en tant que client, rediriger vers son dashboard
  private navigateAfterOAuth2(userInfo: OAuth2UserInfo | null): void {
    if (userInfo && userInfo.success && userInfo.oauth2) {
      this.router.navigate(['/client-dashboard']);
    }
  }

  register(): void {
    this.router.navigate(['/register']);
  }

  onWhyChooseCardMouseEnter(event: MouseEvent): void {
    const card = event.currentTarget as HTMLElement;
    const bounds = card.getBoundingClientRect();
    const x = event.clientX - bounds.left;
    const y = event.clientY - bounds.top;
    // Set transform-origin as a percentage
    card.style.setProperty('--hover-x', `${(x / bounds.width) * 100}%`);
    card.style.setProperty('--hover-y', `${(y / bounds.height) * 100}%`);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private updateSeoMetadata(): void {
    const baseUrl = this.getBaseUrl();
    const pageUrl = `${baseUrl}/home`;
    const pageTitle = 'DevTechly | Agence digitale pour applications web et e-commerce';
    const description =
      "DevTechly accompagne les entreprises avec des solutions web modernes, e-commerce et CMS sur mesure, de l'idée à la mise en production.";
    const keywords = 'devtechly, agence web, développement web, e-commerce, maroc, applications sur mesure';

    this.titleService.setTitle(pageTitle);
    this.metaService.updateTag({ name: 'description', content: description });
    this.metaService.updateTag({ name: 'keywords', content: keywords });
    this.metaService.updateTag({ property: 'og:title', content: pageTitle });
    this.metaService.updateTag({ property: 'og:description', content: description });
    this.metaService.updateTag({ property: 'og:type', content: 'website' });
    this.metaService.updateTag({ property: 'og:url', content: pageUrl });
    this.metaService.updateTag({ property: 'og:site_name', content: 'DevTechly' });
    this.metaService.updateTag({ name: 'twitter:card', content: 'summary_large_image' });
    this.metaService.updateTag({ name: 'twitter:title', content: pageTitle });
    this.metaService.updateTag({ name: 'twitter:description', content: description });

    this.updateCanonicalLink(pageUrl);
    this.setStructuredData(baseUrl);
  }

  private updateCanonicalLink(url: string): void {
    if (!this.document) {
      return;
    }
    let link: HTMLLinkElement | null = this.document.querySelector("link[rel='canonical']");
    if (!link) {
      link = this.document.createElement('link');
      link.setAttribute('rel', 'canonical');
      this.document.head.appendChild(link);
    }
    link.setAttribute('href', url);
  }

  private setStructuredData(baseUrl: string): void {
    const organization = {
      '@context': 'https://schema.org',
      '@type': 'Organization',
      name: 'DevTechly',
      url: `${baseUrl}/home`,
      logo: `${baseUrl}/content/images/dt-logo.png`,
      sameAs: ['https://www.linkedin.com/company/devtechly', 'https://twitter.com/devtechly'],
      contactPoint: [
        {
          '@type': 'ContactPoint',
          telephone: '+212612345678',
          contactType: 'customer service',
          availableLanguage: ['fr', 'en'],
          areaServed: 'Worldwide',
        },
      ],
      makesOffer: [
        {
          '@type': 'Service',
          name: 'Développement web sur mesure',
          serviceType: 'SoftwareDevelopment',
        },
        {
          '@type': 'Service',
          name: 'Solutions e-commerce',
          serviceType: 'ECommerce',
        },
      ],
    };

    const website = {
      '@context': 'https://schema.org',
      '@type': 'WebSite',
      name: 'DevTechly',
      url: `${baseUrl}/home`,
      potentialAction: {
        '@type': 'SearchAction',
        target: `${baseUrl}/search?query={search_term_string}`,
        'query-input': 'required name=search_term_string',
      },
    };

    this.structuredData = this.sanitizer.bypassSecurityTrustHtml(JSON.stringify([organization, website]));
  }

  private getBaseUrl(): string {
    if (typeof window !== 'undefined' && window.location) {
      return window.location.origin;
    }
    return 'https://devtechly.com';
  }
}
