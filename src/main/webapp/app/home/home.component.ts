import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';

import SharedModule from 'app/shared/shared.module';
import { AccountService } from 'app/core/auth/account.service';
import { Account } from 'app/core/auth/account.model';
import { PaiementsService } from 'app/admin/paiements/paiements.service';
import { ClientsService } from 'app/admin/clients/clients.service';
import { MatCardModule } from '@angular/material/card';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'jhi-home',
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
  imports: [SharedModule, RouterModule, MatCardModule, MatGridListModule, MatListModule, MatIconModule, MatBadgeModule, MatButtonModule],
})
export default class HomeComponent implements OnInit, OnDestroy {
  account = signal<Account | null>(null);

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

  ngOnInit(): void {
    this.accountService
      .getAuthenticationState()
      .pipe(takeUntil(this.destroy$))
      .subscribe(account => {
        this.account.set(account);
        if (account) {
          this.loadTickets();
          this.loadClients();
          this.loadPaiements();
        }
      });
  }

  loadTickets(): void {
    this.http.get<any[]>('/api/tickets').subscribe(tickets => {
      this.ticketsCount = tickets.length;
      const today = new Date().toISOString().slice(0, 10);
      this.ticketsToday = tickets.filter(t => t.createdDate && t.createdDate.startsWith(today)).length;
      this.ticketsPending = tickets.filter(t => t.status === 'Pending' || t.status === 'En attente').length;
    });
  }

  loadClients(): void {
    this.clientsService.getAll().subscribe(clients => {
      this.clientsCount = clients.length;
      const today = new Date().toISOString().slice(0, 10);
      this.clientsToday = clients.filter(c => c.createdDate && c.createdDate.startsWith(today)).length;
    });
  }

  loadPaiements(): void {
    this.paiementsService.getPaiements().subscribe(paiements => {
      this.paiementsCount = paiements.length;
      const today = new Date().toISOString().slice(0, 10);
      this.paiementsToday = paiements.filter(p => p.date && p.date.startsWith(today)).length;
    });
  }

  login(): void {
    this.router.navigate(['/login']);
  }

  register(): void {
    this.router.navigate(['/register']);
  }

  onWhyChooseCardMouseEnter(event: MouseEvent) {
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
}
