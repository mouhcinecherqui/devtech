import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AccountService } from 'app/core/auth/account.service';
import { PaiementsService } from 'app/admin/paiements/paiements.service';
import { ClientsService } from 'app/admin/clients/clients.service';
import { DashboardDataService, Ticket, DashboardStats } from './dashboard-data.service';
import { AutoRefreshService } from '../core/services/auto-refresh.service';
import { RefreshButtonComponent } from '../shared/components/refresh-button/refresh-button.component';
import { NotificationBadgeComponent } from '../shared/components/notification-badge/notification-badge.component';
import { MessageNotificationService } from '../core/services/message-notification.service';
import SharedModule from '../shared/shared.module';

// Ticket interface moved to dashboard-data.service.ts

@Component({
  selector: 'jhi-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss'],
  standalone: true,
  imports: [CommonModule, RouterModule, SharedModule, RefreshButtonComponent, NotificationBadgeComponent],
  // changeDetection: ChangeDetectionStrategy.OnPush, // Commenté pour permettre la détection automatique
})
export class AdminDashboardComponent implements OnInit, OnDestroy {
  account: any = null;
  currentTime = new Date();

  // Pour le tableau des tickets
  tickets: Ticket[] = [];
  loading = false;
  error: string | null = null;
  stats: DashboardStats = { tickets: 0, open: 0, closed: 0, urgent: 0 };
  showAllTickets = false;

  // Propriétés pour forcer la réactivité
  statsTickets: number = 0;
  statsOpen: number = 0;
  statsClosed: number = 0;
  statsUrgent: number = 0;

  // Propriétés optimisées pour l'affichage
  displayedTickets: Ticket[] = [];
  readonly MAX_DISPLAYED_TICKETS = 10;

