import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { Subject, takeUntil, interval, timeout, catchError, of } from 'rxjs';
import SharedModule from '../shared/shared.module';
import { ClientDashboardService, Activity, Project, Ticket } from './client-dashboard.service';

// ==========================================================================
// Interfaces
// ==========================================================================

interface DashboardStats {
  tickets: number;
  open: number;
  closed: number;
  responseTime: string;
  trends: {
    tickets: number;
    open: number;
    closed: number;
    responseTime: number;
  };
}

// ==========================================================================
// Component
// ==========================================================================

@Component({
  selector: 'jhi-client-dashboard',
  templateUrl: './client-dashboard.component.html',
  styleUrls: ['./client-dashboard.component.scss'],
  standalone: true,
  imports: [CommonModule, SharedModule],
  providers: [ClientDashboardService],
})
export class ClientDashboardComponent implements OnInit, OnDestroy {
  // ==========================================================================
  // Properties
  // ==========================================================================

  // Data
  tickets: Ticket[] = [];
  activities: Activity[] = [];

  // UI State
  loading = false;
  error: string | null = null;
  stats: DashboardStats = {
    tickets: 0,
    open: 0,
    closed: 0,
    responseTime: '24h',
    trends: {
      tickets: 12,
      open: 8,
      closed: 15,
      responseTime: -5,
    },
  };

  // Auto-refresh
  private destroy$ = new Subject<void>();
  private refreshInterval = 30000; // 30 seconds

  // ==========================================================================
  // Constructor
  // ==========================================================================

  constructor(
    private clientDashboardService: ClientDashboardService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  // ==========================================================================
  // Lifecycle Hooks
  // ==========================================================================

  ngOnInit(): void {
    this.initializeDashboard();
    this.startAutoRefresh();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ==========================================================================
  // Initialization
  // ==========================================================================

  private initializeDashboard(): void {
    // Affichage IMMÉDIAT - pas de loading du tout
    console.log('Dashboard client - affichage immédiat');

    // Charger les données par défaut immédiatement
    this.loadFallbackData();
    this.loading = false;

    // Essayer de charger les vraies données en arrière-plan (sans bloquer)
    this.loadRealDataInBackground();
  }

  private loadRealDataInBackground(): void {
    // Charger les vraies données en arrière-plan et remplacer les données de fallback
    this.clientDashboardService.getTickets().subscribe({
      next: tickets => {
        console.log('Tickets réels chargés:', tickets);
        // Toujours remplacer les données, même si le tableau est vide
        this.tickets = tickets || [];
        this.updateStats();
        console.log('Tickets mis à jour:', this.tickets);
        // Forcer la détection des changements
        this.cdr.detectChanges();
      },
      error: error => {
        console.warn('Erreur chargement tickets en arrière-plan:', error);
      },
    });

    this.clientDashboardService.getActivities().subscribe({
      next: activities => {
        console.log('Activités réelles chargées:', activities);
        // Toujours remplacer les données, même si le tableau est vide
        this.activities = activities || [];
        console.log('Activités mises à jour:', this.activities);
        // Forcer la détection des changements
        this.cdr.detectChanges();
      },
      error: error => {
        console.warn('Erreur chargement activités en arrière-plan:', error);
      },
    });
  }

  private loadFallbackData(): void {
    console.log('Chargement des données de fallback');
    // Données de fallback - tableau vide pour les activités (sera rempli par les vraies données)
    this.tickets = [];
    this.activities = []; // Pas de données de fallback pour les activités - on affichera "Aucune activité" si vide
    this.updateStats();
    console.log('Données de fallback chargées');
  }

  private startAutoRefresh(): void {
    interval(this.refreshInterval)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.initializeDashboard();
      });
  }

  // ==========================================================================
  // Data Fetching
  // ==========================================================================

  // ==========================================================================
  // Stats Management
  // ==========================================================================

  private updateStats(): void {
    console.log('Mise à jour des statistiques avec tickets:', this.tickets);

    this.stats.tickets = this.tickets.length;
    this.stats.open = this.tickets.filter(t => t.status === 'Nouveau' || t.status === 'En cours' || t.status === 'Urgent').length;
    this.stats.closed = this.tickets.filter(t => t.status === 'Résolu' || t.status === 'Fermé').length;

    // Mettre à jour les tendances (valeurs par défaut pour l'instant)
    this.stats.trends = {
      tickets: 12,
      open: 8,
      closed: 15,
      responseTime: -5,
    };

    console.log('Statistiques mises à jour:', this.stats);

    // Forcer la détection des changements
    this.cdr.detectChanges();
  }

  // ==========================================================================
  // Actions
  // ==========================================================================

  logout(): void {
    // Supprimer les données de session
    localStorage.removeItem('jhi-authenticationtoken');
    sessionStorage.clear();

    // Rediriger vers la page de login
    this.router.navigate(['/login']);
  }

  // user menu removed; handled in navbar

  createNewTicket(): void {
    this.router.navigate(['/client-tickets/new']);
  }

  contactSupport(): void {
    this.router.navigate(['/support']);
  }

  viewDocumentation(): void {
    this.router.navigate(['/client-documentation']);
  }

  viewHistory(): void {
    this.router.navigate(['/history']);
  }

  viewAllTickets(): void {
    this.router.navigate(['/client-tickets']);
  }

  viewTicket(ticketId: number): void {
    this.router.navigate(['/client-tickets', ticketId]);
  }

  // ==========================================================================
  // Fallback Data
  // ==========================================================================

  private loadMockTickets(): void {
    this.tickets = [];
    this.updateStats();
  }

  // ==========================================================================
  // Utility Methods
  // ==========================================================================

  getTimeAgo(timestamp: string): string {
    const now = new Date();
    const date = new Date(timestamp);
    const diffInHours = Math.floor((now.getTime() - date.getTime()) / (1000 * 60 * 60));

    if (diffInHours < 1) return 'Il y a quelques minutes';
    if (diffInHours < 24) return `Il y a ${diffInHours}h`;
    if (diffInHours < 48) return 'Il y a 1j';
    return `Il y a ${Math.floor(diffInHours / 24)}j`;
  }

  getPriorityColor(priority: string): string {
    switch (priority) {
      case 'urgent':
        return '#dc3545';
      case 'normal':
        return '#007bff';
      case 'low':
        return '#6c757d';
      default:
        return '#6c757d';
    }
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'Urgent':
      case 'Nouveau':
        return '#dc3545';
      case 'En cours':
        return '#007bff';
      case 'Résolu':
      case 'Fermé':
        return '#28a745';
      default:
        return '#6c757d';
    }
  }

  getActivityTypeClass(activityType: string): string {
    switch (activityType) {
      case 'SUCCESS':
        return 'success';
      case 'INFO':
        return 'info';
      case 'WARNING':
        return 'warning';
      case 'ERROR':
        return 'error';
      default:
        return 'info';
    }
  }

  // ==========================================================================
  // Error Handling
  // ==========================================================================

  clearError(): void {
    this.error = null;
  }

  retryFetch(): void {
    this.clearError();
    this.initializeDashboard();
  }
}
