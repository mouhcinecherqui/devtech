import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, shareReplay, tap, forkJoin, map, timeout } from 'rxjs';

export interface Ticket {
  id?: number;
  type: string;
  description: string;
  backofficeUrl?: string;
  backofficeLogin?: string;
  backofficePassword?: string;
  hostingUrl?: string;
  createdDate?: string;
  status?: string;
  imageUrl?: string;
  messages?: string[];
  messageStrings?: string[];
}

export interface DashboardStats {
  tickets: number;
  open: number;
  closed: number;
  urgent: number;
}

@Injectable({ providedIn: 'root' })
export class DashboardDataService {
  private readonly http = inject(HttpClient);

  // Cache for tickets data
  private ticketsCache$ = new BehaviorSubject<Ticket[]>([]);
  private lastTicketsFetch = 0;
  private readonly CACHE_DURATION = 30000; // 30 seconds

  getTickets(): Observable<Ticket[]> {
    const now = Date.now();

    // Return cache if still valid and has data
    if (this.lastTicketsFetch > 0 && now - this.lastTicketsFetch < this.CACHE_DURATION && this.ticketsCache$.value.length > 0) {
      console.log('Utilisation du cache pour les tickets');
      return this.ticketsCache$.asObservable();
    }

    // Fetch from server and cache
    return this.http.get<Ticket[]>('/api/tickets').pipe(
      tap(tickets => {
        console.log("Tickets reçus de l'API:", tickets);
        this.ticketsCache$.next(tickets);
        this.lastTicketsFetch = now;
      }),
      shareReplay(1),
      // Timeout pour éviter les blocages
      timeout(5000),
    );
  }

  getPaiements(): Observable<any> {
    return this.http.get('/api/paiements').pipe(shareReplay(1));
  }

  calculateStats(tickets: Ticket[]): DashboardStats {
    return {
      tickets: tickets.length,
      open: tickets.filter(t => t.status === 'Nouveau' || t.status === 'En cours').length,
      closed: tickets.filter(t => t.status === 'Résolu' || t.status === 'Fermé').length,
      urgent: tickets.filter(t => t.status === 'Urgent').length,
    };
  }

  // Load all dashboard data in parallel
  loadDashboardData(): Observable<{ tickets: Ticket[]; paiements: any; stats: DashboardStats }> {
    return forkJoin({
      tickets: this.getTickets(),
      paiements: this.getPaiements(),
    }).pipe(
      map(data => {
        // Calculate stats from tickets
        const stats = this.calculateStats(data.tickets);
        return {
          tickets: data.tickets,
          paiements: data.paiements,
          stats: stats,
        };
      }),
    );
  }
}