  private readonly accountService = inject(AccountService);
  private readonly http = inject(HttpClient);
  private readonly paiementsService = inject(PaiementsService);
  private readonly clientsService = inject(ClientsService);
  private readonly dashboardDataService = inject(DashboardDataService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly autoRefreshService = inject(AutoRefreshService);
  private readonly messageNotificationService = inject(MessageNotificationService);

  private intervalId?: number;

  ngOnInit(): void {
    // Load account data first (critical for UI)
    this.accountService.identity().subscribe(account => {
      this.account = account;
      this.cdr.detectChanges();
    });

    // Load dashboard data
    this.loadDashboardData();

    // Configurer l'actualisation automatique toutes les 30 secondes
    this.autoRefreshService.setRefreshInterval(30000);
    this.autoRefreshService.refreshTrigger$.subscribe(() => {
      this.loadDashboardData();
    });

    // Démarrer l'écoute des notifications de messages
    this.messageNotificationService.startListening();

    // Mettre à jour l'heure en temps réel avec setTimeout pour éviter les fuites mémoire
    this.updateTime();
  }

  ngOnDestroy(): void {
    if (this.intervalId) {
      clearTimeout(this.intervalId);
    }
    // Arrêter l'écoute des notifications de messages
    this.messageNotificationService.stopListening();
  }

  private updateTime(): void {
    this.currentTime = new Date();
    // Update every 30 seconds instead of every second for better performance
    this.intervalId = window.setTimeout(() => this.updateTime(), 30000);
  }

  trackByTicketId(index: number, ticket: Ticket): number {
    return ticket.id || index;
  }

  getStatusClass(status: string | undefined): string {
    if (!status) return 'status-new';

    switch (status) {
      case 'Nouveau':
        return 'status-new';
      case 'En cours':
        return 'status-open';
      case 'Résolu':
        return 'status-resolved';
      case 'Urgent':
        return 'status-urgent';
      case 'Fermé':
        return 'status-closed';
      default:
        return 'status-new';
    }
  }

  toggleShowAllTickets(): void {
    this.showAllTickets = !this.showAllTickets;
    this.updateDisplayedTickets();
  }

  private updateDisplayedTickets(): void {
    if (this.showAllTickets) {
      this.displayedTickets = [...this.tickets]; // Copie pour éviter les mutations
    } else {
      this.displayedTickets = this.tickets.slice(0, this.MAX_DISPLAYED_TICKETS);
    }
  }

  loadDashboardData(): void {
    this.loading = true;
    console.log('Chargement des données du dashboard...');

    // Chargement optimisé avec timeout pour éviter les blocages
    const timeoutId = setTimeout(() => {
      console.warn('Timeout API, utilisation des données de test');
      this.loadFallbackData();
    }, 3000); // Timeout de 3 secondes

    // Load tickets directly
    this.dashboardDataService.getTickets().subscribe({
      next: tickets => {
        clearTimeout(timeoutId);
        console.log('Tickets chargés:', tickets);
        console.log('Nombre de tickets:', tickets.length);

        // Si l'API retourne une liste vide, utiliser les données de test
        if (!tickets || tickets.length === 0) {
          console.warn('API retourne une liste vide, utilisation des données de test');
          this.loadFallbackData();
        } else {
          this.tickets = tickets;
          this.updateStats();
        }
        this.loading = false;
      },
      error: error => {
        clearTimeout(timeoutId);
        console.warn('Erreur API, utilisation des données de test:', error);
        this.loadFallbackData();
        this.loading = false;
      },
    });
  }

  private loadFallbackData(): void {
    this.tickets = [];
    this.updateStats();
  }

  private updateStats(): void {
    this.stats = this.dashboardDataService.calculateStats(this.tickets);

    // Mettre à jour les propriétés individuelles pour forcer la réactivité
    this.statsTickets = this.stats.tickets;
    this.statsOpen = this.stats.open;
    this.statsClosed = this.stats.closed;
    this.statsUrgent = this.stats.urgent;

    // Mettre à jour les tickets affichés
    this.updateDisplayedTickets();

    console.log('Statistiques mises à jour:', this.stats);

    // Forcer l'actualisation de l'interface
    this.cdr.detectChanges();
  }

  private getMockTickets(): Ticket[] {
    return [
      { id: 1, type: 'Bug', description: 'Problème de connexion', status: 'Nouveau', createdDate: '2025-01-09T10:00:00' },
      { id: 2, type: 'Feature', description: 'Demande de nouvelle fonctionnalité', status: 'En cours', createdDate: '2025-01-09T09:00:00' },
      { id: 3, type: 'Support', description: "Question sur l'utilisation", status: 'Résolu', createdDate: '2025-01-09T08:00:00' },
      { id: 4, type: 'Urgent', description: 'Problème critique', status: 'Urgent', createdDate: '2025-01-09T07:00:00' },
      { id: 5, type: 'Bug', description: "Erreur d'affichage", status: 'Fermé', createdDate: '2025-01-09T06:00:00' },
    ];
  }

  loadDashboardDataProgressive(): void {
    this.loading = true;

    // Use requestIdleCallback for better performance
    if ('requestIdleCallback' in window) {
      requestIdleCallback(() => {
        this.loadTicketsData();
      });
    } else {
      // Fallback for browsers without requestIdleCallback
      setTimeout(() => {
        this.loadTicketsData();
      }, 0);
    }
  }

  private loadTicketsData(): void {
    // Load tickets first (most important data)
    this.dashboardDataService.getTickets().subscribe({
      next: tickets => {
        this.tickets = tickets;
        this.stats = this.dashboardDataService.calculateStats(tickets);
        this.loading = false;

        // Load paiements in background (non-critical) with delay
        setTimeout(() => {
          this.dashboardDataService.getPaiements().subscribe({
            next: () => {
              // Paiements loaded silently in background
            },
            error: () => {
              // Silent error for non-critical data
            },
          });
        }, 500);
      },
      error: () => {
        this.error = 'Erreur lors du chargement des tickets';
        this.loading = false;
      },
    });
  }

  // loadClients(): void {
  //   this.clientsService.getAll().subscribe(clients => {
  //     this.clientsCount = clients.length;
  //     const today = new Date().toISOString().slice(0, 10);
  //     this.clientsToday = clients.filter(c => c.createdDate && c.createdDate.startsWith(today)).length;
  //   });
  // }

  // loadPaiements method moved to DashboardDataService for optimization
}
